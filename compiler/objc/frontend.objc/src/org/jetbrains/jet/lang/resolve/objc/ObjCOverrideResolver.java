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
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ObjCOverrideResolver {
    private final Set<ObjCClassDescriptor> processed = new HashSet<ObjCClassDescriptor>();

    public void process(@NotNull Collection<ObjCClassDescriptor> classes) {
        for (ObjCClassDescriptor descriptor : classes) {
            generateOverrides(descriptor);

            ObjCClassDescriptor classObject = descriptor.getClassObjectDescriptor();
            if (classObject != null) {
                generateOverrides(classObject);
            }
        }
    }

    private void generateOverrides(@NotNull final ObjCClassDescriptor descriptor) {
        if (processed.contains(descriptor)) return;
        processed.add(descriptor);

        for (JetType supertype : descriptor.getSupertypes()) {
            ClassifierDescriptor classifier = supertype.getConstructor().getDeclarationDescriptor();
            assert classifier instanceof ObjCClassDescriptor : "Supertype of Obj-C class not an Obj-C class: " + classifier;
            generateOverrides((ObjCClassDescriptor) classifier);
        }

        OverrideResolver.generateOverridesInAClass(
                descriptor,
                descriptor.getDeclaredCallableMembers(),
                new OverrideResolver.DescriptorSink() {
                    @Override
                    public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                        if (fakeOverride instanceof SimpleFunctionDescriptor) {
                            descriptor.getBuilder().addFunctionDescriptor((SimpleFunctionDescriptor) fakeOverride);
                        }
                        else if (fakeOverride instanceof PropertyDescriptor) {
                            descriptor.getBuilder().addPropertyDescriptor((PropertyDescriptor) fakeOverride);
                        }
                        else {
                            throw new IllegalStateException("Unknown fake override: " + fakeOverride + " in " + descriptor);
                        }
                    }

                    @Override
                    public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                        throw new IllegalStateException("Override conflict: " + fromSuper + " vs " + fromCurrent);
                    }
                }
        );

        OverrideResolver.resolveUnknownVisibilities(descriptor.getAllCallableMembers(), new BindingTraceContext());
    }
}
