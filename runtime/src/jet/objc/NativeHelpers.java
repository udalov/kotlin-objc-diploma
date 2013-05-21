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

package jet.objc;

import jet.runtime.objc.Native;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class NativeHelpers {
    private NativeHelpers() {}

    private static final Set<String> LOADED_LIBRARIES = new HashSet<String>();

    public static void loadLibrary(String fileName) {
        if (LOADED_LIBRARIES.add(fileName)) {
            Native.dlopen(fileName);
        }
    }

    public static long getClass(String name) {
        return Native.objc_getClass(name);
    }

    public static void sendMessageVoid(ObjCObject receiver, String messageName, NativeValue... args) {
        Native.objc_msgSend(receiver, messageName, args);
    }

    public static int sendMessageInt(ObjCObject receiver, String messageName, NativeValue... args) {
        PrimitiveValue value = (PrimitiveValue) Native.objc_msgSend(receiver, messageName, args);
        return value.getInt();
    }

    public static long sendMessageLong(ObjCObject receiver, String messageName, NativeValue... args) {
        PrimitiveValue value = (PrimitiveValue) Native.objc_msgSend(receiver, messageName, args);
        return value.getLong();
    }

    public static short sendMessageShort(ObjCObject receiver, String messageName, NativeValue... args) {
        PrimitiveValue value = (PrimitiveValue) Native.objc_msgSend(receiver, messageName, args);
        return value.getShort();
    }

    public static char sendMessageChar(ObjCObject receiver, String messageName, NativeValue... args) {
        PrimitiveValue value = (PrimitiveValue) Native.objc_msgSend(receiver, messageName, args);
        return value.getChar();
    }

    public static boolean sendMessageBoolean(ObjCObject receiver, String messageName, NativeValue... args) {
        PrimitiveValue value = (PrimitiveValue) Native.objc_msgSend(receiver, messageName, args);
        return value.getBoolean();
    }

    public static double sendMessageDouble(ObjCObject receiver, String messageName, NativeValue... args) {
        PrimitiveValue value = (PrimitiveValue) Native.objc_msgSend(receiver, messageName, args);
        return value.getDouble();
    }

    public static float sendMessageFloat(ObjCObject receiver, String messageName, NativeValue... args) {
        PrimitiveValue value = (PrimitiveValue) Native.objc_msgSend(receiver, messageName, args);
        return value.getFloat();
    }

    public static ObjCObject sendMessageObjCObject(ObjCObject receiver, String messageName, NativeValue... args) {
        return (ObjCObject) Native.objc_msgSend(receiver, messageName, args);
    }

    public static ObjCSelector sendMessageObjCSelector(ObjCObject receiver, String messageName, NativeValue... args) {
        return (ObjCSelector) Native.objc_msgSend(receiver, messageName, args);
    }

    public static Pointer<?> sendMessagePointer(ObjCObject receiver, String messageName, NativeValue... args) {
        return (Pointer) Native.objc_msgSend(receiver, messageName, args);
    }
}
