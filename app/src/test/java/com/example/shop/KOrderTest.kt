package com.example.shop

import android.support.test.filters.SmallTest
import com.example.shop.model.*
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 10.12.18
 * Time: 11:34
 */
@RunWith(JUnit4::class)
@SmallTest
class KOrderTest {

    private lateinit var order: KOrder

    @Before
    fun createBasketSet() {
        order = KOrder()
    }



    @Test
    fun testFormat() {
        assertTrue((BigDecimal(100)).formatPrice() == "100,00")
        assertTrue((BigDecimal(1)).formatPrice() == "1,00")
        assertTrue((BigDecimal(0.01)).formatPrice() == "0,01")
        assertTrue((BigDecimal(1.01)).formatPrice() == "1,01")
        assertTrue((BigDecimal(1000)).formatPrice() == "1 000,00")
        assertTrue((BigDecimal(1000000)).formatPrice() == "1 000 000,00")
        assertTrue((BigDecimal(0.9999999)).formatPrice() == "1,00")
        assertTrue((BigDecimal(0.995)).formatPrice() == "1,00")
        assertTrue((BigDecimal(0.9911111111111)).formatPrice() == "0,99")
        assertTrue((BigDecimal(0.9949)).formatPrice() == "0,99")
    }


    @Test
    fun testForLineAdding() {
        val id = 1
        val price = 1000.0
        val quantity = 5


        assertTrue(order.orderLines.size == 0)

        order.add(getVariant(id, price), BigDecimal(quantity))

        assertTrue(order.orderLines.size == 1)
        assertTrue(order.orderLines[0].variant.id == id)
        assertTrue(BigDecimal(order.orderLines[0].variant.price) == BigDecimal(price))
        assertTrue(order.orderLines[0].quantity == BigDecimal(quantity))
    }


    
    @Test
    fun testForAddingVariantTwoTimes(){
        val id = 1
        val price = 1000.0
        val firstQuantityInOrder = 5
        val secondQuantityInOrder = 6
        val startCorrectSize = 0
        val endCorrectSize = 1
        val endCorrectPrice = price * (firstQuantityInOrder + secondQuantityInOrder)

        assertTrue(order.orderLines.size == startCorrectSize)

        order.add(getVariant(id, price), BigDecimal(firstQuantityInOrder))
        order.add(getVariant(id, price), BigDecimal(secondQuantityInOrder))

        assertTrue(order.orderLines.size == endCorrectSize)
        assertTrue(order.orderLines[0].totalPrice == BigDecimal(endCorrectPrice))
    }


    @Test
    fun testFilter() {
        assertTrue(listOf(1).filter { it !=1 }.equals(emptyList<Int>()))
    }

    @Test
    fun testForLineRemoving() {
        val id = 1
        val price = 1000.0
        val quantityInOrder = 5

        assertTrue(order.orderLines.size == 0)
        order.add(getVariant(id, price), BigDecimal(quantityInOrder))

        assertTrue(order.orderLines.size == 1)
        assertTrue(order.orderLines[0].variant.id == id)

        order.remove(id)

        assertTrue(order.orderLines.size==0)
    }


    @Test
    fun testForLineReplacing() {
        val id = 1
        val price = 1000.0
        val quantityInOrder = 5
        val id2 = 2
        val price2 = 2000.0

        val startCorrectSize = 0
        val afterAddingCorrectSize = 1
        val endCorrectSize = 1
        val firstLineCorrectPrice = price * quantityInOrder
        val endCorrectPrice = price2 * quantityInOrder

        assertTrue(order.orderLines.size == startCorrectSize)

        order.add(getVariant(id, price), BigDecimal(quantityInOrder))

        assertTrue(order.orderLines.size == afterAddingCorrectSize)
        assertTrue(order.orderLines[0].variant.id == id)
        assertTrue(order.orderLines[0].totalPrice == BigDecimal(firstLineCorrectPrice))

        order.replace(id, getVariant(id2, price2))

        assertTrue(order.orderLines.size == endCorrectSize)
        assertTrue(order.orderLines[0].variant.id == id2)
        assertTrue(order.orderLines[0].totalPrice == BigDecimal(endCorrectPrice))
    }

