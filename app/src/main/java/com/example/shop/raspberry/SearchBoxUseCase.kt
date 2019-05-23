package com.example.shop.raspberry

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import com.example.shop.raspberry.iprepos.IpsRepo
import com.example.shop.raspberry.iprepos.IIpRepo
import java.util.prefs.Preferences

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 08.12.17
 * Time: 18:36
 */
class SearchBoxUseCase {

    private val ipsRepo: IIpRepo = IpsRepo()
    private val heapDevicesRepo = HeapDevicesRepo.INSTANCE
    private val apiClient = BoxRestApi.create()
    private val preferences = Preferences.getInstance()

    fun getScanListObs():Observable<BoxInfo> {
        return Observable.just(ipsRepo.getIps())
                         .flatMap(::ipsOrError)
                         .doOnSubscribe{ heapDevicesRepo.clear()}
                         .flatMap(::separateSchedulerPing,
                                  ::processBoxData)
                         .flatMap(::beaconObs)
                         .filter { it.isOnline }
                         .map(::onDeviceFound)
    }

    
    fun getSingleIpObs(ip:String):Observable<BoxInfo> {
        return Observable.just(listOf(ip))
                .flatMap{
                    if(it.isEmpty())
                        Observable.error<String>(Throwable("Некорректные настройки сети"))
                    else
                        Observable.fromIterable(it)
                }
                .doOnSubscribe{ heapDevicesRepo.clear()}
                .flatMap(::separateSchedulerPingForSingle,
                        ::processBoxData)
                .flatMap(::beaconObs)
                .filter { it.isOnline }
                .doOnNext { heapDevicesRepo.add(it) }
                .flatMap { boxDevicesBinder().getBindBoxObs(it.serialNumber) }
    }



    private fun beaconObs(boxInfo: BoxInfo): Observable<BoxInfo> {
        return beaconObs(BoxInfo.host)
                .map {
                    it.apply {
                        isOnline = true
                        ip = BoxInfo.ip
                        lastSeen = System.currentTimeMillis()
                        host = BoxInfo.host + "/"
                    }
                }
                .onErrorResumeNext(Observable.just(BoxInfo().apply { isOnline = false }))
    }


    fun beaconObs(host: String): Observable<BoxInfo> {
        return apiClient.beacon("$host/beacon")
    }


    private fun onDeviceFound(Info: BoxInfo): BoxInfo {
        heapDevicesRepo.add(Info)

        //если наша коробка нашлась, но айпи изменился
        val ipChanged = Info.serialNumber == preferences.raspberrySerial
                && preferences.piIp != Info.ip

        if (ipChanged) {
            preferences.boundPi(Info)
        }
        return Info
    }


    private fun ipsOrError(l: List<String>):Observable<String> {
        return if (l.isEmpty()) {
            Observable.error<String>(Throwable("Некорректные настройки сети"))
        } else {
            Observable.fromIterable(ipsRepo.getIps())
        }
    }


    private fun separateSchedulerPing(s: String): Observable<BoxInfo> {
            return apiClient.ping(getUrl(s))
                .onErrorResumeNext( Observable.empty<BoxInfo>())
                .subscribeOn(Schedulers.io())
    }

    private fun separateSchedulerPingForSingle(s: String): Observable<BoxInfo> {
            return apiClient.ping(getUrl(s))
                .subscribeOn(Schedulers.io())
    }

    
    private fun processBoxData(s: String, Info: BoxInfo): BoxInfo {
        return Info.apply {
            ip = s
            host = getUrl(s)
        }
    }


}