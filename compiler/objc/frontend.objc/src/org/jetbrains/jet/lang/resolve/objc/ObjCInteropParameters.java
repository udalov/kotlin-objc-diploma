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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ObjCInteropParameters {
    private ObjCInteropParameters() {}

    private final static Map<Project, String> HEADERS = new HashMap<Project, String>();

    @Nullable
    public static String getArgs(@NotNull Project project) {
        return HEADERS.get(project);
    }

    public static void setArgs(@NotNull Project project, @NotNull String args) {
        HEADERS.put(project, args);
    }

    public static void clear() {
        HEADERS.clear();
    }
}
