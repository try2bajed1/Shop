package com.example.shop.rest

import android.support.annotation.CheckResult
import com.example.shop.app.AppSingleton
import com.example.shop.db.DatabaseHelper
import io.reactivex.Completable
import io.reactivex.Observable
import org.reactivestreams.Subscriber
import com.example.shop.utils.setIOScheduler
import java.util.prefs.Preferences

class AccessCheckUseCase(private val preferences: Preferences,
                         private val databaseHelper: DatabaseHelper,
                         private val restRepository: RestRepository) {

    /**
     * Возвращает [Observable], всегда возвращающий в [Subscriber.onNext] true,
     * а в [Subscriber.onError] - [AccessException] или другой неизвестный [Exception]
     * После подписки выполняется [checkAccessGetOthers] и [checkAccessCashier]
     * Служит для проверки оплаты, кассы и кассира
     */
    @CheckResult
    fun checkAccessAtAll(): Completable =
            this.checkAccessPaidAndCashbox()
                    .concatWith(this.checkAccessCashier())
                    .setIOScheduler()

    /**
     * Возвращает [Observable], всегда возвращающий в [Subscriber.onNext] true,
     * а в [Subscriber.onError] - [AccessException] или другой неизвестный [Exception]
     * После подписки выполняется [DataManager.getOthers] и [checkAccessCashier]
     * Служит для проверки оплаты, кассы и кассира с запросом данных аккаунта
     */
    @CheckResult
    fun checkAccessGetOthers(): Completable =
            DataManager(this.restRepository, this.databaseHelper, this.preferences)
                    .getOthers(token, preferences.isApkFromMarket, preferences.latitude, preferences.longitude)
                    .concatWith(this.checkAccessCashier())
                    .setIOScheduler()

    /**
     * Возвращает [Observable], всегда возвращающий в [Subscriber.onNext] true,
     * а в [Subscriber.onError] - [AccessException] или другой неизвестный [Exception]
     * После подписки выполняется [DataManager.syncAll] и [checkAccessCashier]
     * Служит для проверки оплаты, кассы и кассира с синхронизацией данных аккаунта
     */
    @CheckResult
    fun checkAccessSyncAll(): Completable =
            DataManager(this.restRepository, this.databaseHelper, this.preferences)
                    .syncAll(preferences.isApkFromMarket, preferences.latitude, preferences.longitude)
                    .concatWith(this.checkAccessCashier())
                    .setIOScheduler()



    /**
     * Возвращает [Observable], всегда возвращающий в [Subscriber.onNext] true,
     * а в [Subscriber.onError] - [AccessException] или другой неизвестный [Exception]
     * После подписки выполняется [DataManager.getSellingPointInfo]
     * Служит для проверки оплаты и кассы
     */
    @CheckResult
    private fun checkAccessPaidAndCashbox(): Completable =
            DataManager(this.restRepository, this.databaseHelper, this.preferences)
                    .getSellingPointInfo(token, preferences.isApkFromMarket, preferences.latitude, preferences.longitude)
                    .ignoreElements()
                    .setIOScheduler()

    /**
     * Возвращает [Observable], всегда возвращающий в [Subscriber.onNext] true,
     * а в [Subscriber.onError] - [AccessException] или другой неизвестный [Exception]
     * После подписки выполняется [DataManager.getCashier] и проверка, есть ли в полученном списке текущий кассир
     * Служит для проверки кассира
     */
    @CheckResult
    private fun checkAccessCashier(): Completable =
            DataManager(this.restRepository, this.databaseHelper, this.preferences)
                    .getCashier(token)
                    .map { it.items }
                    .onErrorReturnItem(this.databaseHelper.usersDao.queryForAllSave())
                    .map { it -> it.map { it.id } }
                    .map { it ->
                        AppSingleton.INSTANCE.getCurrentUser().id.takeIf { it != 0 }?.let { id -> it.contains(id) }
                                ?: true
                    }
                    .map { if (!it) throw AccessException(CASHIER_DOES_NOT_EXISTS_EXCEPTION) else true }
                    .ignoreElements()
                    .setIOScheduler()
                    .sendAccessErrorIfOccurred()
}