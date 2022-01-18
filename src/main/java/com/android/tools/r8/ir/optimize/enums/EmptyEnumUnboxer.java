// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.optimize.enums;

import com.android.tools.r8.graph.AppView;
import com.android.tools.r8.graph.DexProgramClass;
import com.android.tools.r8.graph.GraphLens;
import com.android.tools.r8.graph.ProgramMethod;
import com.android.tools.r8.ir.analysis.fieldvalueanalysis.StaticFieldValues;
import com.android.tools.r8.ir.code.IRCode;
import com.android.tools.r8.ir.code.InstructionListIterator;
import com.android.tools.r8.ir.code.InvokeMethod;
import com.android.tools.r8.ir.code.Phi;
import com.android.tools.r8.ir.conversion.IRConverter;
import com.android.tools.r8.ir.conversion.MethodConversionOptions.MutableMethodConversionOptions;
import com.android.tools.r8.ir.conversion.MethodProcessor;
import com.android.tools.r8.ir.conversion.PostMethodProcessor.Builder;
import com.android.tools.r8.ir.optimize.info.OptimizationFeedbackDelayed;
import com.android.tools.r8.shaking.AppInfoWithLiveness;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class EmptyEnumUnboxer extends EnumUnboxer {

  private static final EmptyEnumUnboxer INSTANCE = new EmptyEnumUnboxer();

  private EmptyEnumUnboxer() {}

  static EmptyEnumUnboxer get() {
    return INSTANCE;
  }

  @Override
  public void prepareForPrimaryOptimizationPass(GraphLens graphLensForPrimaryOptimizationPass) {
    // Intentionally empty.
  }

  @Override
  public void analyzeEnums(IRCode code, MutableMethodConversionOptions conversionOptions) {
    // Intentionally empty.
  }

  @Override
  public void onMethodPruned(ProgramMethod method) {
    // Intentionally empty.
  }

  @Override
  public void onMethodCodePruned(ProgramMethod method) {
    // Intentionally empty.
  }

  @Override
  public void recordEnumState(DexProgramClass clazz, StaticFieldValues staticFieldValues) {
    // Intentionally empty.
  }

  @Override
  public Set<Phi> rewriteCode(IRCode code, MethodProcessor methodProcessor) {
    return Sets.newIdentityHashSet();
  }

  @Override
  public void rewriteNullCheck(InstructionListIterator iterator, InvokeMethod invoke) {
    // Intentionally empty.
  }

  @Override
  public void unboxEnums(
      AppView<AppInfoWithLiveness> appView,
      IRConverter converter,
      Builder postMethodProcessorBuilder,
      ExecutorService executorService,
      OptimizationFeedbackDelayed feedback) {
    appView.setUnboxedEnums(EnumDataMap.empty());
  }

  @Override
  public void unsetRewriter() {
    // Intentionally empty.
  }

  @Override
  public void updateEnumUnboxingCandidatesInfo() {
    // Intentionally empty.
  }
}
