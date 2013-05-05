package test

import objc.OKPrinter
import objc.Printer
import objc.PrinterProvider

fun main(args: Array<String>) {
    var printer : Printer

    do {
        printer = PrinterProvider.getPrinter()
    } while (printer !is OKPrinter)

    (printer : Printer).print()   // KT-3572
}
