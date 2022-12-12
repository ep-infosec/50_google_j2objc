/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc;

import com.google.devtools.j2objc.util.FileUtil;
import com.google.devtools.j2objc.util.HeaderMap;
import com.google.devtools.j2objc.util.SourceVersion;
import com.google.devtools.j2objc.util.Version;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Tests for {@link com.google.devtools.j2objc.J2ObjC}.
 */
public class J2ObjCTest extends GenerationTest {
  String jarPath;
  String exampleJavaPath;
  String packageInfoPath;

  private static final String EXAMPLE_JAVA_SOURCE =
      "package com.google.devtools.j2objc.annotations;\n\n"
      + "import com.google.j2objc.annotations.ObjectiveCName;\n\n"
      + "@ObjectiveCName(\"foo\")\n"
      + "public class Example {\n"
      + "}\n";

  @Override
  public void setUp() throws IOException {
    super.setUp();

    List<String> classpathEntries = options.fileUtil().getClassPathEntries();
    for (String entry : getComGoogleDevtoolsJ2objcPath()) {
      classpathEntries.add(entry);
    }

    jarPath = getResourceAsFile("util/example.jar");
    exampleJavaPath = "com/google/devtools/j2objc/util/Example.java";
    packageInfoPath = "com/google/devtools/j2objc/util/package-info.java";
    addSourceFile(
        "package com.google.devtools.j2objc.util; public class Example {}", exampleJavaPath);
    addSourceFile(
        "@ObjectiveCName(\"CBT\")\n"
        + "package com.google.devtools.j2objc.util;\n"
        + "import com.google.j2objc.annotations.ObjectiveCName;", packageInfoPath);
  }

  // No assertions for packageInfoH--nothing interesting in it that other tests don't assert.
  private void makeAssertions(String exampleH, String exampleM, String packageInfoM)
      throws Exception {
    // These assertions should hold for any generated files, including (in the future)
    // combined jar generation.
    assertTranslation(exampleH, "Generated by the J2ObjC translator.");
    assertTranslation(exampleH, "interface CBTExample : NSObject");
    assertTranslation(exampleH, "- (instancetype)init;");
    assertTranslation(exampleH, "J2OBJC_EMPTY_STATIC_INIT(CBTExample)");
    assertTranslation(exampleH, "J2OBJC_TYPE_LITERAL_HEADER(CBTExample)");
    assertTranslation(exampleM, "@implementation CBTExample");
    assertTranslation(exampleM, "- (instancetype)init {");
    assertTranslation(exampleM, "+ (const J2ObjcClassInfo *)__metadata {");
    assertTranslation(exampleM, "J2OBJC_CLASS_TYPE_LITERAL_SOURCE(CBTExample)");
    assertTranslatedLines(packageInfoM,
        "+ (NSString *)__prefix {",
        "  return @\"CBT\";",
        "}");

    assertNoErrors();
  }

  private void makeAssertionsForJar() throws Exception {
    String exampleH = getTranslatedFile("com/google/test/Example.h");
    String exampleM = getTranslatedFile("com/google/test/Example.m");
    String packageInfoH = getTranslatedFile("com/google/test/package-info.h");
    String packageInfoM = getTranslatedFile("com/google/test/package-info.m");
    // Test the file header comments.
    assertTranslation(exampleH, "source: jar:file:");
    assertTranslation(exampleM, "source: jar:file:");
    assertTranslation(packageInfoH, "source: jar:file:");
    assertTranslation(packageInfoM, "source: jar:file:");
    assertTranslation(exampleH, jarPath);
    assertTranslation(exampleM, jarPath);
    assertTranslation(packageInfoH, jarPath);
    assertTranslation(packageInfoM, jarPath);
    assertTranslation(exampleH, "com/google/test/Example.java");
    assertTranslation(exampleM, "com/google/test/Example.java");
    assertTranslation(packageInfoH, "com/google/test/package-info.java");
    assertTranslation(packageInfoM, "com/google/test/package-info.java");
    assertNotInTranslation(packageInfoH, "J2ObjCTempDir");
    assertNotInTranslation(packageInfoM, "J2ObjCTempDir");
    // Test the includes
    assertTranslation(exampleH, "#pragma push_macro(\"INCLUDE_ALL_ComGoogleTestExample\")");
    assertTranslation(exampleM, "#include \"com/google/test/Example.h\"");
    assertTranslation(
        packageInfoH, "#pragma push_macro(\"INCLUDE_ALL_ComGoogleTestPackage_info\")");
    assertTranslation(packageInfoM, "@interface ComGoogleTestpackage_info : NSObject");
    assertTranslation(packageInfoM, "@implementation ComGoogleTestpackage_info");
    // All other assertions
    makeAssertions(exampleH, exampleM, packageInfoM);
  }

