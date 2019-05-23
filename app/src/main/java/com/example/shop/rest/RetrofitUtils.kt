package com.example.shop.rest

const val WRONG_DATA_ERROR_CODE = 401
const val BAD_TOKEN_ERROR_CODE = 403
const val NOT_UNIC_DATA_ERROR_CODE = 422
//Получаем в случае, если подписка истекла
const val BAD_TOKEN_NOT_PAID_ERROR_TEXT = "403 Not paid"
//Получаем при отключении кассы, входе в кассу с другого устройства или в другом случае
const val BAD_TOKEN_OTHER_ERROR_TEXT = "403 Forbidden"

//Подписка истекла
const val BAD_TOKEN_NOT_PAID_EXCEPTION = "Bad Token. Not Paid Exception"
//Касса отключена или используется на другом устройстве
const val BAD_TOKEN_CASHBOX_IS_OFF_OR_ANOTHER_DEVICE_IS_USED_EXCEPTION = "Bad Token. Cashbox Is Off Or Another Device Is Used Exception"
//Кассир удалён
const val CASHIER_DOES_NOT_EXISTS_EXCEPTION = "Cashier Does Not Exists"
//Ошибка доступа при закрыти смены
const val ACCESS_ERROR_DURING_SHIFT_CLOSING_EXCEPTION = "Access error during shift closing"

/**
 * Возвращает true, если тип ошибки - HTTP
 */
fun RetrofitException.isKindHttp() = this.kind == RetrofitException.Kind.HTTP

/**
 * Возвращает true, если тип ошибки - NETWORK
 */
fun RetrofitException.isKindNetwork() = this.kind == RetrofitException.Kind.NETWORK

/**
 * Возвращает true, если код ошибки - [BAD_TOKEN_ERROR_CODE]
 */
fun RetrofitException.is403(): Boolean = this.isKindHttp() && this.response.code() == BAD_TOKEN_ERROR_CODE

/**
 * Возвращает true, если код ошибки - [WRONG_DATA_ERROR_CODE]
 */
fun RetrofitException.is401(): Boolean = this.isKindHttp() && this.response.code() == WRONG_DATA_ERROR_CODE

/**
 * Возвращает true, если код ошибки - [WRONG_DATA_ERROR_CODE]
 */
fun RetrofitException.is422(): Boolean = this.isKindHttp() && this.response.code() == NOT_UNIC_DATA_ERROR_CODE

/**
 * Возвращает true, если [is403] вернул true и ошибка - [errorText]
 */
fun RetrofitException.is403WithErrorText(errorText: String): Boolean =
        this.is403() && (Gson().fromJson(this.response.errorBody()?.charStream(), RestError::class.java)?.error == errorText || this.message == errorText)

/**
 * Возвращает true, если ошибка - [BAD_TOKEN_NOT_PAID_ERROR_TEXT]
 */
fun RetrofitException.is403NotPaid(): Boolean = this.is403WithErrorText(BAD_TOKEN_NOT_PAID_ERROR_TEXT)

/**
 * Возвращает true, если ошибка = [BAD_TOKEN_OTHER_ERROR_TEXT]
 */
fun RetrofitException.is403Other(): Boolean = this.is403WithErrorText(BAD_TOKEN_OTHER_ERROR_TEXT)

/**
 * Возвращает текст ошибки, есои он есть
 */
fun RetrofitException.getErrorText() = Gson().fromJson(this.response.errorBody()?.charStream(), RestError::class.java)?.error
        ?: ""