// Copyright (c) 2019, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.desugar.corelib.corelibjdktests;

import static com.android.tools.r8.ToolHelper.JDK_TESTS_BUILD_DIR;
import static com.android.tools.r8.utils.FileUtils.CLASS_EXTENSION;
import static com.android.tools.r8.utils.FileUtils.JAVA_EXTENSION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;

import com.android.tools.r8.D8TestCompileResult;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.TestRuntime.CfVm;
import com.android.tools.r8.ToolHelper;
import com.android.tools.r8.ToolHelper.DexVm.Version;
import com.android.tools.r8.utils.AndroidApiLevel;
import com.android.tools.r8.utils.BooleanUtils;
import com.android.tools.r8.utils.StringUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class Jdk11ConcurrentMapTests extends Jdk11CoreLibTestBase {

  private static final Path CONCURRENT_TESTS_FOLDER =
      Paths.get(ToolHelper.JDK_11_TESTS_DIR + "java/util/concurrent/ConcurrentMap/");
  private static final Path CONCURRENT_HASH_TESTS_FOLDER =
      Paths.get(ToolHelper.JDK_11_TESTS_DIR + "java/util/concurrent/ConcurrentHashMap/");
  private static final Path CONCURRENT_COMPILED_TESTS_FOLDER =
      Paths.get(ToolHelper.JDK_11_TESTS_CLASSES_DIR + "ConcurrentMap/");
  private static final Path CONCURRENT_HASH_COMPILED_TESTS_FOLDER =
      Paths.get(ToolHelper.JDK_11_TESTS_CLASSES_DIR + "ConcurrentHashMap/");
  private static Path[] CONCURRENT_COMPILED_TESTS_FILES;
  private static Path[] CONCURRENT_HASH_COMPILED_TESTS_FILES;
  private static final Path[] SUPPORT_LIBS =
      new Path[]{
          Paths.get(ToolHelper.JDK_11_TESTS_DIR + "lib/testlibrary/jdk/testlibrary/Utils.java"),
          Paths.get(ToolHelper.JDK_11_TESTS_DIR + "lib/testlibrary/jdk/testlibrary/Asserts.java")
      };

  private final TestParameters parameters;
  private final boolean shrinkDesugaredLibrary;

  @Parameters(name = "{1}, shrinkDesugaredLibrary: {0}")
  public static List<Object[]> data() {
    // TODO(b/134732760): Skip Android 4.4.4 due to missing libjavacrypto.
    return buildParameters(
        BooleanUtils.values(),
        getTestParameters()
            .withDexRuntime(Version.V4_0_4)
            .withDexRuntimesStartingFromIncluding(Version.V5_1_1)
            .withAllApiLevels()
            .build());
  }

  public Jdk11ConcurrentMapTests(boolean shrinkDesugaredLibrary, TestParameters parameters) {
    this.shrinkDesugaredLibrary = shrinkDesugaredLibrary;
    this.parameters = parameters;
  }

  @BeforeClass
  public static void compileAtomicClasses() throws Exception {
    ToolHelper.runJavac(
        CfVm.JDK11,
        Collections.singletonList(Paths.get(JDK_TESTS_BUILD_DIR + "testng-6.10.jar")),
        CONCURRENT_COMPILED_TESTS_FOLDER,
        getAllFilesWithSuffixInDirectory(CONCURRENT_TESTS_FOLDER, JAVA_EXTENSION));
    List<Path> concHashFilesAndDependencies = new ArrayList<>();
    Collections.addAll(
        concHashFilesAndDependencies,
        getAllFilesWithSuffixInDirectory(CONCURRENT_HASH_TESTS_FOLDER, JAVA_EXTENSION));
    Collections.addAll(concHashFilesAndDependencies, SUPPORT_LIBS);
    ToolHelper.runJavac(
        CfVm.JDK11,
        Collections.singletonList(Paths.get(JDK_TESTS_BUILD_DIR + "testng-6.10.jar")),
        CONCURRENT_HASH_COMPILED_TESTS_FOLDER,
        concHashFilesAndDependencies.toArray(new Path[0]));
    CONCURRENT_COMPILED_TESTS_FILES =
        getAllFilesWithSuffixInDirectory(CONCURRENT_COMPILED_TESTS_FOLDER, CLASS_EXTENSION);
    CONCURRENT_HASH_COMPILED_TESTS_FILES =
        getAllFilesWithSuffixInDirectory(CONCURRENT_HASH_COMPILED_TESTS_FOLDER, CLASS_EXTENSION);
    assert CONCURRENT_COMPILED_TESTS_FILES.length > 0;
    assert CONCURRENT_HASH_COMPILED_TESTS_FILES.length > 0;
  }

  @Test
  public void testD8Concurrent() throws Exception {
    // TODO(b/134732760): Support Java 9+ libraries.
    // We skip the ConcurrentRemoveIf test because of the  non desugared class CompletableFuture.
    // TODO(b/142377475).
    Assume.assumeTrue(!shrinkDesugaredLibrary);
    KeepRuleConsumer keepRuleConsumer = createKeepRuleConsumer(parameters);
    String verbosity = "2";
    testForD8()
        .addProgramFiles(CONCURRENT_COMPILED_TESTS_FILES)
        .addProgramFiles(testNGSupportProgramFiles())
        .addProgramFiles(getPathsFiles())
        .setMinApi(parameters.getApiLevel())
        .enableCoreLibraryDesugaring(parameters.getApiLevel(), keepRuleConsumer)
        .addLibraryFiles(ToolHelper.getAndroidJar(AndroidApiLevel.P))
        .compile()
        .withArt6Plus64BitsLib()
        .addDesugaredCoreLibraryRunClassPath(
            this::buildDesugaredLibrary,
            parameters.getApiLevel(),
            keepRuleConsumer.get(),
            shrinkDesugaredLibrary)
        .run(parameters.getRuntime(), "TestNGMainRunner", verbosity, "ConcurrentModification")
        .assertSuccessWithOutputThatMatches(
            endsWith(StringUtils.lines("ConcurrentModification: SUCCESS")));
  }

  private Path[] concurrentHashTestToCompile() {
    // We exclude WhiteBox.class because of Method handles, they are not supported on old devices
    // and the test uses methods not present even on 28.
    List<Path> toCompile = new ArrayList<>();
    Collections.addAll(toCompile, CONCURRENT_HASH_COMPILED_TESTS_FILES);
    toCompile.removeIf(file -> file.getFileName().toString().equals("WhiteBox.class"));
    return toCompile.toArray(new Path[0]);
  }

  private String[] concurrentHashTestNGTestsToRun() {
    List<String> toRun = new ArrayList<>();
    // TODO(b/134732760): Support Java 9+ libraries.
    // We exclude ConcurrentAssociateTest and ConcurrentContainsKeyTest due to non
    // desugared class CompletableFuture.
    // toRun.add("ConcurrentAssociateTest");
    // toRun.add("ConcurrentContainsKeyTest");
    return toRun.toArray(new String[0]);
  }

  private String[] concurrentHashMainTestsToRun() {
    return new String[]{
        "MapCheck",
        // TODO(b/134732760): Support Java 9+ libraries.
        // Following fails due to non desugared class SplittableRandom.
        // "MapLoops",
        // TODO(b/134732760): Support Java 9+ libraries.
        // Following fails due to non desugared class CompletableFuture.
        // "ToArray",
        "DistinctEntrySetElements",
    };
  }

  @Test
  public void testD8ConcurrentHash() throws Exception {
    // TODO(b/134732760): Investigate failure when shrinkDesugaredLibrary is on.
    Assume.assumeFalse(shrinkDesugaredLibrary);
    KeepRuleConsumer keepRuleConsumer = createKeepRuleConsumer(parameters);
    String verbosity = "2";
    D8TestCompileResult d8TestCompileResult =
        testForD8()
            .addProgramFiles(concurrentHashTestToCompile())
            .addProgramFiles(testNGSupportProgramFiles())
            .addProgramFiles(getPathsFiles())
            .setMinApi(parameters.getApiLevel())
            .enableCoreLibraryDesugaring(parameters.getApiLevel(), keepRuleConsumer)
            .compile()
            .withArt6Plus64BitsLib()
            .addDesugaredCoreLibraryRunClassPath(
                this::buildDesugaredLibrary,
                parameters.getApiLevel(),
                keepRuleConsumer.get(),
                shrinkDesugaredLibrary);
    System.out.println(keepRuleConsumer.get());
    for (String className : concurrentHashTestNGTestsToRun()) {
      d8TestCompileResult
          .run(parameters.getRuntime(), "TestNGMainRunner", verbosity, className)
          .assertSuccessWithOutputThatMatches(endsWith(StringUtils.lines(className + ": SUCCESS")));
    }
    for (String className : concurrentHashMainTestsToRun()) {
      // Main jdk tests relies on the main function running without issues.
      // Failure implies a runtime exception.
      // We ensure that everything could be resolved (no missing method or class)
      // with the assertion on stderr.
      d8TestCompileResult
          .run(parameters.getRuntime(), className).assertSuccess()
          .assertStderrMatches(not(containsString("Could not")));
    }
  }
}
