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
    public NamespaceDescriptor resolveTranslationUnit(@NotNull TranslationUnit tu) {
        calculateProtocolNames(tu);

        WritableScope scope = namespace.getMemberScope();

        List<ObjCClassDescriptor> classes = new ArrayList<ObjCClassDescriptor>(tu.getClassCount() + tu.getProtocolCount());

        for (ObjCClass clazz : tu.getClassList()) {
            ObjCClassDescriptor descriptor = resolveClass(clazz);
            classes.add(descriptor);
            scope.addClassifierAlias(descriptor.getName(), descriptor);

            ObjCClassDescriptor metaclass = resolveMetaclass(descriptor, clazz.getMethodList());
            classes.add(metaclass);
            scope.addClassifierAlias(metaclass.getName(), metaclass);

            resolveClassObject(descriptor, metaclass);
        }

        for (ObjCProtocol protocol : tu.getProtocolList()) {
            ObjCClassDescriptor descriptor = resolveProtocol(protocol);
            classes.add(descriptor);
            scope.addClassifierAlias(descriptor.getName(), descriptor);

            ObjCClassDescriptor metaclass = resolveMetaclass(descriptor, protocol.getMethodList());
            classes.add(metaclass);
            scope.addClassifierAlias(metaclass.getName(), metaclass);

            resolveClassObject(descriptor, metaclass);
        }

        for (ObjCClassDescriptor descriptor : classes) {
            descriptor.initialize();

            ObjCClassDescriptor classObject = descriptor.getClassObjectDescriptor();
            if (classObject != null) {
                classObject.initialize();
            }
        }

        new ObjCOverrideResolver().process(classes);

        for (ObjCClassDescriptor descriptor : classes) {
            descriptor.lockScopes();
        }

        return namespace;
    }

    private void calculateProtocolNames(@NotNull TranslationUnit tu) {
        Set<Name> existingNames = new HashSet<Name>();
        for (ObjCClass clazz : tu.getClassList()) {
            existingNames.add(Name.identifier(clazz.getName()));
        }

        for (ObjCProtocol protocol : tu.getProtocolList()) {
            String protocolName = protocol.getName();
            Name name = Name.identifier(protocolName);
            if (existingNames.contains(name)) {
                // Since Objective-C classes and protocols exist in different namespaces and Kotlin classes and traits don't,
                // we invent a new name here for the trait when a class with the same name exists already
                // TODO: handle collisions (where both classes X and XProtocol and a protocol X exist)
                name = Name.identifier(protocolName + PROTOCOL_NAME_SUFFIX);
            }

            protocolNames.put(protocolName, name);
            existingNames.add(name);
        }
    }

    @NotNull
    private Name nameForProtocol(@NotNull String protocolName) {
        return protocolNames.get(protocolName);
    }

    @NotNull
    private ObjCClassDescriptor resolveClass(@NotNull ObjCClass clazz) {
        Name name = Name.identifier(clazz.getName());

        List<JetType> supertypes = new ArrayList<JetType>(clazz.getProtocolCount() + 1);
        if (clazz.hasBaseClass()) {
            Name baseName = Name.identifier(clazz.getBaseClass());
            JetType supertype = createDeferredSupertype(baseName);
            supertypes.add(supertype);
        }

        for (String baseProtocolName : clazz.getProtocolList()) {
            Name baseName = nameForProtocol(baseProtocolName);
            JetType supertype = createDeferredSupertype(baseName);
            supertypes.add(supertype);
        }

        ObjCClassDescriptor descriptor = new ObjCClassDescriptor(namespace, ClassKind.CLASS, Modality.OPEN, name, supertypes);
        addMethodsToClassScope(clazz.getMethodList(), descriptor, MethodKind.INSTANCE_METHOD);

        return descriptor;
    }

    @NotNull
    private JetType createDeferredSupertype(@NotNull Name baseClassName) {
        return new ObjCDeferredType(namespace, baseClassName);
    }

    @NotNull
    private ObjCClassDescriptor resolveProtocol(@NotNull ObjCProtocol protocol) {
        Name name = nameForProtocol(protocol.getName());

        List<JetType> supertypes = new ArrayList<JetType>(protocol.getBaseProtocolCount());
        for (String baseProtocolName : protocol.getBaseProtocolList()) {
            Name baseName = nameForProtocol(baseProtocolName);
            JetType supertype = createDeferredSupertype(baseName);
            supertypes.add(supertype);
        }

        ObjCClassDescriptor descriptor = new ObjCClassDescriptor(namespace, ClassKind.TRAIT, Modality.ABSTRACT, name, supertypes);
        addMethodsToClassScope(protocol.getMethodList(), descriptor, MethodKind.INSTANCE_METHOD);

        return descriptor;
    }

    private static void resolveClassObject(@NotNull ObjCClassDescriptor descriptor, @NotNull ObjCClassDescriptor metaclass) {
        Name name = DescriptorUtils.getClassObjectName(descriptor.getName());
        Collection<JetType> supertypes = Collections.singletonList(metaclass.getDefaultType());

        ObjCClassDescriptor classObject = new ObjCClassDescriptor(descriptor, ClassKind.CLASS_OBJECT, Modality.FINAL, name, supertypes);

        ClassObjectStatus result = descriptor.getBuilder().setClassObjectDescriptor(classObject);
        assert result == ClassObjectStatus.OK : result;
    }

    @NotNull
    private ObjCMetaclassDescriptor resolveMetaclass(@NotNull ObjCClassDescriptor descriptor, @NotNull List<ObjCMethod> methods) {
        List<JetType> supertypes = createDeferredSupertypesForMetaclass(descriptor);

        ObjCMetaclassDescriptor metaclass = new ObjCMetaclassDescriptor(descriptor, supertypes);
        addMethodsToClassScope(methods, metaclass, MethodKind.CLASS_METHOD);

        return metaclass;
    }

    @NotNull
    private List<JetType> createDeferredSupertypesForMetaclass(@NotNull ObjCClassDescriptor descriptor) {
        List<JetType> supertypes = new ArrayList<JetType>(1);
        for (JetType supertype : descriptor.getLazySupertypes()) {
            assert supertype instanceof ObjCDeferredType : "Unexpected Obj-C supertype: " + supertype.getClass().getName();
            Name supertypeName = ((ObjCDeferredType) supertype).getClassName();
            Name superMetaName = ObjCMetaclassDescriptor.getMetaclassName(supertypeName);
            supertypes.add(createDeferredSupertype(superMetaName));
        }
        return supertypes;
    }

    private enum MethodKind {
        CLASS_METHOD,
        INSTANCE_METHOD;

        public boolean isKind(@NotNull ObjCMethod method) {
            return method.getClassMethod() == (this == CLASS_METHOD);
        }
    }

    private void addMethodsToClassScope(
            @NotNull List<ObjCMethod> methods,
            @NotNull ObjCClassDescriptor descriptor,
            @NotNull MethodKind kind
    ) {
        NamespaceLikeBuilder builder = descriptor.getBuilder();
        for (ObjCMethod method : methods) {
            if (kind.isKind(method)) {
                SimpleFunctionDescriptor functionDescriptor = resolveMethod(method, descriptor);
                builder.addFunctionDescriptor(functionDescriptor);
            }
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
