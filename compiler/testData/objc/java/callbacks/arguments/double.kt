package test

import java.lang.Math.abs
import objc.A

fun main(args: Array<String>) {
    A.invokeWithPi {
        if (abs(it - 3.14159265) < 0.00001)
            print("OK")
        else
            print("Fail $it")
    }
}
