// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.optimize.library;

import com.android.tools.r8.graph.DexItemFactory;
import com.android.tools.r8.graph.DexMethod;
import com.android.tools.r8.graph.LibraryMethod;
import com.android.tools.r8.ir.code.InvokeMethod;
import com.android.tools.r8.ir.code.Value;
import com.android.tools.r8.utils.BiPredicateUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class LibraryMethodSideEffectModelCollection {

  private final Map<DexMethod, BiPredicate<DexMethod, List<Value>>> finalMethodsWithoutSideEffects;
  private final Set<DexMethod> unconditionalFinalMethodsWithoutSideEffects;

  private final Set<DexMethod> nonFinalMethodsWithoutSideEffects;

  public LibraryMethodSideEffectModelCollection(DexItemFactory dexItemFactory) {
    finalMethodsWithoutSideEffects = buildFinalMethodsWithoutSideEffects(dexItemFactory);
    unconditionalFinalMethodsWithoutSideEffects =
        buildUnconditionalFinalMethodsWithoutSideEffects(dexItemFactory);
    nonFinalMethodsWithoutSideEffects = buildNonFinalMethodsWithoutSideEffects(dexItemFactory);
  }

  private static Map<DexMethod, BiPredicate<DexMethod, List<Value>>>
      buildFinalMethodsWithoutSideEffects(DexItemFactory dexItemFactory) {
    ImmutableMap.Builder<DexMethod, BiPredicate<DexMethod, List<Value>>> builder =
        ImmutableMap.<DexMethod, BiPredicate<DexMethod, List<Value>>>builder()
            .put(
                dexItemFactory.stringMembers.constructor,
                (method, arguments) -> arguments.get(0).isNeverNull());
    putAll(
        builder,
        dexItemFactory.stringBufferMethods.constructorMethods,
        dexItemFactory.stringBufferMethods::constructorInvokeIsSideEffectFree);
    putAll(
        builder,
        dexItemFactory.stringBuilderMethods.constructorMethods,
        dexItemFactory.stringBuilderMethods::constructorInvokeIsSideEffectFree);
    return builder.build();
  }

  private static Set<DexMethod> buildUnconditionalFinalMethodsWithoutSideEffects(
      DexItemFactory dexItemFactory) {
    return ImmutableSet.<DexMethod>builder()
        .add(dexItemFactory.booleanMembers.toString)
        .add(dexItemFactory.byteMembers.toString)
        .add(dexItemFactory.charMembers.toString)
        .add(dexItemFactory.doubleMembers.toString)
        .add(dexItemFactory.enumMembers.constructor)
        .add(dexItemFactory.floatMembers.toString)
        .add(dexItemFactory.integerMembers.toString)
        .add(dexItemFactory.longMembers.toString)
        .add(dexItemFactory.npeMethods.init)
        .add(dexItemFactory.npeMethods.initWithMessage)
        .add(dexItemFactory.objectMembers.constructor)
        .add(dexItemFactory.objectMembers.getClass)
        .add(dexItemFactory.shortMembers.toString)
        .add(dexItemFactory.stringBufferMethods.toString)
        .add(dexItemFactory.stringBuilderMethods.toString)
        .add(dexItemFactory.stringMembers.hashCode)
        .add(dexItemFactory.stringMembers.toString)
        .addAll(dexItemFactory.classMethods.getNames)
        .addAll(dexItemFactory.boxedValueOfMethods())
        .build();
  }

  private static Set<DexMethod> buildNonFinalMethodsWithoutSideEffects(
      DexItemFactory dexItemFactory) {
    return ImmutableSet.of(
        dexItemFactory.objectMembers.equals,
        dexItemFactory.objectMembers.hashCode,
        dexItemFactory.objectMembers.toString);
  }

  private static <K, V> void putAll(ImmutableMap.Builder<K, V> builder, Iterable<K> keys, V value) {
    for (K key : keys) {
      builder.put(key, value);
    }
  }

  public void forEachSideEffectFreeFinalMethod(Consumer<DexMethod> consumer) {
    unconditionalFinalMethodsWithoutSideEffects.forEach(consumer);
  }

  public boolean isCallToSideEffectFreeFinalMethod(InvokeMethod invoke) {
    return isSideEffectFreeFinalMethod(invoke.getInvokedMethod(), invoke.arguments());
  }

  public boolean isSideEffectFreeFinalMethod(DexMethod method, List<Value> arguments) {
    return unconditionalFinalMethodsWithoutSideEffects.contains(method)
        || finalMethodsWithoutSideEffects
            .getOrDefault(method, BiPredicateUtils.alwaysFalse())
            .test(method, arguments);
  }

  // This intentionally takes the invoke instruction since the determination of whether a library
  // method has side effects may depend on the arguments.
  public boolean isSideEffectFree(InvokeMethod invoke, LibraryMethod singleTarget) {
    return isSideEffectFreeFinalMethod(singleTarget.getReference(), invoke.arguments())
        || nonFinalMethodsWithoutSideEffects.contains(singleTarget.getReference());
  }
}
