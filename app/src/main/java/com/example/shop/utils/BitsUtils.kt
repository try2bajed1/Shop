package com.example.shop.utils

import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 28.11.18
 * Time: 11:51
 */

fun Int.isPowerOfTwo(): Boolean {
    return this > 0 && this and (this - 1) == 0
}


fun Int.bitNumbers() : List<Int> {
    var n = 1
    val res = ArrayList<Int>()
    while (n <= this) {
        if (n and this > 0) {
            res.add(n)
        }
        n = n shl 1
    }
    return res
}
