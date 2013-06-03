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

@SuppressWarnings("UnusedDeclaration")
public class Native {
    private Native() {}

    static {
        System.loadLibrary("KotlinNative");
    }

    public static native void dlopen(String path);


    public static native long malloc(long bytes);

    public static native void free(long pointer);

    public static native long getWord(long pointer);

    public static native void setWord(long pointer, long value);


    public static native long objc_getClass(String name);

    public static native Object objc_msgSend(String returnType, ObjCObject receiver, String selectorName, Object... args);
}
