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
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.objc.descriptors.ObjCModuleDescriptor;
import org.jetbrains.jet.lang.resolve.objc.descriptors.ObjCNamespaceDescriptor;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;

public class ObjCDescriptorResolver {
    static {
        System.loadLibrary("ObjCDescriptorResolver");
    }

    private native void buildObjCIndex(@NotNull String header, @NotNull String outputFileName);

    @NotNull
    public NamespaceDescriptor resolve(@NotNull /* TODO: List<File> */ File header) {
        File tmpFile;
        try {
            tmpFile = File.createTempFile(System.currentTimeMillis() + "", "kotlin-objc");
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }

        buildObjCIndex(header.getAbsolutePath(), tmpFile.getAbsolutePath());

        ObjCModuleDescriptor module = new ObjCModuleDescriptor();
        ObjCNamespaceDescriptor namespace = new ObjCNamespaceDescriptor(module);
        return namespace;
    }
}
