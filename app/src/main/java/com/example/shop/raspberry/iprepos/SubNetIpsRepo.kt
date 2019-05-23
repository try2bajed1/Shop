package com.example.shop.raspberry.iprepos

import android.content.Context
import android.net.wifi.WifiManager
import com.example.shop.app.AppSingleton
import java.net.*

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 08.12.17
 * Time: 12:07
 */
class SubNetIpsRepo(val wifiManager: WifiManager = AppSingleton
                                                .INSTANCE
                                                .applicationContext
                                                .getSystemService(Context.WIFI_SERVICE) as WifiManager) : IIpRepo {


    override fun getIps(): List<String> {
//        if(BuildConfig.DEBUG)
//            return listOf("192.168.1.1","192.168.1.2","192.168.1.1","192.168.1.1","192.168.1.1","192.168.1.1","192.168.1.1","192.168.1.1")
        
        val ipAddress = NetworkUtils.getIPAddress(wifiManager.connectionInfo.ipAddress) ?:""

        val netPrefix = getNetPrefix()

        if(ipAddress.isEmpty() || !lessThan256(netPrefix))
            return emptyList()

        return getNetPrefix()
                .let { getMaskFromNetPrefix(it) }
                .let { SubnetUtils(ipAddress, it) }.info.allAddresses.toList()
    }



    //пока условились не больше 256 айпи адресов в подсети
    private fun lessThan256(netPrefix: Int): Boolean = netPrefix in 24..32


    private fun getNetPrefix(): Int {
        var netPrefix: Short = 0
        val inetAddress = intToInetAddress(wifiManager.dhcpInfo?.ipAddress ?: 0)

        return try {
            inetAddress.let { NetworkInterface.getByInetAddress(it)?.interfaceAddresses }
                    ?.filter { !it.address.isLoopbackAddress && isIP_v4(it) }
                    ?.forEach { netPrefix = it.networkPrefixLength }
                    ?.let { netPrefix.toInt() } ?: 0
        } catch (e: NullPointerException) {
            return try {
                val nwis = NetworkInterface.getNetworkInterfaces() ?: return 0
                nwis.toList()
                .filter { it.displayName.contains("eth") }
                .map { it.interfaceAddresses }
                .flatMap { it }
                .filter { !it.address.isLoopbackAddress && isIP_v4(it) }
                .forEach { netPrefix = it.networkPrefixLength }
                .let { netPrefix.toInt() }

            } catch (e1: SocketException) {
                0
            }
        } catch (e: SocketException) { // нпе на networkInterface.getInterfaceAddresses(), если вайфай выключен
            0
        }
    }

    private fun getMaskFromNetPrefix(netPrefix: Int): String {
        try {
            var shiftby = 1 shl 31
            for (i in netPrefix - 1 downTo 1) {
                shiftby = shiftby shr 1
            }
            return Integer.toString(shiftby shr 24 and 255) +
                    "." + Integer.toString(shiftby shr 16 and 255) +
                    "." + Integer.toString(shiftby shr 8 and 255) +
                    "." + Integer.toString(shiftby and 255)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "0"
    }

    private fun intToInetAddress(hostAddress: Int): InetAddress {
        val addressBytes = byteArrayOf((0xff and hostAddress).toByte(),
                                       (0xff and (hostAddress shr 8)).toByte(),
                                       (0xff and (hostAddress shr 16)).toByte(),
                                       (0xff and (hostAddress shr 24)).toByte())
        try {
            return InetAddress.getByAddress(addressBytes)
        } catch (e: UnknownHostException) {
            throw AssertionError()
        }

    }


    private fun isIP_v4(address: InterfaceAddress): Boolean {
        return InetAddressUtils.isIPv4Address(address.address.hostAddress)
    }



}