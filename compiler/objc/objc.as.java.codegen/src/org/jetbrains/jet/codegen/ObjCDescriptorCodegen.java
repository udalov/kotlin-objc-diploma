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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;

public class ObjCDescriptorCodegen {
    private final JetTypeMapper typeMapper;

    public ObjCDescriptorCodegen() {
        this.typeMapper = new JetTypeMapper(new BindingTraceContext(), true, ClassBuilderMode.FULL);
    }

    @NotNull
    public Type asmType(@NotNull ClassDescriptor descriptor) {
        return typeMapper.mapType(descriptor.getDefaultType(), JetTypeMapperMode.IMPL);
    }

    // This is needed to make JetTypeMapper correctly map class objects
    private void recordFQNForClassObject(@NotNull ClassDescriptor classDescriptor, @NotNull ClassDescriptor classObject) {
        String internalName = asmType(classDescriptor).getInternalName();
        JvmClassName classObjectName = JvmClassName.byInternalName(internalName + JvmAbi.CLASS_OBJECT_SUFFIX);
        typeMapper.getBindingTrace().record(CodegenBinding.FQN, classObject, classObjectName);
    }

    private void writeClassFile(@NotNull ClassDescriptor descriptor, @NotNull File outputDir, @NotNull byte[] bytes) {
        String internalName = asmType(descriptor).getInternalName();
        File file = new File(outputDir, internalName + ".class");

        File outerDir = file.getParentFile();
        if (outerDir != null) {
            outerDir.mkdirs();
        }

        try {
            FileUtil.writeToFile(file, bytes);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    private void generateAndWriteClass(@NotNull File dylib, @NotNull File outputDir, @NotNull ClassDescriptor classDescriptor) {
        ObjCClassCodegen codegen = new ObjCClassCodegen(typeMapper, classDescriptor, dylib);
        byte[] bytes = codegen.generateClass();
        writeClassFile(classDescriptor, outputDir, bytes);
    }

    public void generate(@NotNull NamespaceDescriptor namespace, @NotNull File outputDir, @NotNull File dylib) {
        for (DeclarationDescriptor descriptor : namespace.getMemberScope().getAllDescriptors()) {
            if (!(descriptor instanceof ClassDescriptor)) continue;

            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            generateAndWriteClass(dylib, outputDir, classDescriptor);

            ClassDescriptor classObject = classDescriptor.getClassObjectDescriptor();
            if (classObject != null) {
                recordFQNForClassObject(classDescriptor, classObject);
                generateAndWriteClass(dylib, outputDir, classObject);
            }
        }
    }
}
