// Copyright (c) 2019, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.desugar.backports;

import com.android.tools.r8.TestParameters;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SparseArrayBackportTest extends AbstractBackportTest {

  private static final String SPARSE_ARRAY_DESCRIPTOR = "Landroid/util/SparseArray;";

  @Parameters(name = "{0}")
  public static Iterable<?> data() {
    return getTestParameters().withDexRuntimes().withAllApiLevels().build();
  }

  public SparseArrayBackportTest(TestParameters parameters) throws IOException {
    super(
        parameters,
        SparseArrayBackportTest.getSparseArray(),
        ImmutableList.of(
            SparseArrayBackportTest.getTestRunner(), SparseArrayBackportTest.getSparseArray()));

    // The constructor is used by the test and put has been available since API 1 and is the
    // method set is rewritten to.
    ignoreInvokes("<init>");
    ignoreInvokes("put");
  }

  private static byte[] getSparseArray() throws IOException {
    return transformer(SparseArray.class).setClassDescriptor(SPARSE_ARRAY_DESCRIPTOR).transform();
  }

  private static byte[] getTestRunner() throws IOException {
    return transformer(TestRunner.class)
        .replaceClassDescriptorInMethodInstructions(
            descriptor(SparseArray.class), SPARSE_ARRAY_DESCRIPTOR)
        .transform();
  }

  public static class SparseArray {
    public void set(int index, Object value) {
      TestRunner.doFail("set should not be called");
    }

    public void put(int index, Object value) {
      TestRunner.doAssertEquals(42, index);
      TestRunner.doAssertEquals("Forty two", value);
    }
  }

  public static class TestRunner extends MiniAssert {

    public static void main(String[] args) {
      new SparseArray().set(42, "Forty two");
    }

    // Forwards to MiniAssert to avoid having to make it public.
    public static void doAssertEquals(int expected, int actual) {
      MiniAssert.assertEquals(expected, actual);
    }

    public static void doAssertEquals(Object expected, Object actual) {
      MiniAssert.assertEquals(expected, actual);
    }

    public static void doFail(String message) {
      MiniAssert.fail(message);
    }
  }
}
