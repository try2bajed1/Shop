package com.example.shop

import java.math.BigDecimal
import java.util.*
import kotlin.math.absoluteValue

val random = Random()

fun randomInt() = random.nextInt()
fun randomLong() = random.nextLong()
fun randomType() = if (random.nextBoolean()) ReceiptType.CHECK_SALE.typeName else ReceiptType.CHECK_RETURN.typeName
fun randomMoneyDecimal() = BigDecimal(random.nextInt().toString() + "." + 99)
fun randomBoolean() = random.nextBoolean()

fun randomDate() = Date(random.nextLong().absoluteValue)
private const val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_+=-abcdefghijklmnopqrstuvwxyz[]{};:'"
fun randomString(length: Long) =
        (0 until length).asSequence()
                .map { source[random.nextInt(source.length)] }
                .joinToString(separator = "")

fun createRandomError(receiptRequestId: Long) =
        ReceiptRequestError(
                receiptRequestId = receiptRequestId,
                errorCode = randomInt(),
                errorDescription = randomString(256),
                recoverable = randomBoolean()
        )

fun createReceiptRequestWithoutNullFields(id: Long, linesNumber: Int, lineIdBasValue: Long = id) =
        SaleReceiptDB(id = id,
                lines = (0 until linesNumber).map { createLinesWithoutNulls(lineIdBasValue + it, id) },
                accountId = randomInt(),
                fiscalcopy = false,
                webCashboxId = randomInt(),
                type = ReceiptType.CHECK_SALE.typeName,
                status = "pending",
                kktReceiptId = randomLong(),
                amount = BigDecimal(1000),
                cashAmount = randomMoneyDecimal(),
                electronAmount = randomMoneyDecimal(),
                prepaidAmount = randomMoneyDecimal(),
                postpaidAmount = randomMoneyDecimal(),
                counterOfferAmount = randomMoneyDecimal(),
                uuid = UUID.randomUUID().toString(),
                cashierName = randomString(20),
                email = randomString(20),
                phoneNumber = randomString(20),
                shouldPrint = false,
                orderId = randomString(8),
                orderNumber = randomString(12),
                createdAt = java.util.Date(),
                updatedAt = java.util.Date(),
                archivedAt = java.util.Date(),
                lockedPreviously = randomBoolean(),
                transactionAddress = randomString(20),
                cashierInn = randomString(20),
                cashierRole = randomString(20))

fun createRandomResult(id: Long) =
        Result(id,
                Result.PENDING, 
                randomInt(),
                randomString(64),
                randomString(64),
                randomString(64),
                randomString(64),
                randomString(64),
                randomString(64),
                randomInt(),
                DateTime(randomDate()),
                randomString(64),
                randomInt(),
                randomString(10),
                randomString(256),
                10,
                randomString(64),
                randomString(64),
                DateTime(randomDate()),
                DateTime(randomDate()),
                "1.0")

fun createLinesWithoutNulls(id: Long, receiptRequestId: Long) =
        Line(id = id,
                receiptId = receiptRequestId,
                title = randomString(20),
                quantity = BigDecimal.ONE,
                totalPrice = BigDecimal(1000),
                price = BigDecimal(1000),
                vatRate = 0,
                vatAmount = randomMoneyDecimal(),
                createdAt = randomDate(),
                updatedAt = randomDate(),
                fiscalProductType = 1,
                paymentCase = 4)





































