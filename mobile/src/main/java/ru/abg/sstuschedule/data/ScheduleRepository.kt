package ru.abg.sstuschedule.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.abg.sstuschedule.model.WeekSchedule
import java.io.File

class ScheduleRepository(private val context: Context) {

    companion object {
        private const val TAG = "ScheduleRepository"
        private const val JSON_FILE = "schedule.json"
        private const val PDF_FILE = "schedule.pdf"
    }

    private val downloader = PdfDownloader()
    private val parser = PdfScheduleParser()
    private val settings = SettingsRepository(context)

    private val pdfFile: File get() = File(context.filesDir, PDF_FILE)
    private val jsonFile: File get() = File(context.filesDir, JSON_FILE)

    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val groupId = settings.getGroupId()
        if (groupId == null) {
            Log.e(TAG, "Группа не выбрана")
            return@withContext false
        }

        Log.d(TAG, "Обновляем группу $groupId")

        if (!downloader.download(groupId, pdfFile)) {
            Log.e(TAG, "Не удалось скачать PDF")
            return@withContext false
        }

        val weeks = parser.parse(pdfFile)
        if (weeks == null || weeks.isEmpty()) {
            Log.e(TAG, "Не удалось распарсить PDF")
            return@withContext false
        }

        val json = ScheduleJson.toJson(weeks)
        jsonFile.writeText(json)
        Log.d(TAG, "JSON сохранён: ${jsonFile.length()} байт, недель: ${weeks.size}")

        Log.d(TAG, "Отправляем JSON на часы...")
        val syncOk = WearSyncManager.sendSchedule(context, json)
        AlarmScheduler.scheduleNextAlarm(context, weeks)
        Log.d(TAG, "Результат синхронизации: $syncOk")

        true
    }

    fun loadCached(): List<WeekSchedule>? {
        if (!jsonFile.exists()) return null
        return ScheduleJson.fromJson(jsonFile.readText())
    }

    fun hasCached(): Boolean = jsonFile.exists()

    fun clearCache() {
        jsonFile.delete()
        pdfFile.delete()
    }
}