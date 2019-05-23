package com.example.shop

import android.os.Environment
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WifiDataUtilsTest {

    private val wifiData1 = WifiData()
    private val wifiData2 = WifiData("111", "111")
    private val wifiData3 = WifiData("at3tRtA3", "ak3AjR3")
    private val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/folder"
    private val fileName = "TestFileName.json"

    @Test
    fun writeAndRemoveAndReadTest() {
        this.writeAndRemoveAndRead(this.wifiData1)
        this.writeAndRemoveAndRead(this.wifiData2)
        this.writeAndRemoveAndRead(this.wifiData3)
    }

    @Test
    fun writeAndReadTest() {
        this.writeAndRead(this.wifiData1)
        this.writeAndRead(this.wifiData2)
        this.writeAndRead(this.wifiData3)
    }

    private fun writeAndRead(wifiData: WifiData) {
        val wifiDataResult = WifiData()
        assert(wifiData.writeFrom(InstrumentationRegistry.getContext(), this.folderPath, this.fileName))
        assert(wifiDataResult.readTo(this.folderPath, this.fileName))
        assert(wifiData.hotspot == wifiDataResult.hotspot)
        assert(wifiData.password == wifiDataResult.password)
    }

    private fun writeAndRemoveAndRead(wifiData: WifiData) {
        val wifiDataResult = WifiData()
        this.writeAndRead(wifiData)
        assert(wifiData.renameAndRemoveFileIfHostEmpty(this.folderPath, this.fileName))
        assert(!wifiDataResult.readTo(this.folderPath, this.fileName))
        assert(null == wifiDataResult.hotspot)
        assert(null == wifiDataResult.password)
    }
}