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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;

import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.TranslationUnit;

public class ObjCResolveFacade {
    private Project project;
    private NamespaceDescriptor rootNamespace;
    private NamespaceDescriptor resolvedNamespace;

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    public void setRootNamespace(@NotNull NamespaceDescriptor rootNamespace) {
        this.rootNamespace = rootNamespace;
    }

    static {
        System.loadLibrary("KotlinNativeIndexer");
    }

    private native byte[] buildObjCIndex(@NotNull String args);

    @NotNull
    private TranslationUnit indexObjCHeaders(@NotNull String args) {
        try {
            byte[] bytes = buildObjCIndex(args);
            return TranslationUnit.parseFrom(bytes);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    public NamespaceDescriptor resolve() {
        if (resolvedNamespace != null) {
            return resolvedNamespace;
        }

        assert project != null : "Project should be initialized in " + getClass().getName();
        String args = ObjCInteropParameters.getArgs(project);
        assert args != null : "Header parameter should be saved into " + ObjCInteropParameters.class.getName();
        assert rootNamespace != null : "Root namespace should be set before Obj-C resolve";

        TranslationUnit translationUnit = indexObjCHeaders(args);

        ObjCDescriptorResolver resolver = new ObjCDescriptorResolver(rootNamespace);
        resolvedNamespace = resolver.resolveTranslationUnit(translationUnit);

        return resolvedNamespace;
    }
}
