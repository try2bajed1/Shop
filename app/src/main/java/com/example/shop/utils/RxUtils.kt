package com.example.shop.utils

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Устанавливает для выполнения IO поток для [Observable]
 */
fun <T> Observable<T>.setIOScheduler(): Observable<T> =
        this.compose { it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }

/**
 * Устанавливает для выполнения IO поток для [Completable]
 */
fun Completable.setIOScheduler(): Completable =
        this.compose { it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }

/**
 * Устанавливает для выполнения IO поток для [Single]
 */
fun <T> Single<T>.setIOScheduler(): Single<T> =
        this.compose { it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }

/**
 * Устанавливает для выполнения Main поток для [Observable]
 */
fun <T> Observable<T>.setMainScheduler(): Observable<T> =
        this.compose { it.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread()) }

/**
 * Конвертирует [Action] в [Completable]
 */
fun Action.toCompletable(): Completable =
        Completable.fromAction(this)

/**
 * Добавляет секундную задержку к текущему [Observable]
 */
fun <T> Observable<T>.addSecondDelay(): Observable<T> =
        this.compose { it.delay(1, TimeUnit.SECONDS, Schedulers.trampoline()) }

/**
 * Добавляет логгирование ошибок для [Observable]
 */
fun <T> Observable<T>.addDebugErrorLogger(): Observable<T> =
        this.compose { it.doOnError { if (BuildConfig.DEBUG) it.printStackTrace() } }

/**
 * Добавляет логгирование ошибок для [Completable]
 */
fun Completable.addDebugErrorLogger(): Completable =
        this.compose { it.doOnError { if (BuildConfig.DEBUG) it.printStackTrace() } }

fun <T> Observable<T>.withDoOnFirst(action: Consumer<T>): Observable<T> {
    return take(1).doOnNext(action).concatWith(this)
}