package com.example.shop.components.keyboard.interactors

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 12.06.18
 * Time: 11:40
 */
class DecimalUseCase @JvmOverloads constructor(private val subj: PublishSubject<KBType>,
                                               val maxSymbols:Int,
                                               private val signsAfterComa:Int = 2) {
    
    var value =  ""
    var valueInCopecks:Long =0
    var valueInPercents:Double = 0.0

    fun reset() {
        value = ""
    }


    fun setValFromPercent(perc: Double) {
        value = if (perc == 0.0)
            ""
        else
            PercentsUtils.getFormattedPercents(perc)
    }

    fun setValFromCopecks(copecks: Long) {
        value = if (copecks == 0L) {
            ""
        } else {
            CopecksUtils.getFormattedPrice(copecks).replace(" ".toRegex(), "")
        }
    }

    
    private fun getObs(): Observable<String> {
        return subj
                .filter(::checkForMaxLength)
                .filter(::allowConcatComa)
                .filter(::allowConcatAfterComa)
                .filter(::allowConcatZeroSign)
                .flatMap(::processInputValue)
    }

                                                     
    fun getMoneyObs(): Observable<Long> {
        return getObs().map(::toCopecks)
    }


    fun getPercentObs(): Observable<Double> {
        return getObs().map(::toPercents)
    }

    fun getWeightObs(): Observable<String> {
        return getObs()
    }


    private fun toCopecks(enteredValue: String):Long {
        valueInCopecks =  if (comaPosition!=-1) {
            CopecksUtils.getCopecksFromFormatted(value)
        } else {
            if (!value.isEmpty())
                java.lang.Long.parseLong(value) * 100
            else 0
        }
        return valueInCopecks
    }


    private fun toPercents(enteredValue: String): Double {
        valueInPercents = if (comaPosition != -1) {
            PercentsUtils.getPercentsFromFormatted(value)
        } else {
            (if (value.isEmpty()) 0 else java.lang.Long.parseLong(value)).toDouble()
        }
        return valueInPercents
    }


    private fun processInputValue(kbType: KBType):Observable<String> {
        when (kbType) {
            is Num ->  {
                value+=kbType.value
            }
            
            is Coma -> {
                if(value.isEmpty()) value = "0,"
                else
                    value += kbType.value
            }
            is BackSpace -> {
                value = if (value.endsWith(",")) {
                    if (value.length != 1) {
                        // если запятая последний символ, то вместе с ней удаляем и следующий  38, <= 3
                        value.dropLast(2)
                    } else ""
                    
                } else {
                    value.dropLast(1)
                }
            }
        }
        return Observable.just(value)
    }


    private val comaPosition: Int
        get() = value.indexOf(",")


    private fun checkForMaxLength(kbType: KBType):Boolean {
        // игнорируем число или запятую , если достигли макс. длинны строки
        if (kbType is Num || kbType is Coma) {
            return value.length <= maxSymbols
        }
        //backspace пропускаем ниже
        return true
    }


    private fun allowConcatZeroSign(kbType: KBType): Boolean {
        //не даем вводить нули, если ноль уже введен
        if(kbType is Num) {
            return !( kbType.value == "0" && value=="0")
        }
        return true
    }


    private fun allowConcatAfterComa(kbType: KBType): Boolean {
        if(kbType is Num) {
            if(comaPosition==-1)
                return true
            
            val delta = value.length - comaPosition
            return delta <= this.signsAfterComa
        }
        return true
    }


    private fun allowConcatComa(kbType: KBType): Boolean {
        if (kbType is Coma) {
            return value.indexOf(",") == -1
        }
        return true
    }




}