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

@SuppressWarnings("UnusedDeclaration")
public abstract class ObjCObject implements NativeValue {
    public final ID id;

    protected ObjCObject(ID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "[ObjCObject " + getClass().getName() + " " + id.toString() + "]";
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjCObject)) return false;
        ObjCObject obj = (ObjCObject) o;
        return id == null ? obj.id == null : id.equals(obj.id);
    }
}