    @Test
    fun testForLineRewrite() {
        val id = 1
        val price = 1000.0
        val quantityInOrder = 5
        val id2 = 2
        val price2 = 2000.0
        val quantityInOrder2 = 2

        val startCorrectSize = 0
        val afterAddingCorrectSize = 2
        val endCorrectSize = 1
        val firstLineCorrectPrice = price * quantityInOrder
        val secondLineCorrectPrice = price2 * quantityInOrder2

        assertTrue(order.orderLines.size == startCorrectSize)

        order.add(getVariant(id, price), BigDecimal(quantityInOrder))
        order.add(getVariant(id2, price2), BigDecimal(quantityInOrder2))

        assertTrue(order.orderLines.size == afterAddingCorrectSize)
        assertTrue(order.orderLines[0].variant.id == id)
        assertTrue(order.orderLines[0].totalPrice == BigDecimal(firstLineCorrectPrice))
        assertTrue(order.orderLines[1].variant.id == id2)
        assertTrue(order.orderLines[1].totalPrice == BigDecimal(secondLineCorrectPrice))

        order.rewrite(id, getVariant(id2, price2))

        assertTrue(order.orderLines.size == endCorrectSize)
        assertTrue(order.orderLines[0].variant.id == id2)

        val endCorrectPrice = price2 * (quantityInOrder +quantityInOrder2)
        val endCorrectQuantity = quantityInOrder + quantityInOrder2

        assertTrue(order.orderLines[0].totalPrice==BigDecimal(endCorrectPrice))
        assertTrue(order.orderLines[0].quantity == BigDecimal(endCorrectQuantity))
    }



    private fun getVariant(variantId: Int, variantPrice: Double) = Variant().apply {
        id = variantId
        price = variantPrice
        product = Product().apply { unit = UnitType.UNIT_pce }
    }

    private fun getWeightVariant(variantId: Int, variantPrice: Double) = Variant().apply {
        id = variantId
        price = variantPrice
        product = Product().apply { unit = UnitType.UNIT_kgm }
    }


    @Test
    fun testCopecks() {
        val bd0 = BigDecimal(100)
        assertTrue(bd0.toCopecks() == 10000L)

        val bd1 = BigDecimal(1)
        assertTrue(bd1.toCopecks() == 100L)

        val bd2 = BigDecimal("1.1")
        assertTrue(bd2.toCopecks() == 110L)

        val bd3 = BigDecimal("1.11")
        assertTrue(bd3.toCopecks() == 111L)

        val bd4 = BigDecimal("1.1144")
        assertTrue(bd4.toCopecks() == 111L)

        val bd5 = BigDecimal("1.1151")
        assertTrue(bd5.toCopecks() == 112L)

        val c = 100L
        assertTrue(c.getValueFromCopecks().compareTo(BigDecimal(1))==0)

        val c1 = 0L
        assertTrue(c1.getValueFromCopecks().compareTo(BigDecimal.ZERO)==0)

        val c2 = 138L
        assertTrue(c2.getValueFromCopecks().compareTo(BigDecimal("1.38"))==0)

        val c3 = 456786138L
        assertTrue(c3.getValueFromCopecks().compareTo(BigDecimal("4567861.38"))==0)
    }


    @Test
    fun testDiscount() {
        val kDiscount = KDiscount.Money(BigDecimal(10))
        assertTrue(BigDecimal(100).exec(kDiscount).compareTo(BigDecimal(90))==0)

        val kDiscount2 = KDiscount.Percent(BigDecimal(10))
        assertTrue(BigDecimal(200).exec(kDiscount2).compareTo(BigDecimal(180))==0)

        assertTrue(BigDecimal(200).exec(KDiscount.None()).compareTo(BigDecimal(200))==0)

        assertTrue(BigDecimal(200).exec(KDiscount.Money(BigDecimal(100500))).compareTo(BigDecimal.ZERO)==0)

        assertTrue(BigDecimal(200).exec(KDiscount.Percent(BigDecimal(200))).compareTo(BigDecimal.ZERO)==0)
    }


