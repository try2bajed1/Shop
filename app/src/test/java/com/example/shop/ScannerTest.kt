package com.example.shop

import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by aleksandrmulavka
 * on 22.03.18.
 */

@RunWith(RobolectricTestRunner::class)
class ScannerTest {

    private lateinit var barcodeScannedList: ArrayList<BarcodeScanned>
    val barcodes:List<String> = listOf("1234567891231","9002236311036", "7123456789015", "9783161484100", "5000127162754", "7501054530107", "5060204123733")

    @Before
    fun createBarcodeScanned(){
        barcodeScannedList = ArrayList()
        for(code:String in barcodes){
            barcodeScannedList.add(BarcodeScanned(ScannerData(code)))
        }
    }

    @Test
    fun EAN13Test(){
        for(barcodeScanned: BarcodeScanned in barcodeScannedList){
            assertTrue(barcodeScanned.isEAN13())
        }
    }
}