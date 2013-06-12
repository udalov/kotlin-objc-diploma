package test

import java.lang.IllegalStateException
import java.lang.System.nanoTime
import objc.*

val TRIES = 100000

fun runIncreaseCounter() {
    val x = (IncreaseCounter.alloc() as IncreaseCounter).init() as IncreaseCounter

    if (x.get() != 0) throw IllegalStateException("Starting x ${x.get()}")

    for (i in 0..TRIES-1) {
        x.increase()
    }

    if (x.get() != TRIES) throw IllegalStateException("Ending x ${x.get()}")
}

fun runSumOfManyPrimitives() {
    for (i in 0..TRIES-1) {
        SumOfManyPrimitives.calculate(1, 2, 3, 4.toFloat(), 5.0)
    }
}

fun runSingleton() {
    Singleton.createInstance()

    for (i in 0..TRIES-1) {
        Singleton.getInstance()
    }
}

fun runMirror() {
    val instance = (Mirror.alloc() as Mirror).init() as Mirror
    for (i in 0..TRIES-1) {
        Mirror.get(instance)
    }
}

fun runInvokeCallback() {
    val callback = {}
    for (i in 0..TRIES-1) {
        InvokeCallback.invoke(callback)
    }
}

fun main(args: Array<String>) {
    val start = nanoTime()
    // runIncreaseCounter()
    // runSumOfManyPrimitives()
    // runSingleton()
    // runMirror()
    runInvokeCallback()
    val end = nanoTime()
    println("$TRIES: " + (end - start)*0.000000001)
}
