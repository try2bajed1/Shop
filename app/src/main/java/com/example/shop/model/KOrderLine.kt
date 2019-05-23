package com.example.shop.model

import java.math.BigDecimal
import java.util.prefs.Preferences

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 06.12.18
 * Time: 15:24
 */

class KOrderLine(var variant:Variant, var quantity:BigDecimal) {

    var freePrice:BigDecimal = BigDecimal.ZERO
    var calcPrice:BigDecimal = BigDecimal.ZERO
    var freeTitle:String = ""
    var vat_rate: Int? = null

    init {
        calcPrice = BigDecimal(variant.price)
    }

    var discount: KDiscount = KDiscount.None()
        set(value) {
            calcPrice = if(freePrice!= BigDecimal.ZERO) freePrice else BigDecimal(variant.price) // если стояла наценка сбрасываем до исходной цены

            field = value
        }

    val totalPrice: BigDecimal
        get() {
            return (calcPrice.exec(discount)).multiply(quantity)
        }

    fun getPriceToOTP(): BigDecimal {
        if(freePrice!= BigDecimal.ZERO)
            return calcPrice.exec(discount)
        return BigDecimal(variant.price).exec(discount)
    }

    fun getProductTitle(): String {
        return variant.product?.title ?:"Номенклатура отсутствует"
    }

    fun getQuantityStr(): String { // строка не знает как себя показывать - но это знает вариант
        return variant.calcQuantityStr(quantity.toDouble())
    }
    


    fun hasModifications(): Boolean {
        return variant.product?.hasVariants() ?: false
    }

    fun getVariantTitle(): String {
        return variant.title
    }


    fun decQuantity(quantity: BigDecimal) { // уменьшить количество в строке заказа
        this.quantity -= quantity
    }

    fun incQuantity(quantity: BigDecimal) { // увеличить количество в строке заказа
        this.quantity += quantity
    }


    fun getLongTitle(): String { // в формате Продукт (Вариант)
        return if (variant.title.isNullOrEmpty()) getProductTitle()
        else String.format("%s (%s)", getProductTitle(), variant.title)
    }
}