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
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.objc.descriptors.ObjCNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.*;

public class ObjCDescriptorMapper {
    private final ObjCNamespaceDescriptor namespace;

    public ObjCDescriptorMapper(@NotNull ObjCNamespaceDescriptor namespace) {
        this.namespace = namespace;
    }

    @NotNull
    public ClassDescriptor mapClass(@NotNull ObjCClass clazz) {
        Name name = Name.identifier(clazz.getName());
        TempClassDescriptor descriptor = new TempClassDescriptor(namespace, ClassKind.CLASS, name);

        addMethodsToClassScope(clazz.getMethodList(), descriptor, false);
        descriptor.getWritableScope().changeLockLevel(WritableScope.LockLevel.READING);

        return descriptor;
    }

    private void addMethodsToClassScope(
            @NotNull List<ObjCMethod> methods,
            @NotNull TempClassDescriptor descriptor,
            boolean classMethods
    ) {
        WritableScope scope = descriptor.getWritableScope();
        for (ObjCMethod method : methods) {
            if (method.getClassMethod() == classMethods) {
                FunctionDescriptor functionDescriptor = mapMethod(method, descriptor);
                scope.addFunctionDescriptor(functionDescriptor);
            }
        }
    }

    @NotNull
    public ClassDescriptor mapProtocol(@NotNull ObjCProtocol protocol, @NotNull Name name) {
        TempClassDescriptor descriptor = new TempClassDescriptor(namespace, ClassKind.TRAIT, name);

        addMethodsToClassScope(protocol.getMethodList(), descriptor, false);
        descriptor.getWritableScope().changeLockLevel(WritableScope.LockLevel.READING);

        return descriptor;
    }

    @NotNull
    private FunctionDescriptor mapMethod(@NotNull ObjCMethod method, @NotNull TempClassDescriptor containingClass) {
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
        public TempClassDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull ClassKind kind, @NotNull Name name) {
            super(containingDeclaration, kind, false);

            setName(name);
            setModality(Modality.OPEN);
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

        @NotNull
        private WritableScope getWritableScope() {
            return (WritableScope) getScopeForMemberLookup();
        }
    }
}
