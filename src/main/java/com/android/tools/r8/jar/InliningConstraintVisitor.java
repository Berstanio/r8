// Copyright (c) 2018, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.jar;

import static org.objectweb.asm.Opcodes.ASM6;

import com.android.tools.r8.dex.Constants;
import com.android.tools.r8.errors.Unreachable;
import com.android.tools.r8.graph.DexClass;
import com.android.tools.r8.graph.DexEncodedMethod;
import com.android.tools.r8.graph.DexField;
import com.android.tools.r8.graph.DexMethod;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.graph.GraphLense;
import com.android.tools.r8.graph.JarApplicationReader;
import com.android.tools.r8.ir.conversion.JarSourceCode;
import com.android.tools.r8.ir.optimize.Inliner.Constraint;
import com.android.tools.r8.ir.optimize.InliningConstraints;
import com.android.tools.r8.shaking.Enqueuer.AppInfoWithLiveness;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.TryCatchBlockNode;

// This visitor can be used to determine if a piece of jar code has any instructions that the
// inliner would not be willing to inline. This can be used to determine if a method can be force
// inlined although its IR is still not available.
//
// Note that this class has only been implemented for the hooks in InliningConstraints that may
// return a non-ALWAYS inlining constraint (e.g., InliningConstraints.forReturn is not called).
public class InliningConstraintVisitor extends MethodVisitor {

  private final JarApplicationReader application;
  private final AppInfoWithLiveness appInfo;
  private final InliningConstraints inliningConstraints;
  private final DexEncodedMethod method;
  private final DexType invocationContext;

  private Constraint constraint;

  public InliningConstraintVisitor(
      JarApplicationReader application,
      AppInfoWithLiveness appInfo,
      GraphLense graphLense,
      DexEncodedMethod method,
      DexType invocationContext) {
    super(ASM6);
    this.application = application;
    this.appInfo = appInfo;
    this.inliningConstraints = new InliningConstraints(appInfo, graphLense);
    this.method = method;
    this.invocationContext = invocationContext;

    // Model a synchronized method as having a monitor instruction.
    this.constraint =
        method.accessFlags.isSynchronized() ? inliningConstraints.forMonitor() : Constraint.ALWAYS;
  }

  public Constraint getConstraint() {
    return constraint;
  }

  private void updateConstraint(Constraint other) {
    constraint = Constraint.min(constraint, other);
  }

  // Used to signal that the result is ready, such that we do not need to visit all instructions of
  // the method, if we can see early on that it cannot be inlined anyway.
  public boolean isFinished() {
    return constraint == Constraint.NEVER;
  }

  public void accept(TryCatchBlockNode tryCatchBlock) {
    // Model a try-catch as a move-exception instruction.
    updateConstraint(inliningConstraints.forMoveException());
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    DexField field = application.getField(owner, name, desc);
    switch (opcode) {
      case Opcodes.GETFIELD:
        updateConstraint(inliningConstraints.forInstanceGet(field, invocationContext));
        break;

      case Opcodes.PUTFIELD:
        updateConstraint(inliningConstraints.forInstancePut(field, invocationContext));
        break;

      case Opcodes.GETSTATIC:
        updateConstraint(inliningConstraints.forStaticGet(field, invocationContext));
        break;

      case Opcodes.PUTSTATIC:
        updateConstraint(inliningConstraints.forStaticPut(field, invocationContext));
        break;

      default:
        throw new Unreachable("Unexpected opcode " + opcode);
    }
  }

  @Override
  public void visitLdcInsn(Object cst) {
    if (cst instanceof Type && ((Type) cst).getSort() != Type.METHOD) {
      DexType type = application.getType((Type) cst);
      updateConstraint(inliningConstraints.forConstClass(type, invocationContext));
    } else {
      updateConstraint(inliningConstraints.forConstInstruction());
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    DexType ownerType = application.getTypeFromName(owner);
    DexMethod target = application.getMethod(ownerType, name, desc);
    switch (opcode) {
      case Opcodes.INVOKEDYNAMIC:
        if (JarSourceCode.isCallToPolymorphicSignatureMethod(owner, name)) {
          updateConstraint(inliningConstraints.forInvokePolymorphic(target, invocationContext));
        } else {
          updateConstraint(inliningConstraints.forInvokeCustom());
        }
        break;

      case Opcodes.INVOKEINTERFACE:
        updateConstraint(inliningConstraints.forInvokeInterface(target, invocationContext));
        break;

      case Opcodes.INVOKESPECIAL:
        if (name.equals(Constants.INSTANCE_INITIALIZER_NAME) || ownerType == invocationContext) {
          updateConstraint(inliningConstraints.forInvokeDirect(target, invocationContext));
        } else {
          updateConstraint(inliningConstraints.forInvokeSuper(target, invocationContext));
        }
        break;

      case Opcodes.INVOKESTATIC:
        updateConstraint(inliningConstraints.forInvokeStatic(target, invocationContext));
        break;

      case Opcodes.INVOKEVIRTUAL:
        // Instructions that target a private method in the same class are translated to
        // invoke-direct.
        if (target.holder == method.method.holder) {
          DexClass clazz = appInfo.definitionFor(target.holder);
          if (clazz != null && clazz.lookupDirectMethod(target) != null) {
            updateConstraint(inliningConstraints.forInvokeDirect(target, invocationContext));
            break;
          }
        }

        updateConstraint(inliningConstraints.forInvokeVirtual(target, invocationContext));
        break;

      default:
        throw new Unreachable("Unexpected opcode " + opcode);
    }
  }

  @Override
  public void visitInsn(int opcode) {
    switch (opcode) {
      case Opcodes.MONITORENTER:
      case Opcodes.MONITOREXIT:
        updateConstraint(inliningConstraints.forMonitor());
        break;

      default:
        // All instructions here lead to the inlining constraint ALWAYS.
    }
  }

  @Override
  public void visitMultiANewArrayInsn(String desc, int dims) {
    DexType type = application.getTypeFromDescriptor(desc);
    updateConstraint(inliningConstraints.forInvokeMultiNewArray(type, invocationContext));
  }

  @Override
  public void visitTypeInsn(int opcode, String typeName) {
    DexType type = application.getTypeFromName(typeName);
    switch (opcode) {
      case Opcodes.ANEWARRAY:
        updateConstraint(inliningConstraints.forNewArrayEmpty(type, invocationContext));
        break;

      case Opcodes.CHECKCAST:
        updateConstraint(inliningConstraints.forCheckCast(type, invocationContext));
        break;

      case Opcodes.INSTANCEOF:
        updateConstraint(inliningConstraints.forInstanceOf(type, invocationContext));
        break;

      case Opcodes.NEW:
        updateConstraint(inliningConstraints.forNewInstance(type, invocationContext));
        break;

      default:
        throw new Unreachable("Unexpected opcode " + opcode);
    }
  }
}
