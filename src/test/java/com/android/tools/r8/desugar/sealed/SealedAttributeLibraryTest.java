// Copyright (c) 2022, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.desugar.sealed;

import static org.junit.Assume.assumeTrue;

import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestCompilerBuilder;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.examples.jdk17.Sealed;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SealedAttributeLibraryTest extends TestBase {

  @Parameter(0)
  public TestParameters parameters;

  @Parameters(name = "{0}")
  public static List<Object[]> data() {
    return buildParameters(
        getTestParameters().withAllRuntimesAndApiLevels().build());
  }

  private void runTest(TestCompilerBuilder<?, ?, ?, ?, ?> builder) throws Exception {
    builder
        .addDefaultRuntimeLibrary(parameters)
        .addLibraryFiles(Sealed.jar())
        .addInnerClasses(getClass())
        .setMinApi(parameters.getApiLevel())
        .run(parameters.getRuntime(), TestRunner.class)
        .assertSuccessWithOutputLines("Hello, world!");
  }

  @Test
  public void testD8() throws Exception {
    assumeTrue(parameters.isDexRuntime());
    runTest(testForD8(parameters.getBackend()));
  }

  @Test
  public void testR8() throws Exception {
    runTest(testForR8(parameters.getBackend()).addKeepMainRule(TestRunner.class));
  }

  public static class TestRunner {

    public static void main(String[] args) {
      System.out.println("Hello, world!");
    }
  }
}
