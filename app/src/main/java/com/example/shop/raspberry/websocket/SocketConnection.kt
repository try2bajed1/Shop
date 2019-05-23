package com.example.shop.raspberry.websocket


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log.w
import com.example.shop.app.AppSingleton
import com.example.shop.raspberry.BoxDevicesSocketBinder
import com.example.shop.utils.RxBus
import java.net.URI
import java.util.prefs.Preferences

class SocketConnection(private val context: Context) {

    private var webSocketClient: WebSocketClient? = null
    private var webSocketScanner: WebSocketClient? = null
    private var webSocketWeights: WebSocketClient? = null
    private val prefs = Preferences.getInstance()
    private val boxDevices = BoxDevicesSocketBinder(AppSingleton.INSTANCE.databaseHelper, Preferences.getInstance(), LocalDevicesBinder())

    fun openConnection() {
        webSocketClient?.close()
        webSocketScanner?.close()
        webSocketWeights?.close()
        try {
            webSocketClient = getRunningClient("device_info", ::onDevicesInfoReceived)
            boxDevices.subscribe()
            webSocketScanner = getRunningClient("scanning", ::processScannerData)
            webSocketWeights = getRunningClient("weighing", ::processLibraData)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initScreenStateListener()
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON)
                openConnection()
            else if (intent.action == Intent.ACTION_SCREEN_OFF)
                closeConnection()
        }
    }

    private fun getRunningClient(pathPrefix: String, processFun: (s: String) -> Unit) =
            object : WebSocketClient(URI("ws://${prefs.piIp}:8000/ws/v2/$pathPrefix")) {
                override fun onTextReceived(text: String) {
                    w("onTextReceived $text")
                    processFun(text)
                }

                override fun onOpen() {
                    w("onOpen $pathPrefix")
                    if (!prefs.isBoxConnected) {
                        prefs.isBoxConnected = true
                        RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
                    }
                }

                override fun onBinaryReceived(data: ByteArray) {
                    w("onBinaryReceived $pathPrefix")
                }

                override fun onPingReceived(data: ByteArray) {
                    w("onPingReceived $pathPrefix")
                }

                override fun onPongReceived(data: ByteArray) {
                    w("onPongReceived $pathPrefix")
                }

                override fun onException(e: Exception) {
                    w("onException $pathPrefix")
                    if (prefs.isBoxConnected) {
                        prefs.isBoxConnected = false
                        RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
                    }

                }

                override fun onCloseReceived() {
                    w("onCloseReceived $pathPrefix")
                }

            }.apply {
                setConnectTimeout(10000)
                setReadTimeout(60000)
                enableAutomaticReconnection(5000)
                connect()
            }

    private fun onDevicesInfoReceived(text: String) {
        boxDevices.subject.onNext(text)
    }

    private fun processScannerData(barcode: String) {
        if (barcode.isNotEmpty())
            RxBus.instanceOf().dispatchEvent(BarcodeScanned(Gson().fromJson(barcode, ScannerData::class.java)))
    }

    private fun processLibraData(weight: String) {
        RxBus.instanceOf().dispatchEvent(WeightLibra(Gson().fromJson(weight, LibraData::class.java)))
    }

    fun closeConnection() {
        webSocketClient?.close()
        boxDevices.unsubscribe()
        webSocketScanner?.close()
        webSocketWeights?.close()
        releaseScreenStateListener()
    }

    /**
     * Screen state listener for socket live cycle
     */

    private fun initScreenStateListener() {
        context.registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        context.registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun releaseScreenStateListener() {
        try {
            context.unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}