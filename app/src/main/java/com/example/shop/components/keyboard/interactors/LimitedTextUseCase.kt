package com.example.shop.components.keyboard.interactors

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 12.06.18
 * Time: 11:40
 */
class LimitedTextUseCase(
        private val subj: PublishSubject<KBType>,
        val maxSymbols:Int) {

    
    var value =  ""

    fun reset(value: String? = null) {
        this.value = value ?: ""
    }

    fun getObs(): Observable<String> {
       return subj.filter(::predicate)
            .flatMap(::processInputValue)
    }

    
    private fun predicate(it: KBType) =
            value.length < maxSymbols && (it is Num || it is BackSpace)


    private fun processInputValue(kbType: KBType):Observable<String> {
        when (kbType) {
            is Num -> value += kbType.value
            is Coma -> value += (kbType as Num).value
            is BackSpace ->value = value.dropLast(1)
        }
        return Observable.just(value)
    }
}