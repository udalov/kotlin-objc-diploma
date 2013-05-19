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

import jet.objc.*;
import jet.runtime.objc.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.ClassWriter;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.objc.ObjCBuiltIns;
import org.jetbrains.jet.lang.resolve.objc.ObjCMethodDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.asm4.Type.*;
import static org.jetbrains.jet.codegen.AsmUtil.genInitSingletonField;

public class ObjCClassCodegen {
    public static final String JET_RUNTIME_OBJC = Type.getType(NativeHelpers.class).getInternalName();

    public static final Type JL_OBJECT_TYPE = Type.getType(Object.class);
    public static final Type JL_STRING_TYPE = Type.getType(String.class);

    public static final Type ID_TYPE = Type.getType(ID.class);
    public static final Type OBJC_OBJECT_TYPE = Type.getType(ObjCObject.class);
    public static final Type POINTER_TYPE = Type.getType(Pointer.class);
    public static final Type NATIVE_VALUE_TYPE = Type.getType(NativeValue.class);
    public static final Type NATIVE_VALUE_ARRAY_TYPE = Type.getType(NativeValue[].class);
    public static final Type CALLBACK_FUNCTION_TYPE = Type.getType(CallbackFunction.class);
    public static final Type PRIMITIVE_VALUE_TYPE = Type.getType(PrimitiveValue.class);

    private final JetTypeMapper typeMapper;
    private final ClassDescriptor descriptor;
    private final File dylib;

    private final ClassWriter cw;
    private final Type asmType;

    private final Type classObjectAsmType;

    private final Type superClassAsmType;
    private final String[] superInterfaceNames;

    public ObjCClassCodegen(@NotNull JetTypeMapper typeMapper, @NotNull ClassDescriptor descriptor, @NotNull File dylib) {
        this.typeMapper = typeMapper;
        this.descriptor = descriptor;
        this.dylib = dylib;

        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        asmType = typeMapper.mapType(descriptor.getDefaultType(), JetTypeMapperMode.IMPL);

        ClassDescriptor classObject = descriptor.getClassObjectDescriptor();
        classObjectAsmType = classObject != null ? typeMapper.mapType(classObject) : null;

        superClassAsmType = computeSuperClassAsmType();
        superInterfaceNames = computeSuperInterfaceNames();
    }

    private interface MethodCodegen {
        void generate(@NotNull InstructionAdapter v);
    }

    private void newMethod(int flags, @NotNull String name, @NotNull String descriptor, @NotNull MethodCodegen codegen) {
        if (this.descriptor.getKind() == ClassKind.TRAIT) return;

        MethodVisitor mv = cw.visitMethod(flags, name, descriptor, null, null);
        mv.visitCode();
        codegen.generate(new InstructionAdapter(mv));
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    @NotNull
    public byte[] generateClass() {
        cw.visit(V1_6,
                 computeAccessFlagsForClass(),
                 asmType.getInternalName(),
                 null,
                 superClassAsmType.getInternalName(),
                 superInterfaceNames
        );

        cw.visitSource(null, null);

        if (classObjectAsmType != null) {
            cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, JvmAbi.CLASS_OBJECT_FIELD, classObjectAsmType.getDescriptor(), null, null);
        }

        generateStaticInitializer();

        generateConstructor();

        JetScope scope = descriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        for (DeclarationDescriptor member : scope.getAllDescriptors()) {
            if (member instanceof FunctionDescriptor) {
                generateMethod((FunctionDescriptor) member);
            }
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private int computeAccessFlagsForClass() {
        int access = ACC_PUBLIC;
        if (descriptor.getKind() == ClassKind.TRAIT) {
            access |= ACC_ABSTRACT | ACC_INTERFACE;
        }
        else {
            access |= ACC_SUPER;
        }
        return access;
    }

    @NotNull
    private Type computeSuperClassAsmType() {
        for (JetType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            ClassifierDescriptor superDescriptor = supertype.getConstructor().getDeclarationDescriptor();
            assert superDescriptor instanceof ClassDescriptor : "Supertype is not a class for Obj-C descriptor: " + descriptor;
            if (((ClassDescriptor) superDescriptor).getKind() == ClassKind.CLASS) {
                return typeMapper.mapType(supertype, JetTypeMapperMode.IMPL);
            }
        }

        if (descriptor.getKind() == ClassKind.TRAIT) {
            return JL_OBJECT_TYPE;
        }

        return OBJC_OBJECT_TYPE;
    }

    @NotNull
    private String[] computeSuperInterfaceNames() {
        Collection<JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();

        List<String> superInterfacesNames = new ArrayList<String>(supertypes.size());
        for (JetType supertype : supertypes) {
            ClassifierDescriptor superDescriptor = supertype.getConstructor().getDeclarationDescriptor();
            assert superDescriptor instanceof ClassDescriptor : "Supertype is not a class for Obj-C descriptor: " + descriptor;
            if (((ClassDescriptor) superDescriptor).getKind() == ClassKind.TRAIT) {
                Type type = typeMapper.mapType(superDescriptor.getDefaultType(), JetTypeMapperMode.IMPL);
                superInterfacesNames.add(type.getInternalName());
            }
        }

        return superInterfacesNames.toArray(new String[superInterfacesNames.size()]);
    }

    private void generateStaticInitializer() {
        newMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", getMethodDescriptor(VOID_TYPE), new MethodCodegen() {
            @Override
            public void generate(@NotNull InstructionAdapter v) {
                if (classObjectAsmType != null) {
                    genInitSingletonField(asmType, JvmAbi.CLASS_OBJECT_FIELD, classObjectAsmType, v);
                }
                v.visitLdcInsn(dylib.toString());
                v.invokestatic(JET_RUNTIME_OBJC, "loadLibrary", getMethodDescriptor(VOID_TYPE, JL_STRING_TYPE));
                v.areturn(VOID_TYPE);
            }
        });
    }

