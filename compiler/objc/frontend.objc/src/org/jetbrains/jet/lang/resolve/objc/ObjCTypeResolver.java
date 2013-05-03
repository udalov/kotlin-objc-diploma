package org.jetbrains.jet.lang.resolve.objc;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Map;

public class ObjCTypeResolver {
    private final NamespaceDescriptorImpl namespace;

    public ObjCTypeResolver(@NotNull NamespaceDescriptorImpl namespace) {
        this.namespace = namespace;
    }

    private static final Map<String, JetType> BUILT_IN_TYPES;

    static {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        BUILT_IN_TYPES = new ContainerUtil.ImmutableMapBuilder<String, JetType>()
                .put("V", builtIns.getUnitType())
                .put("UC", builtIns.getCharType())
                .put("US", builtIns.getShortType())
                .put("UI", builtIns.getIntType())
                .put("UJ", builtIns.getLongType())
                .put("C", builtIns.getCharType())
                .put("Z", builtIns.getBooleanType())
                .put("S", builtIns.getShortType())
                .put("I", builtIns.getIntType())
                .put("J", builtIns.getLongType())
                .put("F", builtIns.getFloatType())
                .put("D", builtIns.getDoubleType())
                .build();
    }

    @NotNull
    public JetType createTypeForClass(@NotNull Name className) {
        return new ObjCDeferredType(namespace, className);
    }

    @NotNull
    public JetType resolveType(@NotNull String type) {
        JetType builtInType = BUILT_IN_TYPES.get(type);
        if (builtInType != null) {
            return builtInType;
        }

        if ("LProtocol;".equals(type)) {
            // TODO: for some reason Clang doesn't index forward declaration of the class named 'Protocol' in objc/Protocol.h
            return KotlinBuiltIns.getInstance().getNullableAnyType();
        }

        if (type.startsWith("L")) {
            String className = type.substring(1, type.indexOf(';'));
            return createTypeForClass(Name.identifier(className));
        }

        return KotlinBuiltIns.getInstance().getNullableAnyType();
    }
}
