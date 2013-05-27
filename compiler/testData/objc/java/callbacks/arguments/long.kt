package test

import objc.A

fun main(args: Array<String>) {
    A.invokeWith123456789123456789 {
        if (it == 123456789123456789)
            print("OK")
        else
            print("Fail $it")
    }
}
