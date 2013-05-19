package test

import objc.A

fun main(args: Array<String>) {
    val hello = A.getHello().getString()
    if (hello != "Hello")
        print("Fail hello: $hello")
    else
        print(A.getOK().cStringUsingEncoding(1 /* NSASCIIStringEncoding */).getString())
}
