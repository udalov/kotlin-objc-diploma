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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.*;

public class ObjCDescriptorResolver {
    public static final String PROTOCOL_NAME_SUFFIX = "Protocol";

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

    private native void buildObjCIndex(@NotNull String header, @NotNull String outputFileName);

    @NotNull
    private TranslationUnit indexObjCHeaders(@NotNull File header) {
        try {
            File tmpFile = File.createTempFile(System.currentTimeMillis() + "", "kotlin-objc");
            buildObjCIndex(header.getAbsolutePath(), tmpFile.getAbsolutePath());
            return TranslationUnit.parseFrom(new FileInputStream(tmpFile));
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
        File header = ObjCInteropParameters.getHeaders(project);
        assert header != null : "Header parameter should be saved into " + ObjCInteropParameters.class.getName();
        assert rootNamespace != null : "Root namespace should be set before Obj-C resolve";

        TranslationUnit translationUnit = indexObjCHeaders(header);

        NamespaceDescriptorImpl namespace = new NamespaceDescriptorImpl(rootNamespace,
                Collections.<AnnotationDescriptor>emptyList(), Name.identifier("objc"));
        rootNamespace.addNamespace(namespace);

        ObjCDescriptorMapper mapper = new ObjCDescriptorMapper(namespace);

        WritableScope scope = new WritableScopeImpl(JetScope.EMPTY, namespace, RedeclarationHandler.THROW_EXCEPTION, "objc scope");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        namespace.initialize(scope);

        for (ObjCClass clazz : translationUnit.getClassList()) {
            ClassDescriptor classDescriptor = mapper.mapClass(clazz);
            scope.addClassifierAlias(classDescriptor.getName(), classDescriptor);
        }

        for (ObjCProtocol protocol : translationUnit.getProtocolList()) {
            String protocolName = protocol.getName();
            Name name = Name.identifier(protocolName);
            if (scope.getClassifier(name) != null) {
                // Since Objective-C classes and protocols exist in different namespaces and Kotlin classes and traits don't,
                // we invent a new name here for the trait when a class with the same name exists already
                name = Name.identifier(protocolName + PROTOCOL_NAME_SUFFIX);
            }
            ClassDescriptor classDescriptor = mapper.mapProtocol(protocol, name);
            scope.addClassifierAlias(classDescriptor.getName(), classDescriptor);
        }

        resolvedNamespace = namespace;
        return resolvedNamespace;
    }
}
