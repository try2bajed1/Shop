package com.example.shop

import io.reactivex.Observable

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 18.06.18
 * Time: 12:46
 */
class
SmartposWithFakeZRequired : SmartposPrinter() {

    private var needZ = true

    override fun printPurchase(printRequest: PrintRequest): Observable<PrintResult> {
        if(needZ) {
            needZ = false
            return Observable.just(PrintResult("",errorMessageCode = PrintError.SHIFT_NEEDS_TO_BE_CLOSED))
        }
        return exec { core.printPurchase(printRequest) }
    }


}