    @Test
    fun testDiscountRender() {
        order = KOrder()

        assertTrue(order.discount.inPercents(order.getTotalPriceWithoutDiscount()).formatPrice()=="0,00")

        order.discount = KDiscount.Money(BigDecimal.ZERO)
        assertTrue(order.discount.inPercents(order.getTotalPriceWithoutDiscount()).formatPrice()=="0,00")

        order.add(getVariant(0, 1000.0), BigDecimal(1))
        order.discount = KDiscount.Percent(BigDecimal(50))
        assertTrue(order.discount.inPercents(order.getTotalPriceWithoutDiscount()).formatPrice()=="50,00")

        order.discount = KDiscount.Money(BigDecimal(10))
        assertTrue(order.discount.inPercents(order.getTotalPriceWithoutDiscount()).formatPrice()=="1,00")

        order.discount = KDiscount.Money(BigDecimal(14.88))
        val inPercents = order.discount.inPercents(order.getTotalPriceWithoutDiscount())
        assertTrue(inPercents.formatPrice()=="1,49")
    }


    @Test
    fun testOTP() {
        val id = 1
        val price = 1000.0
        val quantityInOrder = 6

        val id2 = 2
        val price2 = 2000.0
        val quantityInOrder2 = 2

        order.add(getVariant(id, price), BigDecimal(quantityInOrder))
        order.add(getVariant(id2, price2), BigDecimal(quantityInOrder2))

        order.discount = KDiscount.Money(BigDecimal("2019.19"))

        val sum = order.orderLines.asSequence().map { it ->
            order.getOtpPrice(it).multiply(it.quantity).also { println(it.toString()) }
        }.fold(BigDecimal.ZERO,BigDecimal::add)

        val other = order.getTotalPrice()
        assertTrue(sum.compareTo(other)==0)
    }

    
    @Test
    fun testTotalPriceRestToRemove() {
        order = KOrder(KOrder.TYPE_BY_USER,true)
        order.add(getVariant(1, 2.11), BigDecimal(2))
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(4.22))
        order.paidByCash = true
        assertTrue(order.getTotalPrice().setScale(0)==BigDecimal.valueOf(4))
        order.add(getVariant(2, 12.11), BigDecimal(2))
        assertTrue(order.getTotalPrice().setScale(0)==BigDecimal.valueOf(28))
        order.paidByCash = false
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(28.44))
    }


    @Test
    fun testLines() {
        order = KOrder(KOrder.TYPE_BY_USER,true)
        order.add(getVariant(1, 2.11), BigDecimal(2))
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(4.22))
        order.add(getVariant(2, 12.11), BigDecimal(2))
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(28.44))

        order.getLineByIndex(0)?.discount =  KDiscount.Money(BigDecimal(5))
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(24.22))
        order.getLineByIndex(1)?.discount =  KDiscount.Money(BigDecimal(50))
        assertTrue(order.getTotalPrice().compareTo(BigDecimal.ZERO)==0)
    }


    @Test
    fun testWeight() {
        order = KOrder(KOrder.TYPE_BY_USER,true)
        order.add(getWeightVariant(1, 600.0), BigDecimal.valueOf(2.8))
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(1680).setScale(2,RoundingMode.HALF_UP))

        order = KOrder(KOrder.TYPE_BY_USER,true)
        order.add(getWeightVariant(1, 600.0), BigDecimal.valueOf(2.799))
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(1679.4).setScale(2,RoundingMode.HALF_UP))
        order.paidByCash = true
        assertTrue(order.getTotalPrice()==BigDecimal.valueOf(1679).setScale(2,RoundingMode.HALF_UP))
    }



}