    private void generateConstructor() {
        final String objcObjectConstructor = getMethodDescriptor(VOID_TYPE, ID_TYPE);
        if (descriptor.getKind() == ClassKind.CLASS_OBJECT) {
            newMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE), new MethodCodegen() {
                @Override
                public void generate(@NotNull InstructionAdapter v) {
                    v.load(0, asmType);
                    v.visitLdcInsn(descriptor.getContainingDeclaration().getName().getName());
                    v.invokestatic(JET_RUNTIME_OBJC, "getClass", getMethodDescriptor(ID_TYPE, JL_STRING_TYPE));
                    v.invokespecial(superClassAsmType.getInternalName(), "<init>", objcObjectConstructor);
                    v.areturn(VOID_TYPE);
                }
            });
        }
        else {
            newMethod(ACC_PUBLIC, "<init>", objcObjectConstructor, new MethodCodegen() {
                @Override
                public void generate(@NotNull InstructionAdapter v) {
                    v.load(0, asmType);
                    v.load(1, ID_TYPE);
                    v.invokespecial(superClassAsmType.getInternalName(), "<init>", objcObjectConstructor);
                    v.areturn(VOID_TYPE);
                }
            });
        }
    }

    private void generateMethod(@NotNull final FunctionDescriptor method) {
        final JvmMethodSignature signature = typeMapper.mapSignature(method.getName(), method);

        newMethod(ACC_PUBLIC, signature.getName(), signature.getAsmMethod().getDescriptor(), new MethodCodegen() {
            @Override
            public void generate(@NotNull InstructionAdapter v) {
                v.load(0, asmType);
                v.getfield(OBJC_OBJECT_TYPE.getInternalName(), "id", ID_TYPE.getDescriptor());

                v.visitLdcInsn(getObjCMethodName(method));

                putArgumentsAsNativeValueArray(v);

                String sendMessageNameSuffix;
                Type sendMessageReturnType;

                Type returnType = signature.getAsmMethod().getReturnType();
                if (returnType.getSort() == Type.INT) {
                    sendMessageNameSuffix = "Int";
                    sendMessageReturnType = INT_TYPE;
                }
                else if (returnType.getSort() == Type.LONG) {
                    sendMessageNameSuffix = "Long";
                    sendMessageReturnType = LONG_TYPE;
                }
                else if (returnType.getSort() == Type.SHORT) {
                    sendMessageNameSuffix = "Short";
                    sendMessageReturnType = SHORT_TYPE;
                }
                else if (returnType.getSort() == Type.CHAR) {
                    sendMessageNameSuffix = "Char";
                    sendMessageReturnType = CHAR_TYPE;
                }
                else if (returnType.getSort() == Type.BOOLEAN) {
                    sendMessageNameSuffix = "Boolean";
                    sendMessageReturnType = BOOLEAN_TYPE;
                }
                else if (returnType.getSort() == Type.DOUBLE) {
                    sendMessageNameSuffix = "Double";
                    sendMessageReturnType = DOUBLE_TYPE;
                }
                else if (returnType.getSort() == Type.FLOAT) {
                    sendMessageNameSuffix = "Float";
                    sendMessageReturnType = FLOAT_TYPE;
                }
                else if (returnType.equals(POINTER_TYPE)) {
                    sendMessageNameSuffix = "Pointer";
                    sendMessageReturnType = POINTER_TYPE;
                }
                else if (returnType.getSort() == Type.OBJECT) {
                    sendMessageNameSuffix = "ObjCObject";
                    sendMessageReturnType = OBJC_OBJECT_TYPE;
                }
                else {
                    // TODO
                    sendMessageNameSuffix = "Void";
                    sendMessageReturnType = VOID_TYPE;
                }

                v.invokestatic(JET_RUNTIME_OBJC, "sendMessage" + sendMessageNameSuffix,
                               getMethodDescriptor(sendMessageReturnType, ID_TYPE, JL_STRING_TYPE, NATIVE_VALUE_ARRAY_TYPE));
                StackValue.coerce(sendMessageReturnType, returnType, v);

                v.areturn(returnType);
            }

            private void putArgumentsAsNativeValueArray(@NotNull InstructionAdapter v) {
                List<ValueParameterDescriptor> parameters = method.getValueParameters();

                v.iconst(parameters.size());
                v.newarray(NATIVE_VALUE_TYPE);

                int localIndex = 1;
                for (ValueParameterDescriptor parameter : parameters) {
                    JetType type = parameter.getType();
                    Type asmType = typeMapper.mapType(type);
                    int i = parameter.getIndex();

                    v.dup();
                    v.iconst(i);

                    StackValue local = StackValue.local(localIndex, asmType);
                    localIndex += asmType.getSize();

                    if (KotlinBuiltIns.getInstance().isFunctionType(type)) {
                        List<TypeProjection> projections = type.getArguments();
                        // -1 for return type
                        int arity = projections.size() - 1;

                        v.anew(CALLBACK_FUNCTION_TYPE);
                        v.dup();
                        local.put(JL_OBJECT_TYPE, v);
                        v.iconst(arity);
                        v.invokespecial(CALLBACK_FUNCTION_TYPE.getInternalName(), "<init>",
                                        getMethodDescriptor(VOID_TYPE, JL_OBJECT_TYPE, INT_TYPE));
                    }
                    else if (ObjCBuiltIns.getInstance().isPointerType(type)) {
                        local.put(POINTER_TYPE, v);
                    }
                    else if (asmType.getSort() == Type.OBJECT) {
                        local.put(OBJC_OBJECT_TYPE, v);
                    }
                    else {
                        v.anew(PRIMITIVE_VALUE_TYPE);
                        v.dup();
                        if (asmType.getSort() == Type.DOUBLE) {
                            local.put(DOUBLE_TYPE, v);
                            // TODO: or doubleToLongBits?
                            v.invokestatic("java/lang/Double", "doubleToRawLongBits", "(D)J");
                        }
                        else if (asmType.getSort() == Type.FLOAT) {
                            local.put(FLOAT_TYPE, v);
                            // TODO: or floatToIntBits?
                            v.invokestatic("java/lang/Float", "floatToRawIntBits", "(F)I");
                            StackValue.coerce(INT_TYPE, LONG_TYPE, v);
                        }
                        else {
                            local.put(LONG_TYPE, v);
                        }
                        v.invokespecial(PRIMITIVE_VALUE_TYPE.getInternalName(), "<init>", getMethodDescriptor(VOID_TYPE, LONG_TYPE));
                    }

                    v.astore(NATIVE_VALUE_TYPE);
                }
            }
        });
    }

    @NotNull
    private static String getObjCMethodName(@NotNull FunctionDescriptor method) {
        FunctionDescriptor unwrapped = CodegenUtil.unwrapFakeOverride(method);
        assert unwrapped instanceof ObjCMethodDescriptor : "Obj-C method original is not an Obj-C method: " + method + ", " + unwrapped;
        return ((ObjCMethodDescriptor) unwrapped).getObjCName();
    }
}
