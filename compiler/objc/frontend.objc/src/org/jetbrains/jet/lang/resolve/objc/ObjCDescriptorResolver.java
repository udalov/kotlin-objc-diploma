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
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValue;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder.ClassObjectStatus;
import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.*;

public class ObjCDescriptorResolver {
    private static final Name OBJC_NAMESPACE_NAME = Name.identifier("objc");

    private static final String PROTOCOL_NAME_SUFFIX = "Protocol";

    private final NamespaceDescriptorImpl namespace;
    private final Map<String, Name> protocolNames = new HashMap<String, Name>();

    public ObjCDescriptorResolver(@NotNull NamespaceDescriptor rootNamespace) {
        namespace = new NamespaceDescriptorImpl(rootNamespace, Collections.<AnnotationDescriptor>emptyList(), OBJC_NAMESPACE_NAME);
        WritableScope scope = new WritableScopeImpl(JetScope.EMPTY, namespace, RedeclarationHandler.THROW_EXCEPTION, "objc scope");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        namespace.initialize(scope);

        rootNamespace.addNamespace(namespace);
    }

    @NotNull
    public NamespaceDescriptor resolveTranslationUnit(@NotNull TranslationUnit translationUnit) {
        WritableScope scope = namespace.getMemberScope();

        for (ObjCClass clazz : translationUnit.getClassList()) {
            ClassDescriptor classDescriptor = resolveClass(clazz);
            scope.addClassifierAlias(classDescriptor.getName(), classDescriptor);
        }

        for (ObjCProtocol protocol : translationUnit.getProtocolList()) {
            ClassDescriptor classDescriptor = resolveProtocol(protocol);
            scope.addClassifierAlias(classDescriptor.getName(), classDescriptor);
        }

        return namespace;
    }

    @NotNull
    private ClassDescriptor resolveClass(@NotNull ObjCClass clazz) {
        Name name = Name.identifier(clazz.getName());

        Collection<JetType> supertypes;
        if (clazz.hasBaseClass()) {
            JetType supertype = createDeferredSupertype(name, Name.identifier(clazz.getBaseClass()));
            supertypes = Collections.singletonList(supertype);
        }
        else {
            supertypes = Collections.emptyList();
        }

        ObjCClassDescriptor descriptor = new ObjCClassDescriptor(namespace, ClassKind.CLASS, Modality.OPEN, name, supertypes);
        processMethodsOfClassOrProtocol(clazz.getMethodList(), descriptor);
        return descriptor;
    }

    @NotNull
    private JetType createDeferredSupertype(@NotNull final Name className, @NotNull final Name baseClassName) {
        return new ObjCDeferredType(new RecursionIntolerantLazyValue<JetType>() {
            @Override
            protected JetType compute() {
                JetScope scope = namespace.getMemberScope();
                ClassifierDescriptor classifier = scope.getClassifier(baseClassName);
                assert classifier != null : "Super class is not resolved for class: " + className + ", base: " + baseClassName;
                return classifier.getDefaultType();
            }
        });
    }

    @NotNull
    private ClassDescriptor resolveProtocol(@NotNull ObjCProtocol protocol) {
        Name name = nameForProtocol(protocol.getName());

        List<JetType> supertypes = new ArrayList<JetType>(protocol.getBaseProtocolCount());
        for (String baseProtocolName : protocol.getBaseProtocolList()) {
            Name baseName = nameForProtocol(baseProtocolName);
            JetType supertype = createDeferredSupertype(name, baseName);
            supertypes.add(supertype);
        }

        ObjCClassDescriptor descriptor = new ObjCClassDescriptor(namespace, ClassKind.TRAIT, Modality.ABSTRACT, name, supertypes);
        processMethodsOfClassOrProtocol(protocol.getMethodList(), descriptor);
        return descriptor;
    }

    @NotNull
    private Name nameForProtocol(@NotNull String protocolName) {
        if (protocolNames.containsKey(protocolName)) {
            return protocolNames.get(protocolName);
        }

        Name name = Name.identifier(protocolName);
        if (namespace.getMemberScope().getClassifier(name) != null) {
            // Since Objective-C classes and protocols exist in different namespaces and Kotlin classes and traits don't,
            // we invent a new name here for the trait when a class with the same name exists already
            // TODO: handle collisions (where both classes X and XProtocol and a protocol X exist)
            name = Name.identifier(protocolName + PROTOCOL_NAME_SUFFIX);
        }

        protocolNames.put(protocolName, name);
        return name;
    }

    private void processMethodsOfClassOrProtocol(@NotNull List<ObjCMethod> methods, @NotNull ObjCClassDescriptor descriptor) {
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
            ObjCClassDescriptor classObject = new ObjCClassDescriptor(descriptor, ClassKind.CLASS_OBJECT, Modality.FINAL, name,
                    Collections.<JetType>emptyList() /* TODO: does class object need to subclass from anything, e.g. NSObject? */);
            addMethodsToClassScope(classMethods, classObject);

            ClassObjectStatus result = descriptor.getBuilder().setClassObjectDescriptor(classObject);
            assert result == ClassObjectStatus.OK : result;
        }

        descriptor.lockScopes();
    }

    private void addMethodsToClassScope(@NotNull List<ObjCMethod> methods, @NotNull ObjCClassDescriptor descriptor) {
        NamespaceLikeBuilder builder = descriptor.getBuilder();
        for (ObjCMethod method : methods) {
            SimpleFunctionDescriptor functionDescriptor = resolveMethod(method, descriptor);
            builder.addFunctionDescriptor(functionDescriptor);
        }
    }

    @NotNull
    private SimpleFunctionDescriptor resolveMethod(@NotNull ObjCMethod method, @NotNull ObjCClassDescriptor containingClass) {
        Function function = method.getFunction();
        Name name = transformMethodName(function.getName());
        SimpleFunctionDescriptorImpl descriptor = new SimpleFunctionDescriptorImpl(containingClass,
                Collections.<AnnotationDescriptor>emptyList(), name, CallableMemberDescriptor.Kind.DECLARATION);

        int params = function.getParameterCount();
        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(params);
        for (int i = 0; i < params; i++) {
            ValueParameterDescriptor parameter = resolveFunctionParameter(function.getParameter(i), descriptor, i);
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
    private ValueParameterDescriptor resolveFunctionParameter(
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
            .put("ULong", KotlinBuiltIns.getInstance().getLongType())
            .put("ULongLong", KotlinBuiltIns.getInstance().getLongType())
            .put("Short", KotlinBuiltIns.getInstance().getShortType())
            .put("Int", KotlinBuiltIns.getInstance().getIntType())
            .put("Long", KotlinBuiltIns.getInstance().getLongType())
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
