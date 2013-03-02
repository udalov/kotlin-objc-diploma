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

package org.jetbrains.jet.objc;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.objc.ObjCDescriptorResolver;

import java.io.File;

import static org.jetbrains.jet.test.util.NamespaceComparator.RECURSIVE;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaceWithFile;

public class ObjCDescriptorResolverTest extends TestCase {
    public static void doTest(@NotNull String filename) {
        String header = "compiler/testData/objc/" + filename;

        assert header.endsWith(".h") : header;
        File expected = new File(header.substring(0, header.length() - ".h".length()) + ".txt");

        ObjCDescriptorResolver resolver = new ObjCDescriptorResolver();
        NamespaceDescriptor descriptor = resolver.resolve(new File(header));
        compareNamespaceWithFile(descriptor, RECURSIVE, expected);
    }

    public void testFoundation() {
        doTest("foundation.h");
    }
}
