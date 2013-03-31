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
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

public class ObjCModuleConfiguration implements ModuleConfiguration {
    private ObjCResolveFacade resolver;
    private ModuleConfiguration delegateConfiguration;

    @Inject
    public void setResolver(@NotNull ObjCResolveFacade resolver) {
        this.resolver = resolver;
    }

    @PostConstruct
    public void init() {
        this.delegateConfiguration = DefaultModuleConfiguration.createStandardConfiguration();
    }

    public ObjCResolveFacade getResolver() {
        return resolver;
    }

    @Override
    public List<ImportPath> getDefaultImports() {
        return delegateConfiguration.getDefaultImports();
    }

    @Override
    public void extendNamespaceScope(
            @NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope
    ) {
        if (DescriptorUtils.getFQName(namespaceDescriptor).isRoot()) {
            resolver.setRootNamespace(namespaceDescriptor);
            resolver.resolve();
        }
        delegateConfiguration.extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
    }

    @NotNull
    @Override
    public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
        return PlatformToKotlinClassMap.EMPTY;
    }
}
