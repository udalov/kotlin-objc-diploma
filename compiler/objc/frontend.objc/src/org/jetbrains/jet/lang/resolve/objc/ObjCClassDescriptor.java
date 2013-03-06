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
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import java.util.Collection;
import java.util.Collections;

public class ObjCClassDescriptor extends MutableClassDescriptorLite {
    public ObjCClassDescriptor(
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
