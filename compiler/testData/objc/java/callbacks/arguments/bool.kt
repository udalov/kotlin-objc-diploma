package test

import objc.A

fun main(args: Array<String>) {
    A.invokeWithTrueAndFalse {
        (t: Boolean, f: Boolean): Unit ->
        if (t && !f)
            print("OK")
        else
            print("Fail $t $f")
    }
}
