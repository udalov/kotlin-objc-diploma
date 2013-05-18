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

package org.jetbrains.jet.lang.resolve.objc;

import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForObjC;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum AnalyzerFacadeForObjC implements AnalyzerFacade {

    INSTANCE;

    @NotNull
    @Override
    public AnalyzeExhaust analyzeFiles(
            @NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull List<AnalyzerScriptParameter> scriptParameters,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely
    ) {
        List<ImportPath> imports = new ArrayList<ImportPath>();
        imports.add(new ImportPath("jet.objc.*"));
        imports.addAll(DefaultModuleConfiguration.DEFAULT_JET_IMPORTS);

        ModuleDescriptorImpl owner = new ModuleDescriptorImpl(Name.special("<module>"), imports, PlatformToKotlinClassMap.EMPTY);

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false, scriptParameters
        );

        BindingTrace trace = new BindingTraceContext();

        InjectorForTopDownAnalyzerForObjC injector = new InjectorForTopDownAnalyzerForObjC(
                project, topDownAnalysisParameters,
                new ObservableBindingTrace(trace), owner);
        owner.setModuleConfiguration(injector.getObjCModuleConfiguration());

        // TODO: this is temporary to resolve Objective-C built-ins from Java compiled classes in runtime
        ObjCBuiltIns.initialize(injector.getJavaDescriptorResolver());

        try {
            injector.getTopDownAnalyzer().analyzeFiles(files, scriptParameters);
            return AnalyzeExhaust.success(trace.getBindingContext(), null, owner);
        } finally {
            injector.destroy();
        }
    }

    @NotNull
    @Override
    public AnalyzeExhaust analyzeBodiesInFiles(
            @NotNull Project project,
            @NotNull List<AnalyzerScriptParameter> scriptParameters,
            @NotNull Predicate<PsiFile> filesForBodiesResolve,
            @NotNull BindingTrace traceContext,
            @NotNull BodiesResolveContext bodiesResolveContext,
            @NotNull ModuleDescriptor moduleDescriptor
    ) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ResolveSession getLazyResolveSession(@NotNull Project project, @NotNull Collection<JetFile> files) {
        throw new UnsupportedOperationException();
    }
}
