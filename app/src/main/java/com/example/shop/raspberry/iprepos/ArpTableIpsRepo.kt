package com.example.shop.raspberry.iprepos

import java.util.regex.Pattern

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 08.12.17
 * Time: 17:34
 */
class ArpTableIpsRepo : IIpRepo {



//    private val ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
//    private val ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}"

    override fun getIps(): List<String> {

        val lineList = mutableListOf<String>()

/*
        File("/proc/net/arp")
            .bufferedReader()
            .useLines { lines->lines.forEach { lineList.add(it) } }
*/

        val ipRegex = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
        val pattern = Pattern.compile(ipRegex)

        return lineList.toList().filter { pattern.matcher(it).find() }
                       .map { pattern.matcher(it).group() }
    }

}