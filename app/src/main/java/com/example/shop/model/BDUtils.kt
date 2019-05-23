package com.example.shop.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 06.12.18
 * Time: 20:27
 */

fun BigDecimal.exec(kDiscount: KDiscount): BigDecimal {
    val result = when (kDiscount) {
        is KDiscount.Money -> this - kDiscount.value
        is KDiscount.MarkUp -> this + kDiscount.value
        is KDiscount.Percent -> this - (this * kDiscount.value.movePointLeft(2))
        is KDiscount.None -> this
    }
    return if(result < BigDecimal.ZERO) BigDecimal.ZERO else result
}


fun BigDecimal.formatPrice(): String {
    val locale = Locale("en", "UK")
    val pattern = "#,###,###,##0.00"

    val symbols = DecimalFormatSymbols(locale)
    symbols.decimalSeparator = ','
    symbols.groupingSeparator = ' '

    val numberFormat: NumberFormat = DecimalFormat(pattern, symbols)
    return numberFormat.format(this.setScale(15, RoundingMode.HALF_UP))
}

val Double.bd:BigDecimal
    get() = BigDecimal.valueOf(this)

fun BigDecimal.toCopecks(): Long {
    return this.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
}


fun Long.getValueFromCopecks(): BigDecimal {
    return BigDecimal(this).movePointLeft(2)
}


fun Long.toKg(): BigDecimal {
    return BigDecimal(this).movePointLeft(3)
}

fun BigDecimal.toGrams(): Long {
    return this.movePointRight(3).setScale(0,RoundingMode.HALF_UP).toLong()
}


fun BigDecimal.isZero(): Boolean {
    return this.compareTo(BigDecimal.ZERO) == 0
}