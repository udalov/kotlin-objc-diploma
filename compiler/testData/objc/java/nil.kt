package test

import objc.*

fun test(): String {
    val nil = A.getNilObject()
    if (nil != ObjCObject.NIL)
        return "Fail nil: $nil"

    val a = A.getNilA()
    if (a != ObjCObject.NIL)
        return "Fail a: $a"

    val Nil = A.getNilClass()
    if (Nil != ObjCClass.NIL)
        return "Fail Nil: $Nil"

    val nullPtr = A.getNullPointer()
    if (nullPtr != Pointer.NULL)
        return "Fail null: $nullPtr"

    val s = A.getNullS()
    if (s != Pointer.NULL)
        return "Fail s: $s"


    if (!A.isNilObject(ObjCObject.NIL))
        return "Fail isNilObject"

    /* TODO
    if (!A.isNilA(A(null)))
        return "Fail isNilA"
    */

    if (!A.isNilClass(ObjCClass.NIL))
        return "Fail isNilClass"

    if (!A.isNullPointer(Pointer.NULL))
        return "Fail isNullPointer"

    if (!A.isNullS(Pointer.NULL))
        return "Fail isNullS"

    return "OK"
}

fun main(args: Array<String>) {
    print(test())
}
