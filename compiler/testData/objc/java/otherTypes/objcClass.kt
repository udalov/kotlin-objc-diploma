package test

import objc.SomeClass

fun main(args: Array<String>) {
    val self1: ObjCClass = SomeClass.getSelf()
    val self2: ObjCClass = SomeClass.getSelf()

    val name = self1.javaClass.getSimpleName()

    if (!self1.identityEquals(self2))
        print("Fail: self1 != self2")
    else if (name != "SomeClass\$object")
        print("Fail name: $name")
    else
        print("OK")
}
