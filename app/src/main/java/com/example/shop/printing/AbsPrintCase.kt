package com.example.shop.printing

import android.util.Log
import io.reactivex.Observable

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 05.06.17
 * Time: 15:40
 */

abstract class AbsPrintCase {

    abstract fun getKkmUrlPrefix(): String

    abstract fun getCaseType(): CaseType

    abstract fun getObs(): Observable<WrapPrintData>

    val printer: Observable<IPrinter>
        get() = GetActivePrinterUseCase().getPrinterObs()

    enum class CaseType {
        SIMLE_PURCHASE,
        OTHERS
    }

    protected fun wrapWithPrintingResult(wrapPrintData: WrapPrintData): WrapPrintData = wrapPrintData.apply {
        if (BuildConfig.DEBUG)
            Log.e(TAG, "Print result ${wrapPrintData.printResult.errorMessageCode.message}")
        type = getCaseType()
        result = when (wrapPrintData.printResult.errorMessageCode) {
            PrintError.OK -> RespEnumType.OK
            PrintError.SHIFT_NEEDS_TO_BE_CLOSED -> RespEnumType.Z_REQUIRED
            else -> RespEnumType.RETRY
        }
        if (wrapPrintData.printResult.errorMessageCode == PrintError.UNKNOWN)
            CrashlyticsParamsSendUtils.sendException(Exception("Unknown printer error"),
                    listOf(wrapPrintData.printResult.errorSourceClassName,
                            wrapPrintData.printResult.errorCode.toString(),
                            wrapPrintData.printResult.subErrorCode.toString(),
                            wrapPrintData.printResult.message,
                            wrapPrintData.printResult.exceptionClassName))
    }

    companion object {
        private const val TAG = "AbsPrintCase"
    }
}