  public void testCompilingFromJar() throws Exception {
    J2ObjC.run(Collections.singletonList(jarPath), options);
    makeAssertionsForJar();
  }

  // Make assertions for java files with default output locations.
  private void makeAssertionsForJavaFiles() throws Exception {
    String exampleH = getTranslatedFile("com/google/devtools/j2objc/util/Example.h");
    String exampleM = getTranslatedFile("com/google/devtools/j2objc/util/Example.m");
    String packageInfoH = getTranslatedFile("com/google/devtools/j2objc/util/package-info.h");
    String packageInfoM = getTranslatedFile("com/google/devtools/j2objc/util/package-info.m");
    assertTranslation(exampleM, "#include \"com/google/devtools/j2objc/util/Example.h\"");
    makeAssertionsForJavaFiles(exampleH, exampleM, packageInfoH, packageInfoM);
  }

  private void makeAssertionsForJavaFiles(
      String exampleH, String exampleM, String packageInfoH, String packageInfoM)
      throws Exception {
    // Test the file header comments.
    assertTranslation(exampleH, exampleJavaPath);
    assertTranslation(exampleM, exampleJavaPath);
    assertTranslation(packageInfoH, packageInfoPath);
    assertTranslation(packageInfoM, packageInfoPath);
    // Test the includes
    assertTranslation(
        exampleH, "#pragma push_macro(\"INCLUDE_ALL_ComGoogleDevtoolsJ2objcUtilExample\")");
    assertTranslation(
        packageInfoM, "@interface ComGoogleDevtoolsJ2objcUtilpackage_info : NSObject");
    assertTranslation(packageInfoH,
        "#pragma push_macro(\"INCLUDE_ALL_ComGoogleDevtoolsJ2objcUtilPackage_info\")");
    assertTranslation(packageInfoM, "@implementation ComGoogleDevtoolsJ2objcUtilpackage_info");
    // All other assertions
    makeAssertions(exampleH, exampleM, packageInfoM);
  }

  private void makeAssertionsForGlobalCombinedOutputJavaFiles() throws Exception {
    String exampleH = getTranslatedFile("testGlobalCombinedOutputFile.h");
    String exampleM = getTranslatedFile("testGlobalCombinedOutputFile.m");
    assertTranslation(exampleM, "#include \"testGlobalCombinedOutputFile.h\"");
    makeAssertionsForGlobalCombinedOutputJavaFiles(exampleH, exampleM);
  }

  private void makeAssertionsForGlobalCombinedOutputJavaFiles(String exampleH, String exampleM)
      throws Exception {
    // Test the includes
    assertTranslation(
        exampleH, "#pragma push_macro(\"INCLUDE_ALL_TestGlobalCombinedOutputFile\")");
    assertTranslation(
        exampleM, "@interface ComGoogleDevtoolsJ2objcUtilpackage_info : NSObject");
    assertTranslation(exampleM, "@implementation ComGoogleDevtoolsJ2objcUtilpackage_info");
    // All other assertions
    makeAssertions(exampleH, exampleM, exampleM);
  }

  public void testCompilingFromFiles() throws Exception {
    J2ObjC.run(Arrays.asList(exampleJavaPath, packageInfoPath), options);
    makeAssertionsForJavaFiles();
  }

  private void makeAssertionsForCombinedJar() throws Exception {
    String combinedH = getTranslatedFile(jarPath.replace(".jar", ".h"));
    String combinedM = getTranslatedFile(jarPath.replace(".jar", ".m"));
    makeAssertions(combinedH, combinedM, combinedM);
  }

  public void testCombinedJar() throws Exception {
    options.getHeaderMap().setCombineJars();
    J2ObjC.run(Collections.singletonList(jarPath), options);
    makeAssertionsForCombinedJar();
  }

  public void testGlobalCombinedOutput() throws Exception {
    options.setGlobalCombinedOutput("testGlobalCombinedOutputFile");
    J2ObjC.run(Arrays.asList(exampleJavaPath, packageInfoPath), options);
    makeAssertionsForGlobalCombinedOutputJavaFiles();
  }

  public void testSourceDirsOption() throws Exception {
    options.getHeaderMap().setOutputStyle(HeaderMap.OutputStyleOption.SOURCE);
    J2ObjC.run(Arrays.asList(exampleJavaPath, packageInfoPath), options);
    String exampleH = getTranslatedFile(exampleJavaPath.replace(".java", ".h"));
    String exampleM = getTranslatedFile(exampleJavaPath.replace(".java", ".m"));
    String packageInfoH = getTranslatedFile(packageInfoPath.replace(".java", ".h"));
    String packageInfoM = getTranslatedFile(packageInfoPath.replace(".java", ".m"));
    makeAssertionsForJavaFiles(exampleH, exampleM, packageInfoH, packageInfoM);
  }

