package com.example.shop

import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Response
import retrofit2.adapter.rxjava2.Result

class EOBackendStub : IBackend {

    override fun receiptRequests(): Observable<PollingResult> {
        return Observable.just(genPollingResult())
    }

    override fun postReceiptError(error: ReceiptRequestError): Observable<Result<JsonObject>> {
        return Observable.just(Result.response(Response.success(JsonObject())))
    }

    override fun postKKT(request: ReceiptResultRequest, url: String): Observable<Result<JsonObject>> {
        return Observable.just(Result.response(Response.success(JsonObject())))
    }

    override fun addReceiptError(error: ReceiptRequestError, token: String): Single<Unit> {
        return Single.just(Unit)

    }

    private fun genPollingResult(): PollingResult {
        return PollingResult(listOf(genReceipt()), emptyList(), emptyList(), 2)
    }

    private fun genReceipt(): SaleReceiptDB = createReceiptRequestWithoutNullFields(1, 1, 1L)


}