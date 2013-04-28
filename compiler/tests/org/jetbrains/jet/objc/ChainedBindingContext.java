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

package org.jetbrains.jet.objc;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ChainedBindingContext implements BindingContext {
    private final List<BindingContext> contexts;

    public ChainedBindingContext(@NotNull BindingContext... contexts) {
        this.contexts = Arrays.asList(contexts);
    }

    @Override
    public Collection<Diagnostic> getDiagnostics() {
        throw new IllegalStateException();
    }

    @Nullable
    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        for (BindingContext context : contexts) {
            V value = context.get(slice, key);
            if (value != null) return value;
        }
        return null;
    }

    @NotNull
    @Override
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        throw new IllegalStateException();
    }

    @NotNull
    @Override
    public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
        throw new IllegalStateException();
    }
}
