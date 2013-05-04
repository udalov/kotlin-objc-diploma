package test

import objc.OKPrinter
import objc.PrinterProvider

fun main(args: Array<String>) {
    val printer : OKPrinter = PrinterProvider.getPrinter()

    printer.printOK()
}
