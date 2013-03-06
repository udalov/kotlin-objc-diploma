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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.objc.descriptors.ObjCModuleDescriptor;
import org.jetbrains.jet.lang.resolve.objc.descriptors.ObjCNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.*;

public class ObjCDescriptorResolver {
    public static final String PROTOCOL_NAME_SUFFIX = "Protocol";

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
    public NamespaceDescriptor resolve(@NotNull /* TODO: List<File> */ File header) {
        TranslationUnit translationUnit = indexObjCHeaders(header);

        ObjCModuleDescriptor module = new ObjCModuleDescriptor();
        ObjCNamespaceDescriptor namespace = new ObjCNamespaceDescriptor(module);
        ObjCDescriptorMapper mapper = new ObjCDescriptorMapper(namespace);

        WritableScope scope = namespace.getMemberScope().changeLockLevel(WritableScope.LockLevel.BOTH);

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


        scope.changeLockLevel(WritableScope.LockLevel.READING);

        return namespace;
    }
}
