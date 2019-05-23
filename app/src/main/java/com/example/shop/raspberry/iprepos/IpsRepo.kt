package com.example.shop.raspberry.iprepos

import android.content.Context
import android.net.wifi.WifiManager
import com.example.shop.app.AppSingleton

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 08.12.17
 * Time: 20:59
 */
class IpsRepo(val wifiManager: WifiManager = AppSingleton
        .INSTANCE
        .applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager): IIpRepo {

    private fun inHotSpotMode(): Boolean    { // http://stackoverflow.com/questions/12401108/how-to-check-programmatically-if-hotspot-is-enabled-or-disabled
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("getWifiApState")
            method.isAccessible = true
            //todo: изза рефлекшна тут какой то трабл с выцеплением константы, поэтому сравниваем непосредственно со значением
            return method.invoke(wifiManager, listOf<Any>()) as Int == 13
        } catch (x: Exception) {
            false
        }
    }

    override fun getIps():List<String>{
        return if(inHotSpotMode())
                   ArpTableIpsRepo().getIps()
                else
                   SubNetIpsRepo().getIps()

    }


}