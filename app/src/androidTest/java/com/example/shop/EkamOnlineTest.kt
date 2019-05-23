package com.example.shop

import android.support.test.runner.AndroidJUnit4
import com.example.shop.app.AppSingleton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.prefs.Preferences

/**
 * Created with IntelliJ IDEA.
 * User: nick
 * Date: 09/04/2019
 * Time: 10:13
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OnlineTest {

    private val databaseHelper = AppSingleton.INSTANCE.databaseHelper
    private val receiptsRepo = ReceiptsRepo(databaseHelper)
    private val resultsRepo = ResultsRepo(databaseHelper)
    private val errorsRepo = ErrorsRepo(databaseHelper)
    private val operationsRepo = OperationsRepo(databaseHelper)
    private val eoExitUseCase = EOExitUseCase(ReceiptsRepo(databaseHelper),
            ResultsRepo(databaseHelper),
            ErrorsRepo(databaseHelper),
            OperationsRepo(databaseHelper),
            Preferences.getInstance())


    private val printUseCase: EOPrintUseCase = EOPrintUseCase(
            SmartposPrinter(),
            EOBackendStub(),
            null,
            receiptsRepo,
            resultsRepo,
            errorsRepo,
            operationsRepo)

    private val printWithZRequired: EOPrintUseCase = EOPrintUseCase(
            SmartposWithFakeZRequired(),
            EOBackendStub(),
            null,
            receiptsRepo,
            resultsRepo,
            errorsRepo,
            operationsRepo)

    private val printWithError: EOPrintUseCase = EOPrintUseCase(
            SmartposWithFakeError(),
            EOBackendStub(),
            null,
            receiptsRepo,
            resultsRepo,
            errorsRepo,
            operationsRepo)


    @Before
    fun setUp() {
        //чистим все таблицы для ео
        eoExitUseCase.exit()
    }


    @Test
    fun testAfterFirstIteration() {
        allReposAreEmpty()
        printUseCase.coreObs(1).blockingLast()

        assertEquals(1L, receiptsRepo.total)
        assertEquals("pending", receiptsRepo.getAll()?.getOrNull(0)?.status)

        assertEquals(1L, operationsRepo.total)
        assertEquals(OpsTypes.RECEIVE, operationsRepo.getAll()?.getOrNull(0)?.operationType)

        assertEquals(0L, resultsRepo.total)
        assertEquals(0L, errorsRepo.total)
    }


    @Test
    fun testAfterSecondIteration() {
        allReposAreEmpty()
        printUseCase.coreObs(2).blockingLast()
        checkAfter()
    }


    @Test
    fun testZReq() {
        allReposAreEmpty()
        printWithZRequired.coreObs(2).blockingLast()
        val sale = receiptsRepo.getAll()?.getOrNull(0)
        assertEquals(2L, receiptsRepo.total) //z+default
        assertEquals(Status.PENDING.strVal, sale?.status)
        assertEquals(ReceiptType.CHECK_SALE.typeName, sale?.type)
        assertEquals(ReceiptType.CHECK_SHIFTCLOSE.typeName, receiptsRepo.getAll()?.getOrNull(1)?.type)
        assertEquals(3L, operationsRepo.total)
//        assertEquals(OpsTypes.RECEIVE, operationsRepo.getAll()?.getOrNull(0)?.operationType)
//        assertEquals(OpsTypes.PRINTED, operationsRepo.getAll()?.getOrNull(1)?.operationType)
        assertEquals(1L, resultsRepo.total)
        assertEquals(0L, errorsRepo.total)
    }



    @Test
    fun testZReqAfter() {
        allReposAreEmpty()
        printWithZRequired.coreObs(3).blockingLast()

//        assertEquals(2L, receiptsRepo.total) //z+default
        val allSorted = receiptsRepo.getAllSorted()
        assertEquals(2,allSorted.size)

        val shiftClose = allSorted?.getOrNull(0)
        assertEquals(Status.PRINTED.strVal, shiftClose?.status)
        assertEquals(ReceiptType.CHECK_SHIFTCLOSE.typeName, shiftClose?.type)

        val sale = allSorted?.getOrNull(1)
        assertEquals(Status.PRINTED.strVal, sale?.status)
        assertEquals(ReceiptType.CHECK_SALE.typeName, sale?.type)


        assertEquals(4L, operationsRepo.total)
        assertEquals(2L, resultsRepo.total)
        assertEquals(0L, errorsRepo.total)
    }


    @Test
    fun testZReqAfter4() {
        allReposAreEmpty()
        printWithZRequired.coreObs(4).blockingLast()
        assertEquals(5L, operationsRepo.total)
        assertEquals(0, resultsRepo.total)
        assertEquals(0L, errorsRepo.total)
    }

    @Test
    fun testExit() {
        allReposAreEmpty()
        printUseCase.coreObs(2).blockingLast()
        checkAfter()
        eoExitUseCase.exit()
        allReposAreEmpty()
    }


    private fun allReposAreEmpty() {
        assertTrue(receiptsRepo.total == 0L)
        assertTrue(operationsRepo.total == 0L)
        assertTrue(resultsRepo.total == 0L)
        assertTrue(errorsRepo.total == 0L)
    }


    private fun checkAfter() {
        assertEquals(1L, receiptsRepo.total)
        assertEquals(Status.PRINTED.strVal, receiptsRepo.getAll()?.getOrNull(0)?.status)
        assertEquals(ReceiptType.CHECK_SALE.typeName, receiptsRepo.getAll()?.getOrNull(0)?.type)
        assertEquals(2L, operationsRepo.total)
        assertEquals(1L, resultsRepo.total)
        assertEquals(Result.PROCESSED, resultsRepo.getAll()?.getOrNull(0)?.status)
        assertEquals(0L, errorsRepo.total)
    }


    @Test
    fun testErr() {
        allReposAreEmpty()
        printWithError.coreObs(1).blockingLast()
        assertEquals(1L, receiptsRepo.total)
        assertEquals(0L, errorsRepo.total)
        assertEquals(0L, resultsRepo.total)
        assertEquals(1L, operationsRepo.total)
        assertEquals(OpsTypes.RECEIVE, operationsRepo.getAll()[0].operationType)
    }
    

    @Test
    fun testErr2Iterations() {
        allReposAreEmpty()
        printWithError.coreObs(2).blockingLast()
        assertEquals(0L, receiptsRepo.total)
        assertEquals(0L, errorsRepo.total)
        assertEquals(2L, operationsRepo.total)
        assertEquals(OpsTypes.RECEIVE, operationsRepo.getAll()[0].operationType)
        assertEquals(OpsTypes.FAIL, operationsRepo.getAll()[1].operationType)
    }


    @Test
    fun testErr4Iterations() {
        allReposAreEmpty()
        printWithError.coreObs(4).blockingLast()
        assertEquals(1L, receiptsRepo.total)
        assertEquals(0L, errorsRepo.total)
        assertEquals(4L, operationsRepo.total)
        assertEquals(1L, resultsRepo.total)

        assertTrue(receiptsRepo.getAll()[0].status == Status.PRINTED.strVal)
/*

        val allOps = operationsRepo.getAllSorted()
        assertEquals(OpsTypes.PRINTED,allOps.getOrNull(3)?.operationType)
        assertEquals(OpsTypes.RECEIVE,allOps.getOrNull(2)?.operationType)
        assertEquals(OpsTypes.FAIL,allOps.getOrNull(1)?.operationType)
        assertEquals(OpsTypes.RECEIVE,allOps.getOrNull(0)?.operationType)
*/

    }




}


