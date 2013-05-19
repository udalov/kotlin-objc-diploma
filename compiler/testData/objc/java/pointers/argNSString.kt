package test

import objc.*

fun main(args: Array<String>) {
    val string = Pointer.pointerToString("OK")
    
    val nsstring = NSString.stringWithCString(string, 1 /* NSASCIIStringEncoding */) as NSString

    A.printString(nsstring)
}
