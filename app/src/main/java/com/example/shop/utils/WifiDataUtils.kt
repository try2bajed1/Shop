package com.example.shop.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import java.io.*


fun WifiData.renameAndRemoveFileIfHostEmpty(folderPath: String, fileName: String): Boolean {
    if (TextUtils.isEmpty(this.hotspot))
        File(folderPath, fileName).let { settingsFile ->
            if (settingsFile.exists()) {
                File(folderPath, "com/example/shop/db").let {
                    settingsFile.renameTo(it)
                    it.delete()
                }
            }
            return true
        }
    else
        return false
}

fun WifiData.writeFrom(context: Context, folderPath: String, fileName: String): Boolean {
    val dir = File(folderPath)
    dir.mkdirs()
    val file = File(dir, fileName)

    val asJson = Gson().toJson(WifiData(this.hotspot, this.password))

    return try {
        val fileOutputStream = FileOutputStream(file)
        PrintWriter(fileOutputStream).let {
            it.println(asJson)
            it.flush()
            it.close()
        }
        fileOutputStream.close()
        // фикс бага, чтобы в наутилусе появлялась папка сразу после записи файла
        makeFileDiscoverable(file, context)
        true
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        false
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}

/**
 * Читает файл с указанным именем по указанному пути и кладёт данные в модель WifiData
 * Возвращает true в случае успешного чтения
 * В случае успеха поля модели WifiData будут содержать прочитанные данные
 * В случае ошибки поля модели WifiData будут содержать null
 */
fun WifiData.readTo(folderPath: String, fileName: String): Boolean {
    var fileReader: FileReader? = null
    return try {
        fileReader = FileReader(File(File(folderPath), fileName))
        this.setData(Gson().fromJson<WifiData>(fileReader, WifiData::class.java))
        true
    } catch (e: Exception) {
        this.setData(WifiData())
        false
    } finally {
        fileReader?.close()
    }
}

/**
 * Для фикса бага, чтобы в наутилусе появлялась папка сразу после записи файла
 */
private fun makeFileDiscoverable(file: File, context: Context) {
    MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
}