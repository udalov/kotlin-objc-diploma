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

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder.ClassObjectStatus;
import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.*;

public class ObjCDescriptorMapper {
    private final NamespaceDescriptor namespace;

    public ObjCDescriptorMapper(@NotNull NamespaceDescriptor namespace) {
        this.namespace = namespace;
    }

    @NotNull
    public ClassDescriptor mapClass(@NotNull ObjCClass clazz) {
        Name name = Name.identifier(clazz.getName());
        ObjCClassDescriptor descriptor = new ObjCClassDescriptor(namespace, ClassKind.CLASS, Modality.OPEN, name);
        resolveAndAddMethodsToClassOrProtocol(clazz.getMethodList(), descriptor);
        return descriptor;
    }

    @NotNull
    public ClassDescriptor mapProtocol(@NotNull ObjCProtocol protocol, @NotNull Name name) {
        ObjCClassDescriptor descriptor = new ObjCClassDescriptor(namespace, ClassKind.TRAIT, Modality.ABSTRACT, name);
        resolveAndAddMethodsToClassOrProtocol(protocol.getMethodList(), descriptor);
        return descriptor;
    }

    private void resolveAndAddMethodsToClassOrProtocol(@NotNull List<ObjCMethod> methods, @NotNull ObjCClassDescriptor descriptor) {
        List<ObjCMethod> classMethods = new ArrayList<ObjCMethod>();
        List<ObjCMethod> instanceMethods = new ArrayList<ObjCMethod>();
        for (ObjCMethod method : methods) {
            if (method.getClassMethod()) {
                classMethods.add(method);
            }
            else {
                instanceMethods.add(method);
            }
        }

        addMethodsToClassScope(instanceMethods, descriptor);

        if (!classMethods.isEmpty()) {
            Name name = DescriptorUtils.getClassObjectName(descriptor.getName());
            ObjCClassDescriptor classObject = new ObjCClassDescriptor(namespace, ClassKind.CLASS_OBJECT, Modality.FINAL, name);
            addMethodsToClassScope(classMethods, classObject);

            ClassObjectStatus result = descriptor.getBuilder().setClassObjectDescriptor(classObject);
            assert result == ClassObjectStatus.OK : result;
        }

        descriptor.lockScopes();
    }

    private void addMethodsToClassScope(@NotNull List<ObjCMethod> methods, @NotNull ObjCClassDescriptor descriptor) {
        NamespaceLikeBuilder builder = descriptor.getBuilder();
        for (ObjCMethod method : methods) {
            SimpleFunctionDescriptor functionDescriptor = mapMethod(method, descriptor);
            builder.addFunctionDescriptor(functionDescriptor);
        }
    }

    @NotNull
    private SimpleFunctionDescriptor mapMethod(@NotNull ObjCMethod method, @NotNull ObjCClassDescriptor containingClass) {
        Function function = method.getFunction();
        Name name = transformMethodName(function.getName());
        SimpleFunctionDescriptorImpl descriptor = new SimpleFunctionDescriptorImpl(containingClass,
                Collections.<AnnotationDescriptor>emptyList(), name, CallableMemberDescriptor.Kind.DECLARATION);

        int params = function.getParameterCount();
        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(params);
        for (int i = 0; i < params; i++) {
            ValueParameterDescriptor parameter = mapFunctionParameter(function.getParameter(i), descriptor, i);
            valueParameters.add(parameter);
        }

        JetType returnType = newTempType(function.getReturnType());

        descriptor.initialize(
                /* receiverParameterType */ null,
                containingClass.getThisAsReceiverParameter(),
                Collections.<TypeParameterDescriptor>emptyList(),
                valueParameters,
                returnType,
                Modality.OPEN,
                Visibilities.PUBLIC,
                /* isInline */ false
        );

        return descriptor;
    }

    @NotNull
    private Name transformMethodName(@NotNull String name) {
        // Objective-C method names are usually of form 'methodName:withParam:andOtherParam:'
        // Here we strip away everything but the first part
        // TODO: handle methods with the same effective signature or invent something different
        int colon = name.indexOf(':');
        String beforeColon = colon < 0 ? name : name.substring(0, colon);
        return Name.identifier(beforeColon);
    }

    @NotNull
    private ValueParameterDescriptor mapFunctionParameter(
            @NotNull Function.Parameter parameter,
            @NotNull FunctionDescriptor containingFunction,
            int index
    ) {
        Name name = Name.identifier(parameter.getName());
        return new ValueParameterDescriptorImpl(
                containingFunction,
                index,
                Collections.<AnnotationDescriptor>emptyList(),
                name,
                /* isVar */ false,
                newTempType(parameter.getType()),
                /* declaresDefaultValue */ false,
                null
        );
    }

    private static final Map<String, JetType> BUILT_IN_TYPES = new ContainerUtil.ImmutableMapBuilder<String, JetType>()
            .put("Void", KotlinBuiltIns.getInstance().getUnitType())
            .put("Bool", KotlinBuiltIns.getInstance().getBooleanType())
            .put("UShort", KotlinBuiltIns.getInstance().getShortType())
            .put("UInt", KotlinBuiltIns.getInstance().getIntType())
            .put("ULong", KotlinBuiltIns.getInstance().getIntType())
            .put("ULongLong", KotlinBuiltIns.getInstance().getLongType())
            .put("Short", KotlinBuiltIns.getInstance().getShortType())
            .put("Int", KotlinBuiltIns.getInstance().getIntType())
            .put("Long", KotlinBuiltIns.getInstance().getIntType())
            .put("LongLong", KotlinBuiltIns.getInstance().getLongType())
            .put("Float", KotlinBuiltIns.getInstance().getFloatType())
            .put("Double", KotlinBuiltIns.getInstance().getDoubleType())
            .build();

    @NotNull
    private JetType newTempType(@NotNull String name) {
        JetType builtInType = BUILT_IN_TYPES.get(name);
        return builtInType != null ? builtInType : KotlinBuiltIns.getInstance().getNullableAnyType();
    }
}
