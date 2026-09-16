package ru.abg.sstuschedule.data

import android.content.Context
import ru.abg.sstuschedule.model.WeekSchedule
import java.io.File

/**
 * Локальное хранилище JSON расписания на часах.
 */
object ScheduleStorage {

    private const val FILE_NAME = "schedule.json"

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun save(context: Context, json: String) {
        file(context).writeText(json)
    }

    fun load(context: Context): List<WeekSchedule>? {
        val f = file(context)
        if (!f.exists()) return null
        return ScheduleJson.fromJson(f.readText())
    }

    fun exists(context: Context): Boolean = file(context).exists()

    fun clear(context: Context) {
        file(context).delete()
    }
}