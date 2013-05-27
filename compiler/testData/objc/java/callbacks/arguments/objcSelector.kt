package test

import objc.*

fun main(args: Array<String>) {
    A.invoke {
        (sel: ObjCSelector): Unit -> A.checkIfEqualsToInvoke(sel)
    }
}
