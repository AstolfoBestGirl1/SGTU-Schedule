package ru.abg.sstuschedule.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class PdfDownloader {

    companion object {
        private const val TAG = "PdfDownloader"
    }

    suspend fun download(groupId: Int, targetFile: File): Boolean =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://rasp.sstu.ru/rasp/group/$groupId/pdf")
                Log.d(TAG, "Скачиваем: $url")

                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Android) AppleWebKit/537.36"
                )
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.connect()

                val code = connection.responseCode
                Log.d(TAG, "HTTP $code")

                if (code != 200) return@withContext false

                connection.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val header = ByteArray(4)
                targetFile.inputStream().use { it.read(header) }
                val isPdf = header[0] == 0x25.toByte() &&
                        header[1] == 0x50.toByte() &&
                        header[2] == 0x44.toByte() &&
                        header[3] == 0x46.toByte()

                Log.d(TAG, "Сохранено ${targetFile.length()} байт, isPdf=$isPdf")
                isPdf
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                false
            } finally {
                connection?.disconnect()
            }
        }
}