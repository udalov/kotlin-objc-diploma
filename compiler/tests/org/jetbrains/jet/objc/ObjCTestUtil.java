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

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.objc.ObjCModuleConfiguration;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ObjCTestUtil {
    private ObjCTestUtil() {}

    @NotNull
    public static JetCoreEnvironment createEnvironment(@NotNull Disposable disposable) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK);
        return new JetCoreEnvironment(disposable, configuration);
    }

    @NotNull
    public static NamespaceDescriptor extractObjCNamespaceFromAnalyzeExhaust(@NotNull AnalyzeExhaust analyzeExhaust) {
        ModuleConfiguration moduleConfiguration = analyzeExhaust.getModuleConfiguration();
        assert moduleConfiguration instanceof ObjCModuleConfiguration
                : "Not an Obj-C module configuration: " + moduleConfiguration;
        return ((ObjCModuleConfiguration) moduleConfiguration).getResolver().resolve();
    }

    public static void compileObjectiveC(@NotNull String filename, @NotNull File out) {
        String command = String.format("clang -ObjC -dynamiclib -framework Foundation %s -o %s", filename, out);
        runProcess(command);
    }

    @NotNull
    public static List<String> runProcess(@NotNull String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            List<String> result = readLines(process.getInputStream());

            for (String line : readLines(process.getErrorStream())) {
                System.err.println(line);
            }
            System.err.flush();

            int exitCode = process.exitValue();
            assert exitCode == 0 : "Process exited with code " + exitCode;

            return result;
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    private static List<String> readLines(@NotNull InputStream in) throws Exception {
        List<String> result = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }
}
