package test

import objc.A

fun main(args: Array<String>) {
    val z = A.getCapitalZ()

    if (z != 'Z')
        print("Fail $z")
    else
        print("OK")
}
