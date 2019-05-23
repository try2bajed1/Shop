package com.example.shop.rest

import android.support.annotation.CheckResult
import io.reactivex.Observable
import io.reactivex.Single

class RestRepository {

    companion object {
        val INSTANCE: RestRepository by lazy { RestRepository() }
    }


    private var service = ApiService.getInstance().service

    fun setBaseUrl(baseUrl: String) {
        ApiService.getInstance().baseUrl = baseUrl
        service = ApiService.getInstance().service
    }

    fun getBaseUrl(): String = ApiService.getInstance().baseUrl


    private fun <T> process(func: () -> Observable<T>): Observable<T> {
        return func()
                .mapExceptions()
                .sendAccessErrorIfOccurred()
    }

    private fun <T> processSingle(func: () -> Single<T>): Single<T> {
        return func()
                .mapExceptions()
                .sendAccessErrorIfOccurred()
    }
}