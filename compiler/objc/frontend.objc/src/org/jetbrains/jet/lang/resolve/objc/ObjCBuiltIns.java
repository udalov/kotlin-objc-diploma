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
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;

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

    private ObjCBuiltIns(@NotNull DependencyClassByQualifiedNameResolver resolver) {
    }
}
