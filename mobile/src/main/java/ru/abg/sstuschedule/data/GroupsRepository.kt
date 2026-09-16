package ru.abg.sstuschedule.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.abg.sstuschedule.model.GroupInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class GroupsRepository(private val context: Context) {

    private val cacheFile: File get() = File(context.filesDir, "groups.html")

    /**
     * Возвращает список групп. Если есть кэш — парсит его.
     * Иначе скачивает HTML и парсит.
     */
    suspend fun getGroups(forceRefresh: Boolean = false): List<GroupInfo> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && cacheFile.exists()) {
                val html = cacheFile.readText()
                val cached = GroupsParser.parse(html)
                if (cached.isNotEmpty()) {
                    Log.d("GroupsRepository", "Используем кэш: ${cached.size} групп")
                    return@withContext cached
                }
            }

            val html = downloadHtml() ?: return@withContext emptyList()
            cacheFile.writeText(html)
            GroupsParser.parse(html)
        }

    private fun downloadHtml(): String? {
        return try {
            val url = URL("https://rasp.sstu.ru/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Android) AppleWebKit/537.36"
            )
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.connect()

            if (conn.responseCode != 200) {
                Log.e("GroupsRepository", "HTTP ${conn.responseCode}")
                conn.disconnect()
                return null
            }

            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()
            text
        } catch (e: Exception) {
            Log.e("GroupsRepository", "Ошибка скачивания", e)
            null
        }
    }
}