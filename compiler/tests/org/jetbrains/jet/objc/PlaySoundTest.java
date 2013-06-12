package org.jetbrains.jet.objc;

import java.io.File;

public class PlaySoundTest extends AbstractObjCWithJavaTest {
    public void testPlaySound() {
        String source = "compiler/testData/objc/playSound/playSound.kt";
        String args = "compiler/testData/objc/playSound/playSound.h";
        runTestGetOutput(source, args, new File(FOUNDATION_DYLIB_PATH));
    }
}
