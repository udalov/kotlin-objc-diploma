package test

import objc.Printer

fun main(args: Array<String>) {
    Printer.printOKWithLazyInstance { Printer.instance() }
}
