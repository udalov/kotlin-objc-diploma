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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptorLite;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.objc.descriptors.ObjCNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder.ClassObjectStatus;
import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.*;

public class ObjCDescriptorMapper {
    private final ObjCNamespaceDescriptor namespace;

    public ObjCDescriptorMapper(@NotNull ObjCNamespaceDescriptor namespace) {
        this.namespace = namespace;
    }

    @NotNull
    public ClassDescriptor mapClass(@NotNull ObjCClass clazz) {
        Name name = Name.identifier(clazz.getName());
        TempClassDescriptor descriptor = new TempClassDescriptor(namespace, ClassKind.CLASS, Modality.OPEN, name);

        List<ObjCMethod> classMethods = new ArrayList<ObjCMethod>();
        List<ObjCMethod> instanceMethods = new ArrayList<ObjCMethod>();
        filterClassAndInstanceMethods(clazz.getMethodList(), classMethods, instanceMethods);

        addMethodsToClassScope(instanceMethods, descriptor);
        createAndFillClassObjectIfNeeded(classMethods, descriptor);

        descriptor.lockScopes();

        return descriptor;
    }

    private void filterClassAndInstanceMethods(
            @NotNull List<ObjCMethod> methods,
            @NotNull List<ObjCMethod> classMethods,
            @NotNull List<ObjCMethod> instanceMethods
    ) {
        for (ObjCMethod method : methods) {
            if (method.getClassMethod()) {
                classMethods.add(method);
            }
            else {
                instanceMethods.add(method);
            }
        }
    }

    private void addMethodsToClassScope(@NotNull List<ObjCMethod> methods, @NotNull TempClassDescriptor descriptor) {
        NamespaceLikeBuilder builder = descriptor.getBuilder();
        for (ObjCMethod method : methods) {
            SimpleFunctionDescriptor functionDescriptor = mapMethod(method, descriptor);
            builder.addFunctionDescriptor(functionDescriptor);
        }
    }

    @NotNull
    public ClassDescriptor mapProtocol(@NotNull ObjCProtocol protocol, @NotNull Name name) {
        TempClassDescriptor descriptor = new TempClassDescriptor(namespace, ClassKind.TRAIT, Modality.ABSTRACT, name);

        List<ObjCMethod> classMethods = new ArrayList<ObjCMethod>();
        List<ObjCMethod> instanceMethods = new ArrayList<ObjCMethod>();
        filterClassAndInstanceMethods(protocol.getMethodList(), classMethods, instanceMethods);

        addMethodsToClassScope(instanceMethods, descriptor);
        createAndFillClassObjectIfNeeded(classMethods, descriptor);

        descriptor.lockScopes();

        return descriptor;
    }

    private void createAndFillClassObjectIfNeeded(@NotNull List<ObjCMethod> methods, @NotNull TempClassDescriptor descriptor) {
        if (methods.isEmpty()) return;

        Name name = DescriptorUtils.getClassObjectName(descriptor.getName());
        TempClassDescriptor classObject = new TempClassDescriptor(namespace, ClassKind.CLASS_OBJECT, Modality.FINAL, name);
        addMethodsToClassScope(methods, classObject);

        ClassObjectStatus result = descriptor.getBuilder().setClassObjectDescriptor(classObject);
        assert result == ClassObjectStatus.OK : result;
    }

    @NotNull
    private SimpleFunctionDescriptor mapMethod(@NotNull ObjCMethod method, @NotNull TempClassDescriptor containingClass) {
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

    @NotNull
    private JetType newTempType(@NotNull String name) {
        return KotlinBuiltIns.getInstance().getNullableAnyType();
    }

    private static class TempClassDescriptor extends MutableClassDescriptorLite {
        public TempClassDescriptor(
                @NotNull DeclarationDescriptor containingDeclaration,
                @NotNull ClassKind kind,
                @NotNull Modality modality,
                @NotNull Name name
        ) {
            super(containingDeclaration, kind, false);

            setName(name);
            setModality(modality);
            setVisibility(Visibilities.PUBLIC);

            WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.THROW_EXCEPTION, "Obj-C class");
            setScopeForMemberLookup(scope);
            setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());

            createTypeConstructor();
        }

        @NotNull
        @Override
        public Collection<ConstructorDescriptor> getConstructors() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
            return null;
        }
    }
}
