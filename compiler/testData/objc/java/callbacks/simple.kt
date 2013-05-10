package test

import objc.A

fun main(args: Array<String>) {
    A.invoke {
        print("OK")
    }
}
