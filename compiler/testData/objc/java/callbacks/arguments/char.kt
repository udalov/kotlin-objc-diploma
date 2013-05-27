package test

import objc.A

fun main(args: Array<String>) {
    A.invokeWithABC {
        (a: Char, b: Char, c: Char): Unit ->
        if ("$a$b$c" == "ABC")
            print("OK")
        else
            print("Fail $a$b$c")
    }
}
