package test

import objc.A

fun main(args: Array<String>) {
    val value : Int = A.foo()
    print(if (value == 42) "OK" else "Fail $value")
}
