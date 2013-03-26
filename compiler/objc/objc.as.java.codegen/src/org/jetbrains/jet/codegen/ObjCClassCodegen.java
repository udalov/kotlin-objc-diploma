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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.ClassWriter;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeProjection;

import java.io.File;
import java.util.Collections;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.genInitSingletonField;

public class ObjCClassCodegen {
    public static final String JET_RUNTIME_OBJC = "jet/runtime/objc/ObjC";

    public static final String JL_OBJECT = "Ljava/lang/Object;";
    public static final String JL_STRING = "Ljava/lang/String;";
    public static final Type JL_OBJECT_TYPE = Type.getType(JL_OBJECT);

    private final JetTypeMapper typeMapper;
    private final ClassDescriptor descriptor;
    private final File dylib;

    private final ClassWriter cw;
    private final Type asmType;

    public ObjCClassCodegen(@NotNull JetTypeMapper typeMapper, @NotNull ClassDescriptor descriptor, @NotNull File dylib) {
        this.typeMapper = typeMapper;
        this.descriptor = descriptor;
        this.dylib = dylib;

        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        asmType = typeMapper.mapType(descriptor.getDefaultType(), JetTypeMapperMode.IMPL);
    }

    private interface MethodCodegen {
        void generate(@NotNull InstructionAdapter v);
    }

    private void newMethod(int flags, @NotNull String name, @NotNull String descriptor, @NotNull MethodCodegen codegen) {
        MethodVisitor mv = cw.visitMethod(flags, name, descriptor, null, null);
        mv.visitCode();
        codegen.generate(new InstructionAdapter(mv));
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    @NotNull
    public byte[] generateClass() {
        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, asmType.getInternalName(), null, JL_OBJECT_TYPE.getInternalName(), new String[0]);

        cw.visitSource(null, null);

        if (descriptor.getKind() == ClassKind.CLASS_OBJECT) {
            cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, JvmAbi.INSTANCE_FIELD, asmType.getDescriptor(), null, null);
        }

        generateStaticInitializer();

        generateConstructor();

        JetScope scope = descriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        for (DeclarationDescriptor member : scope.getAllDescriptors()) {
            if (member instanceof FunctionDescriptor) {
                // TODO: other kinds
                if (descriptor.getKind() == ClassKind.CLASS_OBJECT) {
                    generateClassObjectMethod((FunctionDescriptor) member);
                }
            }
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private void generateStaticInitializer() {
        newMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", new MethodCodegen() {
            @Override
            public void generate(@NotNull InstructionAdapter v) {
                if (descriptor.getKind() == ClassKind.CLASS_OBJECT) {
                    genInitSingletonField(asmType, v);
                }
                v.visitLdcInsn(dylib + "");
                v.invokestatic(JET_RUNTIME_OBJC, "loadLibrary", "(" + JL_STRING + ")V");
                v.areturn(Type.VOID_TYPE);
            }
        });
    }

    private void generateConstructor() {
        newMethod(ACC_PUBLIC, "<init>", "()V", new MethodCodegen() {
            @Override
            public void generate(@NotNull InstructionAdapter v) {
                v.load(0, JL_OBJECT_TYPE);
                v.invokespecial(JL_OBJECT_TYPE.getInternalName(), "<init>", "()V");
                v.areturn(Type.VOID_TYPE);
            }
        });
    }

    private void generateClassObjectMethod(@NotNull FunctionDescriptor method) {
        final JvmMethodSignature signature = typeMapper.mapSignature(method.getName(), method);

        newMethod(ACC_PUBLIC, signature.getName(), signature.getAsmMethod().getDescriptor(), new MethodCodegen() {
            @Override
            public void generate(@NotNull InstructionAdapter v) {
                v.visitLdcInsn(descriptor.getContainingDeclaration().getName().getName());
                v.visitLdcInsn(signature.getName());

                // TODO: arguments
                v.iconst(0);
                v.newarray(JL_OBJECT_TYPE);

                // TODO: not only void methods
                v.invokestatic(
                        JET_RUNTIME_OBJC,
                        "sendMessageToClassObjectVoid",
                        "(" + JL_STRING + JL_STRING + "[" + JL_OBJECT
                        + ")V"
                );

                v.areturn(Type.VOID_TYPE);
            }
        });
    }
}
