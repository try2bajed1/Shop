package com.example.shop.model

import com.example.shop.app.AppSingleton
import com.example.shop.model.KDiscount.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 06.12.18
 * Time: 15:21
 */
data class KOrder(var type: Int = TYPE_BY_USER, val allowToRemoveCopecks: Boolean = false) {

    companion object {
        @JvmField
        val TYPE_BY_USER = 0
        @JvmField
        val TYPE_PREPARED = 1
        @JvmField
        val NOT_FOUND = -1
    }

    var orderLines: ArrayList<KOrderLine> = ArrayList()
    var paidByCash: Boolean = false
    var contractor: Contractor? = null
    var numberForDelayed: Int = 0
    var openedTimestamp: Long = 0
    var uuid: String = UUID.randomUUID().toString()

    var terminalVendor: String? = null
    var terminalSerial: String? = null
    var authCode: String? = null
    var txId: String? = null
    var linkNumber: String? = null
    var veerouteCashierId = ""
    var veerouteWebCashboxId = ""

    var change: BigDecimal = BigDecimal.ZERO
    var enteredSum: BigDecimal = BigDecimal.ZERO
    var shipmentsApplied: Boolean = false

    /**
     * остаток в копейках, который отбрасывается от общей стоимости корзины
    т.к у многих клиентов стоит настройка отбрасывать копейки до рубля при наличной оплате
     */
    private fun restToRemove() = if (allowToRemoveCopecks && paidByCash)
        getTotalFromOrigin().remainder(BigDecimal.ONE)
    else BigDecimal.ZERO

    var discount: KDiscount = None()

    init {
        uuid = UUID.randomUUID().toString()
        orderLines = ArrayList()
        openedTimestamp = System.currentTimeMillis()
        numberForDelayed = Random().nextInt(100000)
    }

    private fun getTotalFromOrigin(): BigDecimal =
            getTotalPriceWithoutDiscount().exec(discount)

    fun getTotalPrice(): BigDecimal =
            (getTotalFromOrigin() - restToRemove()).setScale(2, RoundingMode.HALF_UP)

    /**
     * Сумма корзины это сумма лайнов, Капитан Очевидность не дремлет!
     */
    fun getTotalPriceWithoutDiscount(): BigDecimal =
            orderLines.asSequence().map { it.totalPrice }.fold(BigDecimal.ZERO, BigDecimal::add)

    fun getOrderToPost(): OrderToPost = OrderToPost().apply {
        preparedOrder?.let {
            type = OrderToPost.TYPE_Shipment
            parentId = it.id
        } ?: run {
            type = OrderToPost.TYPE_Realization
        }

        uuid = this@KOrder.uuid
        userId = AppSingleton.INSTANCE.getCurrentUser().id
        activatedAt = Utils.getCurrDate()
        cash = paidByCash
        paymentTransactionId = txId
        amount = getTotalPrice().toDouble()
        amountStr = getTotalPrice().toPlainString()
        terminal_serial = this@KOrder.terminalSerial
        terminal_vendor = this@KOrder.terminalVendor
        auth_code = this@KOrder.authCode
        link_number = this@KOrder.linkNumber
        contractor = this@KOrder.contractor
        veerouteCashierid = this@KOrder.veerouteCashierId
        veerouteWebCashboxId = this@KOrder.veerouteWebCashboxId
        realizationLines = getOtpLines()
    }

    private fun getOtpLines(): List<RealizationLine> {
        val currIndex = orderLines.indexOfFirst { it.totalPrice > restToRemove() }
        return orderLines.mapIndexed { index, it ->
            if (index == currIndex) {
                RealizationLine().apply {
                    setVariantId(it.variant.id)
                    position = index
                    quantity = it.quantity.toDouble()
                    basePrice = it.variant.price
                    price = (getOtpPrice(it) - restToRemove().divide(it.quantity, 8, RoundingMode.HALF_UP)).toDouble()
                    totalPrice = (BigDecimal.valueOf(quantity) * BigDecimal.valueOf(price)).toDouble()
                    priceBigDecStr = getOtpPrice(it).toString()
                    title = it.getLongTitle()
                    setVat_rate(it.variant.vat_rate?:-1)
                }
            } else {
                RealizationLine().apply {
                    setVariantId(it.variant.id)
                    position = index
                    quantity = it.quantity.toDouble()
                    basePrice = it.variant.price
                    price = getOtpPrice(it).toDouble()
                    totalPrice = (BigDecimal.valueOf(quantity) * BigDecimal.valueOf(price)).toDouble()
                    priceBigDecStr = getOtpPrice(it).toString()
                    title = it.getLongTitle()
                    setVat_rate(it.variant.vat_rate?:-1)
                }
            }
        }
    }

