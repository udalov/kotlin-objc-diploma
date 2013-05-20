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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValue;

import java.util.Collections;
import java.util.List;

public class ObjCBuiltIns {
    private static ObjCBuiltIns instance = null;

    public static void initialize(@NotNull DependencyClassByQualifiedNameResolver resolver) {
        // TODO: not very good, use injectors or something instead
        instance = new ObjCBuiltIns(resolver);
    }

    @NotNull
    public static ObjCBuiltIns getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Not initialized yet");
        }
        return instance;
    }

    private final ClassDescriptor pointerClass;
    private final ClassDescriptor objcObjectClass;

    private ObjCBuiltIns(@NotNull DependencyClassByQualifiedNameResolver resolver) {
        pointerClass = resolver.resolveClass(new FqName("jet.objc.Pointer"));
        objcObjectClass = resolver.resolveClass(new FqName("jet.objc.ObjCObject"));
    }

    private static class BuiltInType extends DeferredTypeBase {
        protected BuiltInType(@NotNull final ClassDescriptor descriptor, @NotNull final List<TypeProjection> projections) {
            super(new RecursionIntolerantLazyValue<JetType>() {
                @Override
                protected JetType compute() {
                    return new JetTypeImpl(
                            Collections.<AnnotationDescriptor>emptyList(),
                            descriptor.getTypeConstructor(),
                            false,
                            projections,
                            descriptor.getMemberScope(projections)
                    );
                }
            });
        }
    }

    @NotNull
    public JetType getPointerType(@NotNull JetType pointee) {
        return new BuiltInType(pointerClass, Collections.singletonList(new TypeProjection(Variance.INVARIANT, pointee)));
    }

    @NotNull
    public JetType getOpaquePointerType() {
        TypeProjection projection = SubstitutionUtils.makeStarProjection(pointerClass.getTypeConstructor().getParameters().get(0));
        return new BuiltInType(pointerClass, Collections.singletonList(projection));
    }

    public boolean isPointerType(@NotNull JetType type) {
        return pointerClass == type.getConstructor().getDeclarationDescriptor();
    }

    @NotNull
    public ClassDescriptor getObjCObjectClass() {
        return objcObjectClass;
    }
}
