package test

import objc.A

fun main(args: Array<String>) {
    var result = "Fail"

    A.invoke {
        result = "OK"
    }

    print(result)
}