    //тут по другому считаются лайны(с учетом скидки если была) и итоговый прайс надо без отбрасывания копеек
    fun getOrderToPostForRefund(): OrderToPost = OrderToPost().apply {
        preparedOrder?.let {
            type = OrderToPost.TYPE_Shipment
            parentId = it.id
        } ?: run {
            type = OrderToPost.TYPE_Realization
        }

        uuid = this@KOrder.uuid
        userId = AppSingleton.INSTANCE.getCurrentUser().id
        activatedAt = Utils.getCurrDate()
        cash = paidByCash
        paymentTransactionId = txId
        amount = getTotalFromOrigin().toDouble()
        amountStr = getTotalFromOrigin().toPlainString()
        terminal_serial = this@KOrder.terminalSerial
        terminal_vendor = this@KOrder.terminalVendor
        auth_code = this@KOrder.authCode
        link_number = this@KOrder.linkNumber
        contractor = this@KOrder.contractor
        veerouteCashierid = this@KOrder.veerouteCashierId
        veerouteWebCashboxId = this@KOrder.veerouteWebCashboxId
        realizationLines = getOtpLinesRefund()
    }

    private fun getOtpLinesRefund(): List<RealizationLine> = orderLines.mapIndexed { index, it ->
        RealizationLine().apply {
            setVariantId(it.variant.id)
            position = index
            quantity = it.quantity.toDouble()
            basePrice = it.variant.price
            price = it.calcPrice.toDouble()
            totalPrice = quantity * price
            priceBigDecStr = it.calcPrice.toString()
            title = it.getVariantTitle()
            setVat_rate(it.variant.vat_rate)
        }
    }

    fun getOtpPrice(line: KOrderLine): BigDecimal = when (discount) {
        is Percent -> line.getPriceToOTP().exec(discount)
        is Money -> {
            val inPercent = discount.value.divide(getTotalPriceWithoutDiscount(), MathContext(8, RoundingMode.HALF_UP)).multiply(BigDecimal(100))
            val priceToOTP = line.getPriceToOTP()
            val exec = priceToOTP.exec(Percent(inPercent))
            exec
        }
        is None -> line.getPriceToOTP()
        is MarkUp -> line.calcPrice
    }

    fun getLinesForPrinting(): List<PrePrintLine> {
        val rest = restToRemove()
        val currIndex = orderLines.indexOfFirst { it.totalPrice > rest }
        val lines = orderLines.mapIndexed { index, it ->
            if (index == currIndex) {
                PrePrintLine(it.variant,
                        it.variant.price,
                        (getOtpPrice(it) - rest.divide(it.quantity, 8, RoundingMode.HALF_UP)).toDouble(),
                        (it.totalPrice - rest).toDouble(),
                        it.quantity.toDouble(),
                        it.getLongTitle(),
                        it.variant.vat_rate)
            } else {
                PrePrintLine(it.variant,
                        it.variant.price,
                        getOtpPrice(it).toDouble(),
                        it.totalPrice.toDouble(),
                        it.quantity.toDouble(),
                        it.getLongTitle(),
                        it.variant.vat_rate)
            }
        }

        return lines
    }


    fun isEmpty() = orderLines.isEmpty()

    fun size() = orderLines.size

    fun getLineByIndex(pos: Int): KOrderLine? = orderLines.getOrNull(pos)

    fun getVariantIndex(variantId: Int?) = orderLines.indexOfFirst { it.variant.id == variantId }

    fun setQuantity(variantId: Int, quantity: BigDecimal) {
        orderLines.getOrNull(getVariantIndex(variantId))?.let {
            it.quantity = quantity
        }
    }

    fun setQuantityByPos(pos: Int, quantity: Double) {
        orderLines.getOrNull(pos)?.apply { this.quantity = BigDecimal(quantity) }
    }

