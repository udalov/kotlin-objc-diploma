package test

import objc.OKPrinter
import objc.Printer
import objc.PrinterProvider

fun main(args: Array<String>) {
    for (i in 0..10) {
        val printer = PrinterProvider.getPrinter()
        if (printer is OKPrinter) {
            printer.print()
            return
        }
    }

    print("Fail")
}
