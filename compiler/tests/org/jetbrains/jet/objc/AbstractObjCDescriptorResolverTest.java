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
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.objc.AnalyzerFacadeForObjC;
import org.jetbrains.jet.lang.resolve.objc.ObjCInteropParameters;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

import java.io.File;
import java.util.Collections;

import static org.jetbrains.jet.objc.ObjCTestUtil.createEnvironment;
import static org.jetbrains.jet.objc.ObjCTestUtil.extractObjCNamespaceFromAnalyzeExhaust;
import static org.jetbrains.jet.test.util.NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaceWithFile;

public abstract class AbstractObjCDescriptorResolverTest extends TestCaseWithTmpdir {
    public void doTest(@NotNull String header) {
        assert header.endsWith(".h") : header;
        File expected = new File(header.substring(0, header.length() - ".h".length()) + ".txt");

        JetCoreEnvironment environment = createEnvironment(getTestRootDisposable(), ConfigurationKind.ALL);
        ObjCInteropParameters.setArgs(environment.getProject(), header);

        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForObjC.INSTANCE.analyzeFiles(
                environment.getProject(),
                Collections.<JetFile>emptyList(),
                Collections.<AnalyzerScriptParameter>emptyList(),
                Predicates.<PsiFile>alwaysFalse()
        );
        analyzeExhaust.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        NamespaceDescriptor descriptor = extractObjCNamespaceFromAnalyzeExhaust(analyzeExhaust);

        compareNamespaceWithFile(descriptor, DONT_INCLUDE_METHODS_OF_OBJECT, expected);
    }
}