  private void assertServiceAnnotationProcessorOutput() throws IOException {
    String translatedAnnotationHeader = getTranslatedFile("ProcessingResult.h");
    String translatedAnnotationImpl = getTranslatedFile("ProcessingResult.m");

    // Our dummy annotation processor is very simple--it always creates a class with no package,
    // ProcessingResult, with a minimal implementation.
    assertTranslation(translatedAnnotationHeader, "@interface ProcessingResult : NSObject");
    assertTranslation(translatedAnnotationHeader, "- (NSString *)getResult;");
    assertTranslation(translatedAnnotationImpl, "@implementation ProcessingResult");
    assertTranslation(translatedAnnotationImpl, "return @\"ObjectiveCName\"");
  }

  private void assertSpecifiedAnnotationProcessorOutput() throws IOException {
    String translatedAnnotationHeader = getTranslatedFile("ExplicitProcessingResult.h");
    String translatedAnnotationImpl = getTranslatedFile("ExplicitProcessingResult.m");

    // Our dummy annotation processor is very simple--it always creates a class with no package,
    // ProcessingResult, with a minimal implementation.
    assertTranslation(translatedAnnotationHeader, "@interface ExplicitProcessingResult : NSObject");
    assertTranslation(translatedAnnotationHeader, "- (NSString *)getResultForExplicitProcessor;");
    assertTranslation(translatedAnnotationImpl, "@implementation ExplicitProcessingResult");
    assertTranslation(translatedAnnotationImpl, "return @\"ObjectiveCName\"");
  }

  // Test a simple annotation processor on the classpath.
  public void testAnnotationProcessing() throws Exception {
    String processorPath = getResourceAsFile("annotations/Processor.jar");
    options.fileUtil().getClassPathEntries().add(processorPath);

    String examplePath = addSourceFile(EXAMPLE_JAVA_SOURCE, "annotations/Example.java");
    J2ObjC.run(Collections.singletonList(examplePath), options);
    assertNoErrors();

    assertServiceAnnotationProcessorOutput();
  }

  // Test a simple annotation processor on the processor path.
  public void testAnnotationProcessingWithProcessorPath() throws Exception {
    String processorPath = getResourceAsFile("annotations/Processor.jar");
    options.getProcessorPathEntries().add(processorPath);

    String examplePath = addSourceFile(EXAMPLE_JAVA_SOURCE, "annotations/Example.java");
    J2ObjC.run(Collections.singletonList(examplePath), options);
    assertNoErrors();

    assertServiceAnnotationProcessorOutput();
  }

  // Test a specified annotation processor.
  public void testSpecifiedAnnotationProcessing() throws Exception {
    String processorPath = getResourceAsFile("annotations/ExplicitProcessor.jar");
    options.fileUtil().getClassPathEntries().add(processorPath);
    options.setProcessors("com.google.devtools.j2objc.annotations.J2ObjCTestExplicitProcessor");

    String examplePath = addSourceFile(EXAMPLE_JAVA_SOURCE, "annotations/Example.java");
    J2ObjC.run(Collections.singletonList(examplePath), options);
    assertNoErrors();

    assertSpecifiedAnnotationProcessorOutput();
  }

  // Test an explicitly invoked annotation processor on the processor path.
  public void testSpecifiedAnnotationProcessingWithProcessorPath() throws Exception {
    String processorPath = getResourceAsFile("annotations/ExplicitProcessor.jar");
    options.getProcessorPathEntries().add(processorPath);
    options.setProcessors("com.google.devtools.j2objc.annotations.J2ObjCTestExplicitProcessor");

    String examplePath = addSourceFile(EXAMPLE_JAVA_SOURCE, "annotations/Example.java");
    J2ObjC.run(Collections.singletonList(examplePath), options);
    assertNoErrors();

    assertSpecifiedAnnotationProcessorOutput();
  }

  // Test both types of processor detection at the same time, see that -processor indeed bypasses
  // default discovery
  public void testSpecifiedAnnotationProcessingBypass() throws Exception {
    String serviceProcessorPath = getResourceAsFile("annotations/Processor.jar");
    String explicitProcessorPath = getResourceAsFile("annotations/ExplicitProcessor.jar");
    options.fileUtil().getClassPathEntries().add(serviceProcessorPath);
    options.fileUtil().getClassPathEntries().add(explicitProcessorPath);
    options.setProcessors("com.google.devtools.j2objc.annotations.J2ObjCTestExplicitProcessor");

    String examplePath = addSourceFile(EXAMPLE_JAVA_SOURCE, "annotations/Example.java");
    J2ObjC.run(Collections.singletonList(examplePath), options);
    assertNoErrors();

    assertSpecifiedAnnotationProcessorOutput();
    assertFalse("Overridden processor generated output",
        getTempFile("ProcessingResult.h").exists());
  }

