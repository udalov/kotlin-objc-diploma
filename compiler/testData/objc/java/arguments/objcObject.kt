package test

import objc.PrintingServices

fun main(args: Array<String>) {
    val printer = PrintingServices.getDefaultPrinter()

    PrintingServices.printWithPrinter(printer)
}
