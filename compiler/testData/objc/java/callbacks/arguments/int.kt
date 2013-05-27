package test

import objc.A

fun main(args: Array<String>) {
    A.invokeWith42 {
        if (it == 42)
            print("OK")
        else
            print("Fail $it")
    }
}
