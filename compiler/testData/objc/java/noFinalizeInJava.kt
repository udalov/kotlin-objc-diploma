package test

import objc.*

fun main(args: Array<String>) {
    // This test checks that either the "finalize" method doesn't call
    // -[NSObject finalize] or it's not generated at all.
    //
    // If that's not the case, many allocations lead to objc.A instances
    // being garbage-collected, finalize() is invoked, which calls
    // -[NSObject finalize] and somewhere deep in the realms of Objective-C
    // Runtime something breaks, because nobody is allowed to call finalize
    // when GC support is off, which it is in our tests

    for (i in 1..1000000) {
        A.get()
    }

    print("OK")
}
