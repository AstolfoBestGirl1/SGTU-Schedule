package ru.abg.sstuschedule.data

import android.content.Context

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("sgtu_settings", Context.MODE_PRIVATE)

    fun getGroupId(): Int? {
        val id = prefs.getInt(KEY_GROUP_ID, -1)
        return if (id <= 0) null else id
    }

    fun getGroupName(): String? = prefs.getString(KEY_GROUP_NAME, null)

    fun setGroup(id: Int, name: String) {
        prefs.edit()
            .putInt(KEY_GROUP_ID, id)
            .putString(KEY_GROUP_NAME, name)
            .apply()
    }

    fun clearGroup() {
        prefs.edit().remove(KEY_GROUP_ID).remove(KEY_GROUP_NAME).apply()
    }

    // ============================================================
    // Будильник
    // ============================================================

    fun isAlarmEnabled(): Boolean = prefs.getBoolean(KEY_ALARM_ENABLED, false)

    fun setAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALARM_ENABLED, enabled).apply()
    }

    fun getAlarmOffsetMinutes(): Int = prefs.getInt(KEY_ALARM_OFFSET, 60)

    fun setAlarmOffsetMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_ALARM_OFFSET, minutes.coerceIn(5, 180)).apply()
    }

    // ============================================================
    // Текущий установленный будильник
    // ============================================================

    /**
     * Сохраняет информацию об установленном будильнике.
     * @param alarmTimeMs время срабатывания будильника (мс)
     * @param lessonStartMs время начала пары, на которую он поставлен (мс)
     */
    fun setCurrentAlarm(hour: Int, minute: Int, alarmTimeMs: Long, lessonStartMs: Long) {
        prefs.edit()
            .putInt(KEY_ALARM_HOUR, hour)
            .putInt(KEY_ALARM_MIN, minute)
            .putLong(KEY_ALARM_TIME_MS, alarmTimeMs)
            .putLong(KEY_LESSON_START_MS, lessonStartMs)
            .apply()
    }

    fun getCurrentAlarmHour(): Int = prefs.getInt(KEY_ALARM_HOUR, -1)
    fun getCurrentAlarmMinute(): Int = prefs.getInt(KEY_ALARM_MIN, -1)
    fun getCurrentAlarmTimeMs(): Long = prefs.getLong(KEY_ALARM_TIME_MS, -1L)
    fun getCurrentLessonStartMs(): Long = prefs.getLong(KEY_LESSON_START_MS, -1L)

    fun hasActiveAlarm(): Boolean = getCurrentAlarmTimeMs() > 0L

    fun clearCurrentAlarm() {
        prefs.edit()
            .remove(KEY_ALARM_HOUR)
            .remove(KEY_ALARM_MIN)
            .remove(KEY_ALARM_TIME_MS)
            .remove(KEY_LESSON_START_MS)
            .apply()
    }

    companion object {
        private const val KEY_GROUP_ID = "group_id"
        private const val KEY_GROUP_NAME = "group_name"
        private const val KEY_ALARM_ENABLED = "alarm_enabled"
        private const val KEY_ALARM_OFFSET = "alarm_offset_minutes"
        private const val KEY_ALARM_HOUR = "current_alarm_hour"
        private const val KEY_ALARM_MIN = "current_alarm_minute"
        private const val KEY_ALARM_TIME_MS = "current_alarm_time_ms"
        private const val KEY_LESSON_START_MS = "current_lesson_start_ms"
    }
}