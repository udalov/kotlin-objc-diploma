package test

import objc.A

fun main(args: Array<String>) {
    A.printOIfTrue { true }
    A.printKIfFalse { false }
}
