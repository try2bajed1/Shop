package com.example.shop.raspberry

import android.util.Log.w
import com.example.shop.utils.RxBus
import com.example.shop.db.DatabaseHelper
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.prefs.Preferences

class BoxDevicesSocketBinder(private val databaseHelper: DatabaseHelper,
                                 private val preferences: Preferences,
                                 private val devicesBinder: LocalDevicesBinder) {

    val subject: PublishSubject<String> = PublishSubject.create<String>()
    private var disposable: Disposable? = null
    private var lastDevices = listOf<DeviceBD>()

    fun subscribe() {
        disposable = subject
                .flatMap(::handleText)
                .map(::deserialize)
                .map(BoxInfoToDBDeviceMapper()::map)
                .map(::handleDevices)
                .subscribe(::success, ::error)
    }

    fun unsubscribe() {
        disposable?.dispose()
    }

    private fun success(devices: List<DeviceBD>) {
        RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
        lastDevices = devices
    }

    private fun error(throwable: Throwable) {
        RxBus.instanceOf().dispatchEvent(InvalidateDevicesEvent())
        if (BuildConfig.DEBUG)
            throwable.printStackTrace()
    }

    /**
     * Числа взяты из [DeviceType]
     */
    private fun deserialize(text: String): BoxInfo? {
        val adapterFactory = RuntimeTypeAdapterFactory.of(DeviceInfo::class.java, "device_type_id")
                .registerSubtype(FiscalDeviceInfo::class.java, "1")
                .registerSubtype(ScaleInfo::class.java, "2")
                .registerSubtype(ScannerInfo::class.java, "3")
                .registerSubtype(AcquiringInfo::class.java, "4")
                .registerSubtype(ScaleUsbInfo::class.java, "5")
                .registerSubtype(EscposPrinterInfo::class.java, "6")
        return GsonBuilder()
                .registerSerializers().registerTypeAdapterFactory(adapterFactory)
                .create().fromJson<BoxInfo>(text, BoxInfo::class.java)
    }

    private fun handleText(text: String) =
            when {
                preferences.piWasBound() -> Observable.just(text)
                else -> Observable.error(Exception("Box isn't bound"))
            }

    private fun handleDevices(devices: List<DeviceBD>) = devices.also { list ->
        w("handleDevices $list")
        //Добавляем в базу новые устройства и активируем старые
        saveAndActivateDevices(list)
        //Получаем разницу между новым и старым списком устройств
        //first - new connected ; second - disconnected
        val pair = list.getDifference(lastDevices) { it.serialNum }
        //Отмечаем новые подключенные устройства
        pair.first.findLast { it.type == ConstDevices.PrinterAtol }?.let { preferences.isBoxPrinterConnected = true }
        pair.first.findLast { it.type == ConstDevices.Scale }?.let { preferences.isBoxScaleConnected = true }
        pair.first.findLast { it.type == ConstDevices.Scanner }?.let { preferences.isBoxScannerConnected = true }
        //Отмечаем отключенные устройства
        pair.second.findLast { it.type == ConstDevices.PrinterAtol }?.let { preferences.isBoxPrinterConnected = false }
        pair.second.findLast { it.type == ConstDevices.Scale }?.let { preferences.isBoxScaleConnected = false }
        pair.second.findLast { it.type == ConstDevices.Scanner }?.let { preferences.isBoxScannerConnected = false }
    }

    private fun saveAndActivateDevices(devices: List<DeviceBD>) {
        devices.findLast { it.type == ConstDevices.PrinterAtol }?.let { devicesBinder.makeInactiveAllPrinters() }
        val activePrinterDoesNotExists = doesNotActivePrinterExist()
        devices.forEach {
            when {
                getChildExistsAlready(it) == null -> dao().createOrUpdate(it)
                activePrinterDoesNotExists && it.type == ConstDevices.PrinterAtol -> devicesBinder.makeActive(it.serialNum)
            }
        }
    }

    private fun getChildExistsAlready(childDevice: DeviceBD): DeviceBD? {
        return dao().queryBuilder().where().eq(DeviceBD.FIELD_SERIAL_NUM, childDevice.serialNum)
                .and().eq(DeviceBD.FIELD_CONNECTION_TYPE, ConstDevices.Connect_BOX)
                .query().takeIf { it.isNotEmpty() }?.component1()
    }

    private fun doesNotActivePrinterExist(): Boolean {
        return dao().queryBuilder().where()
                .eq(DeviceBD.FIELD_TYPE, ConstDevices.PrinterAtol).and()
                .eq(DeviceBD.FIELD_IS_ACTIVE, true)
                .query().isEmpty()
    }

    private fun dao() = databaseHelper.devicesDao
}