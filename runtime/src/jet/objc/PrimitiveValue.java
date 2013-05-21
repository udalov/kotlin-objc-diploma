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
public final class PrimitiveValue implements NativeValue {
    private final long value;

    public PrimitiveValue(long value) {
        this.value = value;
    }

    public int getInt() {
        return (int) value;
    }

    public long getLong() {
        return value;
    }

    public short getShort() {
        return (short) value;
    }

    public char getChar() {
        return (char) (value & 0xff);
    }

    public boolean getBoolean() {
        return (value & 0xff) != 0;
    }

    public double getDouble() {
        return Double.longBitsToDouble(value);
    }

    public float getFloat() {
        return Float.intBitsToFloat((int) value);
    }
}
