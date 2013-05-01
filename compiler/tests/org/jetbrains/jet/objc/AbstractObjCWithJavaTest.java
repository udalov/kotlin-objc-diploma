/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
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

package org.jetbrains.jet.objc;

import com.google.common.base.Predicates;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.KotlinCodegenFacade;
import org.jetbrains.jet.codegen.ObjCDescriptorCodegen;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.objc.AnalyzerFacadeForObjC;
import org.jetbrains.jet.lang.resolve.objc.ObjCInteropParameters;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.objc.ObjCTestUtil.*;

public abstract class AbstractObjCWithJavaTest extends UsefulTestCase {
    private File tmpDir;
    private JetCoreEnvironment environment;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        tmpDir = JetTestUtils.tmpDirForTest(this);
        environment = createEnvironment(getTestRootDisposable());
    }

    @Override
    protected void tearDown() throws Exception {
        tmpDir = null;
        environment = null;

        super.tearDown();
    }

    protected void doTest(@NotNull String kotlinSource) {
        assert kotlinSource.endsWith(".kt") : kotlinSource;
        String fileNameCommon = kotlinSource.substring(0, kotlinSource.length() - ".kt".length());
        String header = fileNameCommon + ".h";
        String implementation = fileNameCommon + ".m";

        ObjCInteropParameters.saveHeaders(environment.getProject(), new File(header));

        List<JetFile> files = Collections.singletonList(createJetFile(kotlinSource));
        AnalyzeExhaust analyzeExhaust = analyze(files);

        File dylib = new File(tmpDir, "libKotlinObjCTest.dylib");
        compileObjectiveC(implementation, dylib);

        NamespaceDescriptor descriptor = extractObjCNamespaceFromAnalyzeExhaust(analyzeExhaust);

        ObjCDescriptorCodegen codegen = new ObjCDescriptorCodegen();
        codegen.generate(descriptor, tmpDir, dylib);

        generate(files, analyzeExhaust, codegen.getBindingContext());

        String actual = runCompiledKotlinClass();
        assertEquals("OK", actual);
    }

    @NotNull
    private String runCompiledKotlinClass() {
        String classpath = ".:" + tmpDir + ":" + getKotlinRuntimeJarFile();
        String libraryPath = ".:" + tmpDir + ":" + getKotlinNativeDylibFile().getParent();

        String command = "java"
                + " -cp " + classpath
                + " -Djava.library.path=" + libraryPath
                + " " + PackageClassUtils.getPackageClassFqName(new FqName("test"));
        return runProcess(command);
    }

    private static void compileObjectiveC(@NotNull String filename, @NotNull File out) {
        String command = String.format("clang -ObjC -dynamiclib -framework Foundation %s -o %s", filename, out);
        runProcess(command);
    }

    @NotNull
    private static File getKotlinRuntimeJarFile() {
        File kotlinRuntime = new File("dist/kotlinc/lib/kotlin-runtime.jar");
        assert kotlinRuntime.exists() : "kotlin-runtime.jar should exist before this test, run dist";
        return kotlinRuntime;
    }

    @NotNull
    private static File getKotlinNativeDylibFile() {
        File kotlinNative = new File("runtime/native/libKotlinNative.dylib");
        assert kotlinNative.exists() : "libKotlinNative.dylib should exist before this test";
        return kotlinNative;
    }

    @NotNull
    private AnalyzeExhaust analyze(@NotNull List<JetFile> files) {
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForObjC.INSTANCE.analyzeFiles(
                environment.getProject(),
                files,
                Collections.<AnalyzerScriptParameter>emptyList(),
                Predicates.<PsiFile>alwaysTrue());
        analyzeExhaust.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    private void generate(@NotNull List<JetFile> files, @NotNull AnalyzeExhaust analyzeExhaust, @NotNull BindingContext objcBinding) {
        BindingContext context = new ChainedBindingContext(analyzeExhaust.getBindingContext(), objcBinding);

        GenerationState state = new GenerationState(environment.getProject(), ClassBuilderFactories.TEST, context, files);
        KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);

        CompileEnvironmentUtil.writeToOutputDirectory(state.getFactory(), tmpDir);
    }

    @NotNull
    private JetFile createJetFile(@NotNull String fileName) {
        try {
            String content = FileUtil.loadFile(new File(fileName), true);
            return JetTestUtils.createFile(fileName, content, environment.getProject());
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }
}
