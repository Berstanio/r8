// Copyright (c) 2022, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.optimize.unusedarguments;

import static com.android.tools.r8.utils.codeinspector.Matchers.isPresent;
import static com.android.tools.r8.utils.codeinspector.Matchers.isPresentAndRenamed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.android.tools.r8.KeepConstantArguments;
import com.android.tools.r8.NeverClassInline;
import com.android.tools.r8.NeverInline;
import com.android.tools.r8.NeverPropagateValue;
import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.TestParametersCollection;
import com.android.tools.r8.utils.codeinspector.ClassSubject;
import com.android.tools.r8.utils.codeinspector.MethodSubject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StaticizeUpdateGenericSignatureTest extends TestBase {

  @Parameter() public TestParameters parameters;

  @Parameters(name = "{0}")
  public static TestParametersCollection data() {
    return getTestParameters().withAllRuntimesAndApiLevels().build();
  }

  @Test
  public void testR8() throws Exception {
    testForR8Compat(parameters.getBackend())
        .addInnerClasses(getClass())
        .setMinApi(parameters.getApiLevel())
        .addKeepAttributeSignature()
        .addKeepMainRule(Main.class)
        .enableInliningAnnotations()
        .enableNeverClassInliningAnnotations()
        .enableMemberValuePropagationAnnotations()
        .enableConstantArgumentAnnotations()
        .run(parameters.getRuntime(), Main.class)
        .assertSuccessWithOutputLines("Hello World")
        .inspect(
            inspector -> {
              ClassSubject classA = inspector.clazz(A.class);
              assertThat(classA, isPresentAndRenamed());
              MethodSubject foo = classA.uniqueMethod();
              assertThat(foo, isPresent());
              assertEquals("(TT;)Ljava/lang/String;", foo.getFinalSignatureAttribute());
              assertEquals(
                  "java.lang.String a(java.lang.Object)", foo.getFinalSignature().toString());
            });
  }

  @NeverClassInline
  public static class A<T> {

    @NeverInline
    @NeverPropagateValue
    @KeepConstantArguments
    public String foo(T t) {
      return "Hello " + t;
    }
  }

  public static class Main {

    public static void main(String[] args) {
      System.out.println(new A<String>().foo("World"));
    }
  }
}
