package test

import foundation.*
import objc.*

fun main(args: Array<String>) {
    val a = NSString("abc123")
    val b = NSString("abc123")

    if (!a.isEqual(b))
        print("Fail: a != b")
    else if (a.string() != "abc123")
        print("Fail a.string(): ${a.string()}")
    else if (a.string() != b.string())
        print("Fail a.string() != b.string(): ${a.string()} != ${b.string()}")
    else
        print("OK")
}
