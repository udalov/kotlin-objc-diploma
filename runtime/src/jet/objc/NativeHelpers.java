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

import jet.runtime.objc.ID;
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

    public static ID getClass(String name) {
        return Native.objc_getClass(name);
    }

    public static void sendMessageVoid(ID receiver, String messageName, NativeValue... args) {
        Native.objc_msgSendPrimitive(receiver, messageName, args);
    }

    public static int sendMessageInt(ID receiver, String messageName, NativeValue... args) {
        return (int) Native.objc_msgSendPrimitive(receiver, messageName, args);
    }

    public static long sendMessageLong(ID receiver, String messageName, NativeValue... args) {
        return Native.objc_msgSendPrimitive(receiver, messageName, args);
    }

    public static short sendMessageShort(ID receiver, String messageName, NativeValue... args) {
        return (short) Native.objc_msgSendPrimitive(receiver, messageName, args);
    }

    public static char sendMessageChar(ID receiver, String messageName, NativeValue... args) {
        long l = Native.objc_msgSendPrimitive(receiver, messageName, args);
        // Native char is 8-bit, and the rest of l may contain garbage
        return (char) (l & 0xff);
    }

    public static boolean sendMessageBoolean(ID receiver, String messageName, NativeValue... args) {
        long l = Native.objc_msgSendPrimitive(receiver, messageName, args);
        // Native boolean is 8-bit, and the rest of l may contain garbage
        return (l & 0xff) != 0;
    }

    public static double sendMessageDouble(ID receiver, String messageName, NativeValue... args) {
        long l = Native.objc_msgSendPrimitive(receiver, messageName, args);
        return Double.longBitsToDouble(l);
    }

    public static float sendMessageFloat(ID receiver, String messageName, NativeValue... args) {
        long l = Native.objc_msgSendPrimitive(receiver, messageName, args);
        return Float.intBitsToFloat((int) l);
    }

    public static ObjCObject sendMessageObjCObject(ID receiver, String messageName, NativeValue... args) {
        return Native.objc_msgSendObjCObject(receiver, messageName, args);
    }

    public static Pointer<?> sendMessagePointer(ID receiver, String messageName, NativeValue... args) {
        long l = Native.objc_msgSendPrimitive(receiver, messageName, args);
        return new Pointer<Object>(l);
    }
}
