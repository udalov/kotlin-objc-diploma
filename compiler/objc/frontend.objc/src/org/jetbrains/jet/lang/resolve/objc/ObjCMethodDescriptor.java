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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

public class ObjCMethodDescriptor extends SimpleFunctionDescriptorImpl {
    private final String objcName;

    public ObjCMethodDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String objcName
    ) {
        super(containingDeclaration,
              annotations,
              transformMethodName(objcName),
              Kind.DECLARATION);

        this.objcName = objcName;
    }

    @NotNull
    public String getObjCName() {
        return objcName;
    }

    @NotNull
    private static Name transformMethodName(@NotNull String name) {
        // Objective-C method names are usually of form 'methodName:withParam:andOtherParam:'
        // Here we strip away everything but the first part
        // TODO: handle methods with the same effective signature or invent something different
        int colon = name.indexOf(':');
        String beforeColon = colon < 0 ? name : name.substring(0, colon);
        return Name.identifier(beforeColon);
    }
}
