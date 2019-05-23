package com.example.shop

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class BoxInfoTest {

    @Test
    fun testBoxInfo() {
        val ekamBoxInfo = BoxInfo("ddd", "sss", "rrr",
                listOf(ScannerInfo("ppp", "www", DeviceType.SCANNER, "qqq"),
                        ScaleInfo("ppp", "www", DeviceType.LIBRA, "qqq"),
                        ScaleUsbInfo("ppp", "www", DeviceType.LIBRAUSB, "qqq"),
                        EscposPrinterInfo("ppp", "www", DeviceType.ESCPOS_PRINTER, "qqq"),
                        AcquiringInfo("ppp", "www", DeviceType.ACQUIRE, "qqq"),
                        FiscalDeviceInfo("ppp", "www", DeviceType.PRINTER, "qqq", FiscalPrinterType.ATOL,
                                70, "ooo", "nnn", "fff", "zzz")))
        val adapterFactory = RuntimeTypeAdapterFactory.of(DeviceInfo::class.java, "deviceTypeId")
                .registerSubtype(FiscalDeviceInfo::class.java, "1")
                .registerSubtype(ScaleInfo::class.java, "2")
                .registerSubtype(ScannerInfo::class.java, "3")
                .registerSubtype(AcquiringInfo::class.java, "4")
                .registerSubtype(ScaleUsbInfo::class.java, "5")
                .registerSubtype(EscposPrinterInfo::class.java, "6")

        val gson = GsonBuilder().registerSerializers().registerTypeAdapterFactory(adapterFactory).create()
        val json = gson.toJson(ekamBoxInfo)
        val resultBoxInfo = gson.fromJson<BoxInfo>(json, BoxInfo::class.java)
        print("$json\n")
        assert(ekamBoxInfo == resultEkamBoxInfo)
    }
}