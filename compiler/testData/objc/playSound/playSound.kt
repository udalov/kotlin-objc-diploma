package test

import foundation.*
import objc.*

fun main(args: Array<String>) {
    val fileName = NSString(
        if (args.isEmpty()) "/System/Library/Sounds/Glass.aiff"
        else args[0]
    )

    val sound = (NSSound.alloc() as NSSound).initWithContentsOfFile(fileName, true) as NSSound
    sound.play()

    NSRunLoop.currentRunLoop().run()

    java.lang.Thread.sleep(1000);

    print("OK")
}
