package test

import java.lang.Math.abs
import objc.A

fun main(args: Array<String>) {
    val pi = A.getPi()

    if (abs(pi - 3.14159265.toFloat()) < 0.00001)
        print("OK")
    else
        print("Fail $pi")
}
