package test

import objc.A

fun main(args: Array<String>) {
    val fortyTwo = A.getShort42()

    if (fortyTwo != 42.toShort())
        print("Fail $fortyTwo")
    else
        print("OK")
}
