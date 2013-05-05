package test

import java.lang.Math.abs
import objc.A

fun main(args: Array<String>) {
    val z = A.getCapitalZ()
    val fortyTwo = A.getShort42()

    if (z != 'Z')
        print("Fail char: $z")
    else if (fortyTwo != 42.toShort())
        print("Fail short: $fortyTwo")
    else
        print("OK")
}
