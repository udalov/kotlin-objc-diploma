package foundation

import objc.NSString

fun NSString(s: String): NSString {
    val pointer = Pointer.pointerToString(s)
    val result = NSString.stringWithCString(pointer) as NSString
    // TODO: pointer.release()
    return result
}

fun NSString.string(): String {
    val pointer = cStringUsingEncoding(1 /* NSASCIIStringEncoding */)
    val result = pointer.getString()
    // TODO: pointer.release()
    return result
}
