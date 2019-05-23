package com.example.shop

import io.reactivex.Observable

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 18.06.18
 * Time: 12:46
 */
class SmartposWithFakeError : SmartposPrinter() {

    private var startWithError = true

    override fun printPurchase(printRequest: PrintRequest): Observable<PrintResult> {
        if(startWithError) {
            startWithError = false
            return Observable.just(PrintResult("",errorMessageCode = PrintError.NO_PAPER))
        }
        return exec { core.printPurchase(printRequest) }
    }


}