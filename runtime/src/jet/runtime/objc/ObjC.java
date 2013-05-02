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

package jet.runtime.objc;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class ObjC {
    private ObjC() {}

    private static final Set<String> LOADED_LIBRARIES = new HashSet<String>();

    public static void loadLibrary(String fileName) {
        if (LOADED_LIBRARIES.add(fileName)) {
            Native.dlopen(fileName);
        }
    }

    public static void sendMessageToClassObjectVoid(String className, String messageName, Object... args) {
        ID receiver = Native.objc_getClass(className);
        ID selector = Native.sel_registerName(messageName);
        Native.objc_msgSend(receiver, selector, args);
    }
}
