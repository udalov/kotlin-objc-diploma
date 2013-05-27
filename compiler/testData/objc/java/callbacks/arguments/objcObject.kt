package test

import objc.*

fun main(args: Array<String>) {
    PrintingServices.invokeOnPrinter {
        (p: Printer): Unit ->
        p.printOK()
    }
}
