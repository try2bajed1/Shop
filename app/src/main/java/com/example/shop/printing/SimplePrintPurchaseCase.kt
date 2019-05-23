package com.example.shop.printing

import com.example.shop.model.OrdersModel
import com.example.shop.rest.RestRepository
import com.example.shop.db.DatabaseHelper
import io.reactivex.Observable
import org.json.JSONObject
import java.util.prefs.Preferences


class SimplePrintPurchaseCase(val ordersModel: OrdersModel,
                              private val preferences: Preferences,
                              private val databaseHelper: DatabaseHelper,
                              private val restRepository: RestRepository,
                              private val email: String?
) : AbsPrintCase() {


    override fun getObs(): Observable<WrapData> {
        return printer
                .flatMap { it.printPurchase(getPrintRequest(it)) }
                .map { WrapData(ordersModel.kOrder, it, null) }
                .map(::wrapWithPrintingResult)
                .flatMap(::correctPrinting)
                .map(::updateInPair)
                .map(::savePrintedData)
                .flatMap(::post, ::clear)
                .setIOScheduler()
    }

    
    private fun getPrintRequest(printer: IPrinter):Request {
        // secret (:
    }


    private fun correctPrinting(wrapPrintData: WrapData) =
            if (wrapPrintData.result == RespEnumType.OK)
                Observable.just(wrapPrintData)
            else
                Observable.error(PrintException("fail to print", wrapPrintData))


    private fun updateInPair(wrapPrintData: WrapData): WrapData {
        // secret (:
        return wrapPrintData
    }


    private fun savePrintedData(wrapPrintData: WrapData): WrapData {
        // secret (:
        return wrapPrintData
    }


    private fun post(wrapPrintData: WrapData): Observable<JSONObject> {
        // secret (:
    }


    private fun clear(wrapPrintData: WrapData, fiscalCheck: JSONObject): WrapData {
        // secret (:
    }


    override fun getKkmUrlPrefix(): String {
        return "url"
    }

    override fun getCaseType(): AbsPrintCase.CaseType {
        return AbsPrintCase.CaseType.SIMLE_PURCHASE
    }


}