    fun add(variant: Variant, quantity: BigDecimal) {
        val index = getVariantIndex(variant.id!!)
        val alreadyHasLine = index != NOT_FOUND
        if (alreadyHasLine) {
            orderLines[index].quantity = quantity + orderLines[index].quantity
        } else {
            orderLines.add(KOrderLine(variant, quantity))
        }
    }


    fun replace(variantId: Int, variant: Variant) {
        getVariantIndex(variantId).takeIf { it != NOT_FOUND }?.let {
            orderLines[it].variant = variant
            orderLines[it].calcPrice = BigDecimal(variant.price)
        }
    }

    fun getVariantQuantity(variantId: Int): BigDecimal =
            orderLines.getOrNull(getVariantIndex(variantId))?.quantity ?: BigDecimal.ZERO

    fun rewrite(oldId: Int, variant: Variant) {
        val newVariantIndex = getVariantIndex(variant.id)
        if (newVariantIndex >= 0) {
            // когда меняем модификацию на новую, а та новая тоже есть в корзине
            // старый пункт c oldId стираем, а новому добавляем количество
            orderLines[newVariantIndex].quantity = orderLines[newVariantIndex].quantity + getVariantQuantity(oldId)
            remove(oldId)
        } else
        // старый пункт заменяем на новый
            replace(oldId, variant)
    }

    fun remove(variantId: Int) {
        orderLines = ArrayList(orderLines.filter { it.variant.id != variantId })
    }

    fun removeByPos(pos: Int) {
        orderLines.removeAtSafe(pos)
    }

    fun addFreeSale(line: KOrderLine) {
        orderLines.add(line)
    }

    fun getTotalCountInBasket(): Long {
        return orderLines.asSequence().map { if (it.variant.isPce) it.quantity.setScale(0, RoundingMode.HALF_UP).longValueExact() else 1 }.sum()
    }


    fun applyShipmentsAndReturns() { // пересчитать заказ с учетом уже совершенных отгрузок и возвратов
        this.shipmentsApplied = true
        for (shipment in Queries.queryShipments(this.preparedOrder?.id ?: -1)) {
            for (line in shipment.realizationLines) {
                val pos = getVariantIndex(line.variantId) // если какая-то строка заказа полностью или частично отгружена
                if (pos >= 0)
                    orderLines[pos].decQuantity(BigDecimal(line.quantity)) // то уменьшить число товаров в ордере на уже отгруженное
            }
            for (refund in Queries.queryRefundsFor(shipment))
            // возвраты по отгрузке
                for (line in refund.realizationLines) {
                    val pos = getVariantIndex(line.variantId) // если какая-то строка заказа полностью или частично отгружена, а потом возвращена
                    if (pos >= 0)
                        orderLines[pos].incQuantity(BigDecimal(line.quantity)) // то увеличить число товаров в ордере на отгруженное, а потом возвращенное
                }
        }

        // теперь в заказе моут получиться строки с нулевым и даже отрицательным количеством
        // их надо убить из списка. оставшиеся строки будут строками заказа для отгрузки
        orderLines = ArrayList(orderLines.filter { it.quantity > BigDecimal.ZERO })
    }


    fun getSumForPrepared(): BigDecimal { // сумма, которую предлагают заплатить клиенту - это цена товаров в корзине минус предоплата
        var ret = getTotalPrice()
        if (isPreparedOrder())
            ret -= BigDecimal(preparedOrder!!.alreadyPaidAmount) - BigDecimal(preparedOrder!!.alreadyShippedAmount)
        if (ret < BigDecimal.ZERO) ret = BigDecimal.ZERO
        return ret
    }

    fun isPaidWithPrepaid(): Boolean = this.preparedOrder != null
            && BigDecimal(preparedOrder!!.alreadyPaidAmount) -
            BigDecimal(preparedOrder!!.alreadyShippedAmount) +
            getTotalPrice() == BigDecimal.ZERO

    fun hasAnyTransactions(): Boolean = this.preparedOrder != null &&
            (BigDecimal(preparedOrder!!.alreadyShippedAmount) != BigDecimal.ZERO ||
                    BigDecimal(preparedOrder!!.alreadyPaidAmount) != BigDecimal.ZERO)

    fun isPaid(): Boolean = this.enteredSum == this.getTotalPrice()
}