// Auto-generated by org.jetbrains.jet.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
import java.util.ArrayList
import java.lang as j

fun box(): String {
    val list1 = ArrayList<Int>()
    for (i in (3 downTo 5).reversed()) {
        list1.add(i)
    }
    if (list1 != listOf<Int>()) {
        return "Wrong elements for (3 downTo 5).reversed(): $list1"
    }

    val list2 = ArrayList<Byte>()
    for (i in (3.toByte() downTo 5.toByte()).reversed()) {
        list2.add(i)
    }
    if (list2 != listOf<Byte>()) {
        return "Wrong elements for (3.toByte() downTo 5.toByte()).reversed(): $list2"
    }

    val list3 = ArrayList<Short>()
    for (i in (3.toShort() downTo 5.toShort()).reversed()) {
        list3.add(i)
    }
    if (list3 != listOf<Short>()) {
        return "Wrong elements for (3.toShort() downTo 5.toShort()).reversed(): $list3"
    }

    val list4 = ArrayList<Long>()
    for (i in (3.toLong() downTo 5.toLong()).reversed()) {
        list4.add(i)
    }
    if (list4 != listOf<Long>()) {
        return "Wrong elements for (3.toLong() downTo 5.toLong()).reversed(): $list4"
    }

    val list5 = ArrayList<Char>()
    for (i in ('a' downTo 'c').reversed()) {
        list5.add(i)
    }
    if (list5 != listOf<Char>()) {
        return "Wrong elements for ('a' downTo 'c').reversed(): $list5"
    }

    val list6 = ArrayList<Double>()
    for (i in (3.0 downTo 5.0).reversed()) {
        list6.add(i)
    }
    if (list6 != listOf<Double>()) {
        return "Wrong elements for (3.0 downTo 5.0).reversed(): $list6"
    }

    val list7 = ArrayList<Float>()
    for (i in (3.0.toFloat() downTo 5.0.toFloat()).reversed()) {
        list7.add(i)
    }
    if (list7 != listOf<Float>()) {
        return "Wrong elements for (3.0.toFloat() downTo 5.0.toFloat()).reversed(): $list7"
    }

    return "OK"
}
