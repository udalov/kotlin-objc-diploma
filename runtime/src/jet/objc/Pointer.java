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
import jet.runtime.typeinfo.KotlinSignature;

@SuppressWarnings("UnusedDeclaration")
public class Pointer<T> {
    public static final int CHAR_SIZE = 1;

    public final long peer;

    private Pointer(long peer) {
        this.peer = peer;
    }

    // TODO: KotlinSignature for all methods which return Pointer

    public static Pointer<Character> allocateChar() {
        return new Pointer<Character>(Native.malloc(CHAR_SIZE));
    }


    public char getChar() {
        return (char) Native.getWord(peer);
    }


    public void setChar(char c) {
        Native.setWord(peer, (byte) c);
    }


    @KotlinSignature("fun pointerToChar(c: Char): Pointer<Char>")
    public static Pointer<Character> pointerToChar(char c) {
        Pointer<Character> pointer = allocateChar();
        pointer.setChar(c);
        return pointer;
    }

    public void release() {
        Native.free(peer);
    }
}
