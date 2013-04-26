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
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptorLite;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ObjCClassDescriptor extends MutableClassDescriptorLite {
    private final Collection<JetType> lazySupertypes;

    // TODO: maybe reuse this from MutableClassDescriptor
    private final Set<CallableMemberDescriptor> declaredCallableMembers = new LinkedHashSet<CallableMemberDescriptor>();
    private final Set<CallableMemberDescriptor> allCallableMembers = new LinkedHashSet<CallableMemberDescriptor>();

    public ObjCClassDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ClassKind kind,
            @NotNull Modality modality,
            @NotNull Name name,
            @NotNull Collection<JetType> supertypes
    ) {
        super(containingDeclaration, kind, false);

        setName(name);
        setModality(modality);
        setVisibility(Visibilities.PUBLIC);

        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.THROW_EXCEPTION, "Obj-C class");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        setScopeForMemberLookup(scope);
        setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());

        this.lazySupertypes = supertypes;

        createTypeConstructor();
    }

    /* package */ void initialize() {
        // Initialization is a separate step, because addSupertype method actually computes deferred types.
        // Not all supertypes may be available at the time of constructing this class
        for (JetType supertype : lazySupertypes) {
            addSupertype(supertype);
        }
    }

    @NotNull
    public Set<CallableMemberDescriptor> getDeclaredCallableMembers() {
        return declaredCallableMembers;
    }

    @NotNull
    public Set<CallableMemberDescriptor> getAllCallableMembers() {
        return allCallableMembers;
    }

    @Nullable
    @Override
    public ObjCClassDescriptor getClassObjectDescriptor() {
        return (ObjCClassDescriptor) super.getClassObjectDescriptor();
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

    private NamespaceLikeBuilder builder = null;

    @Override
    public NamespaceLikeBuilder getBuilder() {
        if (builder == null) {
            final NamespaceLikeBuilder superBuilder = super.getBuilder();
            builder = new NamespaceLikeBuilder() {
                @NotNull
                @Override
                public DeclarationDescriptor getOwnerForChildren() {
                    return superBuilder.getOwnerForChildren();
                }

                @Override
                public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
                    superBuilder.addClassifierDescriptor(classDescriptor);
                }

                @Override
                public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
                    superBuilder.addObjectDescriptor(objectDescriptor);
                }

                @Override
                public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
                    superBuilder.addFunctionDescriptor(functionDescriptor);
                    if (functionDescriptor.getKind().isReal()) {
                        declaredCallableMembers.add(functionDescriptor);
                    }
                    allCallableMembers.add(functionDescriptor);
                }

                @Override
                public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
                    superBuilder.addPropertyDescriptor(propertyDescriptor);
                    if (propertyDescriptor.getKind().isReal()) {
                        declaredCallableMembers.add(propertyDescriptor);
                    }
                    allCallableMembers.add(propertyDescriptor);
                }

                @Override
                public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
                    return superBuilder.setClassObjectDescriptor(classObjectDescriptor);
                }
            };
        }

        return builder;
    }
}
