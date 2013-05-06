package test

import objc.A

fun main(args: Array<String>) {
    if (!A.getTrue())
        print("Fail getTrue")
    else if (A.getFalse())
        print("Fail getFalse")
    else
        print("OK")
}
