package test

import objc.A

fun main(args: Array<String>) {
    val foo : Long = A.foo()
    val bar : Long = A.bar()

    val expected = "123456789123456789 987654321987654321";
    val actual = "$foo $bar"

    print(if (actual == expected) "OK" else "Fail $actual")
}
