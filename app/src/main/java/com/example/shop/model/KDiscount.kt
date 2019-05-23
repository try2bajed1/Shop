package com.example.shop.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 07.12.18
 * Time: 13:12
 */
sealed class KDiscount : Operations {

    /**
     * Денежная скидка
     */
    class Money(override var value: BigDecimal) : KDiscount() {
        override fun applyLimit(limitPercents: BigDecimal, sum: BigDecimal): Money {
            val maxValue = sum * (limitPercents.movePointLeft(2))
            if(value > maxValue)
                value = maxValue.setScale(2, RoundingMode.HALF_UP)
            return this
        }

        override fun inPercents(totalPrice: BigDecimal): BigDecimal {
            if(totalPrice.isZero())
                return BigDecimal.ZERO
            return if (value.isZero()) BigDecimal.ZERO //иначе выхватим division by zero :)
            else value.divide(totalPrice, MathContext(6, RoundingMode.HALF_UP)).movePointRight(2)
        }

        override fun asStr() = value.formatPrice()
    }

    /**
     * Процентная скидка
     */
    class Percent(override var value: BigDecimal) : KDiscount() {
        override fun inPercents(totalPrice: BigDecimal): BigDecimal {
            return value
        }

        override fun applyLimit(limitPercents: BigDecimal, sum: BigDecimal): Percent {
            if(value > limitPercents)
                value = limitPercents
            return this
        }

        override fun asStr() = value.formatPrice()
    }

    /**
     * Наценка (скидка наоборот)
     */
    class MarkUp(override var value: BigDecimal) : KDiscount() {
        override fun applyLimit(limitPercents: BigDecimal, sum: BigDecimal) : MarkUp {
            return this
        }

        override fun inPercents(totalPrice: BigDecimal): BigDecimal {
            return BigDecimal.ZERO
        }

        override fun asStr() = value.formatPrice()
    }

    /**
     * дефолтное значение скидки  (защита от нпе)
     */
    class None(override var value: BigDecimal = BigDecimal.ZERO) : KDiscount() {
        override fun applyLimit(limitPercents: BigDecimal, sum: BigDecimal): None {
            return this
        }

        override fun inPercents(totalPrice: BigDecimal): BigDecimal {
            return BigDecimal.ZERO
        }

        override fun asStr() = ""
    }
}


interface Operations{
    var value:BigDecimal
    fun asStr():String

    /**
     * в бекофисе есть настройка максимально возможной скидки,
     * метод уменьшает превышенное значение до максимально возможного
     */
    fun applyLimit(limitPercents: BigDecimal, sum: BigDecimal = BigDecimal.ZERO): KDiscount
    fun inPercents(totalPrice:BigDecimal):BigDecimal
}