  // Test for warning if compiling jar with -g.
  public void testJarSourceDebug() throws Exception {
    options.setEmitLineDirectives(true);
    options.setLogLevel(Level.FINEST);
    J2ObjC.run(Collections.singletonList(jarPath), options);
    assertWarningRegex("source debugging of jar files is not supported: .*");
  }

  // Test for error if jar doesn't contain a Java source file.
  public void testJarNoJava() throws Exception {
    String processorJarPath = getResourceAsFile("annotations/Processor.jar");
    J2ObjC.run(Collections.singletonList(processorJarPath), options);
    assertWarningRegex(".* does not contain any Java source files.");
  }

  public void testSourcePathTypesIncludedInGlobalCombinedOutput() throws Exception {
    options.setGlobalCombinedOutput("combined_file");
    jarPath = getResourceAsFile("util/example.jar");
    options.fileUtil().insertSourcePath(0, jarPath);
    String srcPath =
        addSourceFile(
            String.join(
                "\n",
                "package foo.bar;",
                "class Test {",
                "  Class<?> getExampleClass() {",
                "     return com.google.test.Example.class;",
                "  }",
                "}"),
            "foo/bar/Test.java");
    J2ObjC.run(Collections.singletonList(srcPath), options);
    String translation = getTranslatedFile("combined_file.m");

    // Verify both implementations are in the combined file ...
    assertTranslation(translation, "@implementation FooBarTest");
    assertTranslation(translation, "@implementation CBTExample");

    // ... and that there isn't a "type not found" import.
    assertNotInTranslation(translation, "Example.h");
  }

  // Verify module-info.java files generate no-code output files with "-source 1.8" flag.
  public void testSource8EmptyModuleInfo() throws IOException {
    options.setSourceVersion(SourceVersion.JAVA_8);
    String srcPath = addSourceFile(
        "module foo.bar {"
        + "  exports foo.bar;"
        + "}",
        "foo.bar/module-info.java");
    J2ObjC.run(Collections.singletonList(srcPath), options);
    String translation = getTranslatedFile("foo/bar/module-info.h");
    assertTranslation(translation, "INCLUDE_ALL_FooBarModule_info");
    translation = getTranslatedFile("foo/bar/module-info.m");
    assertTranslation(translation, "foo/bar/module-info.h");
    assertNotInTranslation(translation, "@implementation");
  }

  public void testHeaderOutputDirectory() throws IOException {
    File headerOutputDir = FileUtil.createTempDir("testout-hdrs");
    options.fileUtil().setHeaderOutputDirectory(headerOutputDir);
    String srcPath = addSourceFile("package foo.bar; class Test {}", "foo/bar/Test.java");
    J2ObjC.run(Collections.singletonList(srcPath), options);

    // Just the generated header should be in the header output directory.
    assertTrue(new File(headerOutputDir, "foo/bar/Test.h").exists());
    assertFalse(new File(headerOutputDir, "foo/bar/Test.m").exists());

    // Everything else is still in the regular output directory.
    assertFalse(new File(tempDir, "foo/bar/Test.h").exists());
    assertTrue(new File(tempDir, "foo/bar/Test.m").exists());
    assertTrue(new File(tempDir, "foo/bar/Test.java").exists());
  }

  public void testJavacVersionString() {
    assertTrue(Version.jarVersion(Options.class).contains("(javac "));
  }

  public void testClasspathWildcard() throws Exception {
    // The package-info.class in the jar specifies to keep reflection for the package.
    // This should take precedence over the strip-reflection option.
    String jarFilePath = getResourceAsFile("util/packageInfoLookupTest.jar");
    String wildcard = new File(jarFilePath).getParent() + "/*";
    options.fileUtil().getClassPathEntries().clear();
    options.load(new String[] {"--strip-reflection", "-classpath", wildcard});

    String srcPath = addSourceFile(
        "package com.google.test.packageInfoLookupTest; public class A {} ",
        "com/google/test/packageInfoLookupTest/A.java");
    J2ObjC.run(Collections.singletonList(srcPath), options);
    String translation = getTranslatedFile("com/google/test/packageInfoLookupTest/A.m");
    assertTranslation(translation, "__metadata");
  }
}