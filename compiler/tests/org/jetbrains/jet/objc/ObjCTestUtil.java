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
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.objc.ObjCDescriptorResolver;

import java.io.File;

public class ObjCTestUtil {
    private ObjCTestUtil() {}

    @NotNull
    public static NamespaceDescriptor resolveHeaderToNamespaceDescriptor(@NotNull String header) {
        ObjCDescriptorResolver resolver = new ObjCDescriptorResolver();
        return resolver.resolve(new File(header));
    }

    @NotNull
    public static JetCoreEnvironment createEnvironment(@NotNull Disposable disposable) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK);
        return new JetCoreEnvironment(disposable, configuration);
    }
}
