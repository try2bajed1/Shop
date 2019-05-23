package com.example.shop.model

import android.media.VolumeShaper
import com.example.shop.app.AppSingleton
import com.example.shop.db.CatalogRepo
import java.util.Collections.addAll
import java.util.prefs.Preferences


class OrdersModel {

    var orderToPostHistory: VolumeShaper.Operation? = null
    private var originOTP: VolumeShaper.Operation? = null
    private var refundByCash: Boolean = false
    var kOrder: KOrder

    var selectedProductByLongClick: Product? = null

    companion object {
        @Volatile
        var instance: OrdersModel = OrdersModel()

        private val catalogRepo = CatalogRepo(AppSingleton.INSTANCE.databaseHelper)

        fun getOrderFromComposed(preparedOrder: PreparedOrder) =
                KOrder(KOrder.TYPE_PREPARED).apply {
                    this.preparedOrder = preparedOrder
                    orderLines = ArrayList(preparedOrder.preparedRealizationLines
                            .map { line ->
                                val variant = catalogRepo.getVariantById(line.variantId)
                                variant.price = line.price
                                KOrderLine(variant, BigDecimal.valueOf(line.quantity)).apply {
                                    vat_rate = line.vat_rate
                                }
                            })
                }
    }

    val sortedShipments: List<VolumeShaper.Operation>
        get() {
            val id = kOrder.preparedOrder?.id ?: 0
            return OperationQueries.queryShipments(id).apply {
                forEach { addAll(OperationQueries.queryRefundsFor(it)) }
            }.sortedBy { it.createdAt }
        }

    init {
        kOrder = KOrder(KOrder.TYPE_BY_USER, Preferences.getInstance().roundToIntegerEnabled)
    }

    fun resetCurrentOrder() {
        kOrder = KOrder(KOrder.TYPE_BY_USER, Preferences.getInstance().roundToIntegerEnabled)
    }

    fun resetHistoryData() {
        orderToPostHistory = null
    }

    /**
     * меняем местами делаем отгрузку по заказу покупкой пользователя
     */
    fun transformToUserPurchase() {
        kOrder.type = KOrder.TYPE_BY_USER
    }

    fun selectSinglePrepared(preparedOrder: PreparedOrder?) {
        kOrder = preparedOrder?.let { getOrderFromComposed(it) } ?: KOrder()
    }

    fun setOrigin(selectedOtp: VolumeShaper.Operation) {
        originOTP = if (selectedOtp.hasParentId())
            selectedOtp.parentOrder
        else selectedOtp
    }

    fun selectOperationHistory(orderToPostHistory: VolumeShaper.Operation) {
        this.orderToPostHistory = orderToPostHistory
    }

    fun originWasChanged(): Boolean {
        if (originOTP == null || orderToPostHistory == null)
            return false
        return originOTP!!.amount > orderToPostHistory!!.sumLines()
    }

    fun refundByCash(): Boolean {
        return refundByCash
    }

    fun setRefundByCash(selectedReturnPaymentType: Boolean) {
        this.refundByCash = selectedReturnPaymentType
    }
}