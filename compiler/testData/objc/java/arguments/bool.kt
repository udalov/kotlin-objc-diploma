package test

import objc.A

fun main(args: Array<String>) {
    A.printOIfTrue(true)
    A.printOIfTrue(false)
    A.printKIfFalse(true)
    A.printKIfFalse(false)
}
