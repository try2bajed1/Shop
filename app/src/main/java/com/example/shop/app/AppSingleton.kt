package com.example.shop.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log.w

import android.support.v7.app.AppCompatDelegate
import android.util.Log
import android.util.Pair
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.shop.BuildConfig
import com.example.shop.R
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences


/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 29.04.15
 * Time: 18:12
 */

class AppSingleton : Application() {

    fun isServiceRunningInForeground(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return service.foreground
            }
        }
        return false
    }


    lateinit var databaseHelper: DatabaseHelper



    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        databaseHelper = DatabaseHelper(this, appCodeVersion)

        initOrientation()
        initPackage()
        cleanCatalogIf()
        initSmartpos()
        registerReceiversForUSBDevices()
        initSockets()
        initApkFromMarket()
        initTerminalsState()
        initScannerState()
        initScaleState()
        initAtolState()
        initNetworkReceiver()
    }

    private fun initNetworkReceiver() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                RxBus.instanceOf().dispatchEvent(NetworkIsChangedEvent())
            }
        }, IntentFilter(CONNECTIVITY_CHANGE))
    }

    private fun initAtolState() {
        Preferences.getInstance().isUsbPrinterConnected = Build.MODEL == "SHTRIH-SMARTPOS-F" || (getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.filter { it.value.isAtol() }.isNotEmpty()
    }

    private fun initScannerState() {
        Preferences.getInstance().isScannerConnected = (getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.filter { it.value.isScanner() }.isNotEmpty()
    }

    private fun initScaleState() {
        Preferences.getInstance().isScaleConnected = (getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.filter { it.value.isMassaK() }.isNotEmpty()
    }

    fun initTerminalsIf() {
        if (terminalsAreNotInitialized) {
            subscribeInpas()
            subscribeIngenico()
            findInpasIf()
            findIngenicoIf()
            terminalsAreNotInitialized = true
        }
    }


    private fun initOrientation() {
        Preferences.getInstance().let {
            if (it.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                it.orientation = when {
                    resources.getBoolean(R.bool.isTablet) -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                INSTANCE.analyticSender.orientation(it.orientation)
            }
            registerActivityLifecycleCallbacks(ActivityLifecycleForOrientation())
        }
    }


    private fun initPackage() {
        try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            PMEngine.init(applicationContext, TOKEN, String.format("%s %s (%d)", pi.packageName, pi.versionName, pi.versionCode), "ru")
        } catch (x: Throwable) {
            x.printStackTrace()
            CrashlyticsParamsSendUtils.sendException(x)
        }
    }

    private fun cleanCatalogIf() {
        if (BuildConfig.CLEAN_CATALOG_WHILE_SYNC) {
            with(databaseHelper) {
                categoriesDao.deleteBuilder().delete()
                productsDao.deleteBuilder().delete()
                variantsDao.deleteBuilder().delete()
            }

            with(Preferences.getInstance()) {
                clearPrefsCategory()
                clearProductPrefs()
                removeHaveFillLoadedCatalog()
            }
        }
    }

    private fun initSmartpos() {
        if (Build.MODEL == "SHTRIH-SMARTPOS-F") {
            LocalDevicesBinder().bindSmartpos()
        }
    }

    private fun initTerminalsState() {
        (getSystemService(Context.USB_SERVICE) as? UsbManager)?.deviceList?.let { devicesHashMap ->
            inpasDeviceState = when {
                devicesHashMap.filter { it.value.isInpas() }.isNotEmpty() -> {
                    if (BuildConfig.DEBUG)
                        Log.d("InpasInitTerminalsState", "ATTACHED")
                    Inpas.DeviceState.ATTACHED
                }
                else -> {
                    if (BuildConfig.DEBUG)
                        Log.d("InpasInitTerminalsState", "DETACHED")
                    Preferences.getInstance().setInpasTerminalId(null)
                    Inpas.DeviceState.DETACHED
                }
            }
            ingenicoDeviceState = when {
                devicesHashMap.filter { it.value.isIngenico() }.isNotEmpty() -> {
                    if (BuildConfig.DEBUG)
                        Log.d("IngenInitTerminalsState", "ATTACHED")
                    Inpas.DeviceState.ATTACHED
                }
                else -> {
                    if (BuildConfig.DEBUG)
                        Log.d("IngenInitTerminalsState", "DETACHED")
                    Preferences.getInstance().setIngenicoTerminalId(null)
                    Inpas.DeviceState.DETACHED
                }
            }
        }
    }

    private fun initSockets() {
        socketConnection = SocketConnection(this)
        if (Preferences.getInstance().piWasBound()) {
            BackgroundManager.get(this).registerListener(appActivityListener)
        }
    }

    private fun initApkFromMarket() {
        Preferences.getInstance().putIsApkFromMarket(this.isApkFromMarket())
    }

    private fun registerReceiversForUSBDevices() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (BuildConfig.DEBUG)
                    Log.d("Receiver", "onReceive $ACTION_ATTACHED")
                (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice)?.let {
                    when {
                        it.isAtol() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_PRINTER_IS_ATTACHED, listOf())
                            Preferences.getInstance().isUsbPrinterConnected = true
                            LocalDevicesBinder().updateAtolUSB(it.vendorId, it.productId, it.deviceName, "").subscribe({ RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent()) }, Throwable::printStackTrace)
                        }
                        it.isScanner() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_SCANNER_IS_ATTACHED, listOf())
                            Preferences.getInstance().isScannerConnected = true
                            val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) it.productName
                                    ?: "" else ""
                            LocalDevicesBinder().updateScanner(name).subscribe({ RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent()) }, Throwable::printStackTrace)
                        }
                        it.isInpas() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_INPAS_IS_ATTACHED, listOf())
                            Preferences.getInstance().setInpasTerminalSerial(when {
                                it.isProductInpasVerifone520C() -> Inpas.TERMINAL_VERIFONE_520C_SERIAL
                                it.isProductInpasVerifone820() -> Inpas.TERMINAL_VERIFONE_820_SERIAL
                                it.isProductInpasPaxD210() -> Inpas.TERMINAL_PAX_D210_SERIAL
                                else -> null
                            })
                            inpasDeviceState = Inpas.DeviceState.ATTACHED
                            findInpasIf()
                        }
                        it.isIngenico() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_INGENICO_IS_ATTACHED, listOf())
                            ingenicoDeviceState = Inpas.DeviceState.ATTACHED
                            findIngenicoIf()
                        }
                        it.isMassaK() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_SCALE_IS_ATTACHED, listOf())
                            val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
                            usbManager.deviceList.values.extractMassaK { device ->
                                connection = usbManager.openDevice(it)
                                connection?.let { connection ->
                                    Preferences.getInstance().isScaleConnected = true
                                    LocalDevicesBinder().updateScale().subscribe({ RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent()) }, Throwable::printStackTrace)
                                    UsbSerialDevice.createUsbSerialDevice(device, connection)?.apply {
                                        if (open()) {
                                            analyticSender.action(AnalyticSender.ACTION_SCALE_IS_OPENED, listOf())
                                            setBaudRate(4800)
                                            setDataBits(UsbSerialInterface.DATA_BITS_8)
                                            setStopBits(UsbSerialInterface.STOP_BITS_1)
                                            setParity(UsbSerialInterface.PARITY_EVEN)
                                            write("CMD_GET_MASSA".toByteArray())
                                            read(mCallback)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_OTHER_IS_ATTACHED, listOf())
                        }
                    }
                }
            }
        }, IntentFilter(ACTION_ATTACHED))

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (BuildConfig.DEBUG)
                    Log.d("Receiver", "onReceive $ACTION_DETACHED")
                (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice)?.let {
                    when {
                        it.isAtol() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_PRINTER_IS_DETACHED, listOf())
                            Preferences.getInstance().isUsbPrinterConnected = false
                            RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
                        }
                        it.isScanner() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_SCANNER_IS_DETACHED, listOf())
                            Preferences.getInstance().isScannerConnected = false
                            RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
                        }
                        it.isInpas() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_INPAS_IS_DETACHED, listOf())
                            inpasDeviceState = Inpas.DeviceState.DETACHED
                            findInpas()
                        }
                        it.isIngenico() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_INGENICO_IS_DETACHED, listOf())
                            ingenicoDeviceState = Inpas.DeviceState.DETACHED
                            ingenico.disconnect()
                        }
                        it.isMassaK() -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_SCALE_IS_DETACHED, listOf())
                            Preferences.getInstance().isScaleConnected = false
                            RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
                            connection?.close()
                        }
                        else -> {
                            analyticSender.action(AnalyticSender.ACTION_USB_OTHER_IS_DETACHED, listOf())
                        }
                    }
                }
            }
        }, IntentFilter(ACTION_DETACHED))

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (BuildConfig.DEBUG)
                    Log.d("Receiver", "onReceive $ACTION_USB_PERMISSION ${intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)}")
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice)?.takeIf { granted }?.let {
                            when {
                                it.isAtol() -> {
                                    analyticSender.action(AnalyticSender.ACTION_USB_PRINTER_PERMISSION_REQUEST, listOf(Pair("granted", granted.toString())))
                                }
                                it.isScanner() -> {
                                    analyticSender.action(AnalyticSender.ACTION_USB_SCANNER_PERMISSION_REQUEST, listOf(Pair("granted", granted.toString())))
                                }
                                it.isInpas() -> {
                                    analyticSender.action(AnalyticSender.ACTION_USB_INPAS_PERMISSION_REQUEST, listOf(Pair("granted", granted.toString())))
                                }
                                it.isIngenico() -> {
                                    analyticSender.action(AnalyticSender.ACTION_USB_INGENICO_PERMISSION_REQUEST, listOf(Pair("granted", granted.toString())))
                                    ingenicoDeviceState = Inpas.DeviceState.ATTACHED
                                    findIngenicoIf()
                                }
                                it.isMassaK() -> {
                                    analyticSender.action(AnalyticSender.ACTION_USB_SCALE_PERMISSION_REQUEST, listOf(Pair("granted", granted.toString())))
                                }
                                else -> {
                                    analyticSender.action(AnalyticSender.ACTION_USB_OTHER_PERMISSION_REQUEST, listOf(Pair("granted", granted.toString())))
                                }
                            }
                        }
                    }
                }
            }
        }, IntentFilter(ACTION_USB_PERMISSION))
    }

    private fun Collection<UsbDevice>.extractMassaK(connectionFunction: (usbDevice: UsbDevice?) -> Unit) =
            filter { it.isMassaK() }.getOrNull(0)?.also { connectionFunction(it) }

    // A callback for received data must be defined
    private val mCallback = UsbSerialInterface.UsbReadCallback {
        Log.w("@", it.map { it.toInt() }.toString())
        Log.w("@", parseWeightingResult(it).toString())

        RxBus.instanceOf().dispatchEvent(WeightLibra(LibraData(parseWeightingResult(it).toString())))
    }

    private fun parseWeightingResult(data: ByteArray): BigDecimal? {
        if (data.size != 10)
            return null
        if (data[0].unsignedByte().toInt() != 0x55 || data[1].unsignedByte().toInt() != 0xAA)
        // префикс
            return null
        if (data[4].unsignedByte().toInt() != 0x00 && data[4].unsignedByte().toInt() != 0x80)
        // знак
            return null
        if (data[0] != data[5] || data[1] != data[6] || data[2] != data[7] || data[3] != data[8] || data[4] != data[9])
            return null

        val value = data[2].unsignedByte().toInt() + (data[3].unsignedByte().toInt() shl 8)
        val result = if (data[4].unsignedByte().toInt() == 0x80) -value else value
        return BigDecimal(result).divide(BigDecimal(1000))
    }

    private fun findInpasIf() {
        if (inpasDeviceState == Inpas.DeviceState.ATTACHED)
            inpas.runIdGetting()
    }

    private fun findInpas() {
        inpas.runIdGetting()
    }

    private fun findIngenicoIf() {
        if (ingenicoDeviceState == Inpas.DeviceState.ATTACHED)
            ingenico.connect()
    }

    /**
     * Если разрешения для инженико не даны, запрашивает их
     * Используем SuppressLint, если [PermissionHelper.checkAllPermissions] не возвращает false для старых версий android
     */
    @SuppressLint("NewApi")
    fun requestIngenicoPermissionsIf(activity: BaseActivity<*, *>) {
        if (ingenicoDeviceState == Inpas.DeviceState.ATTACHED && !INGENICO_PERMISSION_HELPER.checkAllPermissions(activity))
            INGENICO_PERMISSION_HELPER.requestAllPermissions(activity)
    }

    /**
     * Возвращает true, только если все необходимые разрешения даны
     * Если разрешениия не даны, выводит toast с предупреждением о том, что разрешения необходимы
     * Поместить в [Activity.onRequestPermissionsResult]
     * Используем SuppressLint, если [PermissionHelper.checkPermissions] не возвращает false для старых версий android
     */
    @SuppressLint("NewApi")
    fun handleRequestIngenicoPermissions(activity: BaseActivity<*, *>, requestCode: Int) =
            when (INGENICO_PERMISSION_HELPER.handleAllPermissionsRequestResult(activity, requestCode)) {
                PermissionHelper.RESULT_OK -> true
                else -> {
                    Toasty.warning(activity, INGENICO_PERMISSION_HELPER.getNecessaryPermissionMessage(), Toast.LENGTH_LONG).show()
                    false
                }
            }

    private fun subscribeInpas() {
        val inpasSub = inpas.subject.filter { it.operation == AcquiringOperation.CONNECTION_CHECKING }.subscribe({ event ->
            synchronized(this) {
                LocalDevicesBinder().updateInpasUsbBindObs().subscribe({ inpasDeviceState = Inpas.DeviceState.CONNECTED }, { })
                RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
                inpasSubscription?.dispose()
                if (!event.success && inpasDeviceState == Inpas.DeviceState.ATTACHED)
                    inpasSubscription = Completable.timer(1, TimeUnit.SECONDS)
                            .concatWith(Completable.fromAction { findInpasIf() })
                            .subscribe({}, Throwable::printStackTrace)
            }
        }, Throwable::printStackTrace)
    }

    private fun subscribeIngenico() {
        val ingenicoSub = ingenico.subject.filter { it.operation == AcquiringOperation.CONNECTION_CHECKING }.subscribe({ event ->
            synchronized(this) {
                LocalDevicesBinder().updateIngenicoUsbBindObs().subscribe({ ingenicoDeviceState = if (event.success) Inpas.DeviceState.CONNECTED else Inpas.DeviceState.ATTACHED }, {})
                RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
                ingenicoSubscription?.dispose()
                if (!event.success && ingenicoDeviceState == Inpas.DeviceState.ATTACHED)
                    ingenicoSubscription = Completable.timer(10, TimeUnit.SECONDS)
                            .concatWith(Completable.fromAction { findIngenicoIf() })
                            .subscribe({}, Throwable::printStackTrace)
            }
        }, Throwable::printStackTrace)
    }

    private val appActivityListener = object : BackgroundManager.Listener {
        override fun onBecameForeground() {
            openSocketConnection()
            w("Became Foreground")
        }

        override fun onBecameBackground() {
            closeSocketConnection()
            w("Became Background")
        }
    }

    fun closeSocketConnection() {
        socketConnection?.closeConnection()
    }

    fun openSocketConnection() {
        socketConnection?.openConnection()
    }


    fun setECRSettings() {
        val str = printerPrefs.getString("settings", null)
        if (str != null)
            ecr?.setDeviceSettings(str)
    }

    /*private boolean isHardwareKeyboardAvailable() {
        return getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
    }*/

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        when (newConfig.hardKeyboardHidden) {
            Configuration.HARDKEYBOARDHIDDEN_NO -> {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                applicationContext.sendBroadcast(Intent(SCANNER_ATTACHED))

            }
            Configuration.HARDKEYBOARDHIDDEN_UNDEFINED -> {

                //            Toast.makeText(this, "HARDKEYBOARDHIDDEN_UNDEFINED", Toast.LENGTH_LONG).show();

            }
            Configuration.HARDKEYBOARDHIDDEN_YES -> applicationContext.sendBroadcast(Intent(SCANNER_DETACHED))
        }

    }

    fun getCurrentUser(): User = currentUser ?: User().apply {
        this.id = Preferences.getInstance().lastCashierId
        this.name = Preferences.getInstance().lastCashierName
        currentUser = this
    }

    fun setCurrentUser(currentUser: User?) {
        if (currentUser != null) {
            Preferences.getInstance().lastCashierId = currentUser.id
            Preferences.getInstance().lastCashierName = currentUser.name
        }

        this.currentUser = currentUser
    }


    companion object {
        private const val SCANNER_ATTACHED = "scanner_attached"
        private const val SCANNER_DETACHED = "scanner_detached"
        private const val PRINTER_SETTINGS = "DeviceInfo"
        private const val TOKEN = "5ee59864731cf3feaaa0d7ce0b16f373955c3679635138934dfd0796e60c97ef"
        private const val CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"
        private const val ACTION_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        private const val ACTION_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        const val MASSA_K_VENDOR_ID = 1155
        const val MASSA_K_PRODUCT_ID = 22336
        const val SCANNER_HONEYWELL_VENDOR_ID_1 = 9168
        const val SCANNER_HONEYWELL_VENDOR_ID_2 = 3118
        const val SCANNER_HONEYWELL_YJ_HH360_PRODUCT_ID = 3105
        const val SCANNER_HONEYWELL_ECLIPSE_PRODUCT_ID = 512
        const val SCANNER_VIOTEH_VENDOR_ID = 4292
        const val SCANNER_VIOTEH_VT_1110_PRODUCT_ID = 65297

        private val NECESSARY_PERMISSIONS = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private val NECESSARY_PERMISSIONS_TEXT = listOf("мультимедиа")
        private const val REQUEST_PREFIX = "Для подключения терминала необходимы следующие разрешения:"
        private const val REQUEST_CODE = 192837465
        private val INGENICO_PERMISSION_HELPER = PermissionHelper(NECESSARY_PERMISSIONS, NECESSARY_PERMISSIONS_TEXT, listOf(), listOf(), REQUEST_PREFIX, REQUEST_CODE)


        lateinit var INSTANCE: AppSingleton
            private set

        fun disableBluetooth() {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.disable()
            }
        }

        fun enableBluetooth() {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.enable()
            }
        }
    }
}