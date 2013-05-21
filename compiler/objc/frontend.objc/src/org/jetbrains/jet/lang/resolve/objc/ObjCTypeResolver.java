package org.jetbrains.jet.lang.resolve.objc;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DeferredTypeBase;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private static JetType createFunctionType(@NotNull final List<JetType> paramTypes, @NotNull final JetType returnType) {
        if (paramTypes.size() > KotlinBuiltIns.FUNCTION_TRAIT_COUNT) {
            throw new UnsupportedOperationException("Function types with more than " + KotlinBuiltIns.FUNCTION_TRAIT_COUNT +
                                                    " parameters are not supported");
        }

        return new DeferredFunctionType(new RecursionIntolerantLazyValue<JetType>() {
            @Override
            protected JetType compute() {
                return KotlinBuiltIns.getInstance().getFunctionType(
                        Collections.<AnnotationDescriptor>emptyList(),
                        /* receiverType */ null,
                        paramTypes,
                        returnType
                );
            }
        });
    }

    private static class DeferredFunctionType extends DeferredTypeBase {
        public DeferredFunctionType(@NotNull RecursionIntolerantLazyValue<JetType> lazyValue) {
            super(lazyValue);
        }
    }

    private class TypeParser {
        private final String type;
        private int at;

        public TypeParser(@NotNull String type) {
            this.type = type;
            this.at = 0;
        }

        private boolean at(@NotNull String s) {
            return type.substring(at).startsWith(s);
        }

        private void expect(@NotNull String s) {
            if (!advance(s)) error("Expecting <" + s + "> (at=" + at + ")");
        }

        private boolean advance(@NotNull String s) {
            if (at(s)) {
                at += s.length();
                return true;
            }
            return false;
        }

        private void error(@NotNull String s) {
            throw new IllegalStateException(s + ": " + type);
        }

        @NotNull
        public JetType parse() {
            if (at == type.length()) error("No type to parse");

            for (Map.Entry<String, JetType> entry : BUILT_IN_TYPES.entrySet()) {
                if (advance(entry.getKey())) return entry.getValue();
            }

            if (advance("L")) {
                int semicolon = type.indexOf(';', at);
                if (semicolon < 0) error("L without a matching semicolon");
                String className = type.substring(at, semicolon);
                expect(className);
                expect(";");
                // TODO: for some reason Clang doesn't index forward declaration of the class named 'Protocol' defined in objc/Protocol.h
                if ("Protocol".equals(className)) return KotlinBuiltIns.getInstance().getNullableAnyType();
                return createTypeForClass(Name.identifier(className));
            }

            if (advance("*(")) {
                List<JetType> paramTypes = new ArrayList<JetType>();
                while (!advance(")")) {
                    if (advance(".")) {
                        // TODO: support vararg
                        continue;
                    }
                    paramTypes.add(parse());
                }
                JetType returnType = parse();
                expect(";");
                return createFunctionType(paramTypes, returnType);
            }

            if (advance("*V;")) {
                // Special case for "void *"
                return ObjCBuiltIns.getInstance().getOpaquePointerType();
            }

            if (advance("OI")) {
                return ObjCBuiltIns.getInstance().getObjCObjectClass().getDefaultType();
            }

            if (advance("OC")) {
                return ObjCBuiltIns.getInstance().getObjCClassClass().getDefaultType();
            }

            if (advance("OS")) {
                return ObjCBuiltIns.getInstance().getObjCSelectorClass().getDefaultType();
            }

            if (advance("*")) {
                JetType pointee = parse();
                expect(";");
                return ObjCBuiltIns.getInstance().getPointerType(pointee);
            }

            if (at("X(")) {
                at = type.indexOf(')', at) + 1;
            }
            else {
                throw new UnsupportedOperationException("Unsupported type (at=" + at + "): " + type);
            }

            return KotlinBuiltIns.getInstance().getNullableAnyType();
        }
    }

    @NotNull
    public JetType resolveType(@NotNull String type) {
        return new TypeParser(type).parse();
    }
}
