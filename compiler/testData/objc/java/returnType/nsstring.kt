package test

import objc.A

fun main(args: Array<String>) {
    val string = A.getString()
    val length = string.length().toInt()
    if (length != 5) print("Fail $length")
    else print("OK")
}
