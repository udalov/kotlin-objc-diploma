package test

import objc.A

fun main(args: Array<String>) {
    val str : Pointer<Char> = Pointer.pointerToString("OK")
    A.printString(str)
    str.release()
}
