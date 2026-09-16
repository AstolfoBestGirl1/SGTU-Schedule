package ru.abg.sstuschedule.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import ru.abg.sstuschedule.model.WeekSchedule

object ScheduleJson {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun toJson(weeks: List<WeekSchedule>): String {
        return gson.toJson(weeks)
    }

    fun fromJson(json: String): List<WeekSchedule>? {
        return try {
            val type = object : TypeToken<List<WeekSchedule>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
}