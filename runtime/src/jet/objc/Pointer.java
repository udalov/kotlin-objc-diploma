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
public class Pointer<T> implements NativeValue {
    public static final int CHAR_SIZE = 1;

    public final long peer;

    /* package */ Pointer(long peer) {
        this.peer = peer;
    }

    // TODO: KotlinSignature for all methods which return Pointer

    public static Pointer<Character> allocateChar() {
        return allocateChars(1);
    }

    public static Pointer<Character> allocateChars(long size) {
        return new Pointer<Character>(Native.malloc(size * CHAR_SIZE));
    }


    public char getChar() {
        return getChar(0);
    }

    public char getChar(long offset) {
        return (char) (Native.getWord(peer + offset) & 0xff);
    }


    public void setChar(char c) {
        setChar(0, c);
    }

    public void setChar(long offset, char c) {
        Native.setWord(peer + offset, (byte) c);
    }


    @KotlinSignature("fun pointerToChar(c: Char): Pointer<Char>")
    public static Pointer<Character> pointerToChar(char c) {
        Pointer<Character> pointer = allocateChar();
        pointer.setChar(c);
        return pointer;
    }

    // TODO: support different encodings and stuff
    @KotlinSignature("fun pointerToString(s: String): Pointer<Char>")
    public static Pointer<Character> pointerToString(String s) {
        // TODO: not very optimal, use a native function instead
        int n = s.length();
        Pointer<Character> pointer = allocateChars(n + 1);
        for (int i = 0; i < n; i++) {
            pointer.setChar(i, s.charAt(i));
        }
        pointer.setChar(n, (char) 0);
        return pointer;
    }

    @KotlinSignature("fun getString(): String")
    public String getString() {
        // TODO: not very optimal, use a native function instead
        StringBuilder sb = new StringBuilder();
        char c;
        int offset = 0;
        while ((c = getChar(offset)) != 0) {
            sb.append(c);
            offset++;
        }
        return sb.toString();
    }

    public void release() {
        Native.free(peer);
    }


    @Override
    public String toString() {
        return String.format("[Pointer %016x]", peer);
    }

    @Override
    public int hashCode() {
        return (int) (peer ^ (peer >>> 32));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pointer && ((Pointer) o).peer == peer;
    }
}
