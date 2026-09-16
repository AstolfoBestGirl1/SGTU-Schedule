package ru.abg.sstuschedule.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import ru.abg.sstuschedule.alarm.AlarmReceiver
import ru.abg.sstuschedule.model.Lesson
import ru.abg.sstuschedule.model.WeekSchedule
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE = 1001
    private const val TIME_TOLERANCE_MS = 60_000L

    fun scheduleNextAlarm(context: Context, weeks: List<WeekSchedule>): Boolean {
        val settings = SettingsRepository(context)
        if (!settings.isAlarmEnabled()) {
            Log.d(TAG, "Будильник выключен")
            return false
        }

        val now = System.currentTimeMillis()

        val next = findNextLesson(weeks, now)
        if (next == null) {
            Log.d(TAG, "Нет предстоящих пар")
            return false
        }

        val (lessonTimeMs, lesson) = next
        val offsetMs = settings.getAlarmOffsetMinutes() * 60_000L
        val alarmTimeMs = (lessonTimeMs - offsetMs).coerceAtLeast(now + 60_000L)

        if (isSystemAlarmSet(context, alarmTimeMs)) {
            Log.d(TAG, "Системный будильник уже установлен, не трогаем")
            return true
        }

        dismissCurrentAlarm(context)
        return setAlarmClock(context, alarmTimeMs, lesson, settings)
    }

    private fun setAlarmClock(
        context: Context,
        triggerTimeMs: Long,
        lesson: Lesson,
        settings: SettingsRepository
    ): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Нет разрешения SCHEDULE_EXACT_ALARM")
                    openExactAlarmSettings(context)
                    return false
                }
            }

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_SUBJECT, lesson.subject)
                putExtra(AlarmReceiver.EXTRA_OFFSET, settings.getAlarmOffsetMinutes())
            }
            val alarmPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE + 1,
                Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val info = AlarmManager.AlarmClockInfo(triggerTimeMs, showIntent)
            alarmManager.setAlarmClock(info, alarmPendingIntent)

            val cal = Calendar.getInstance().apply { timeInMillis = triggerTimeMs }
            settings.setCurrentAlarm(
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                triggerTimeMs,
                lessonStartMs = triggerTimeMs + settings.getAlarmOffsetMinutes() * 60_000L
            )

            Log.d(TAG, "Будильник установлен на %02d:%02d — «%s»"
                .format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), lesson.subject))
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException — нет разрешения", e)
            openExactAlarmSettings(context)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка установки будильника", e)
            false
        }
    }

    private fun openExactAlarmSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось открыть настройки", e)
        }
    }

    fun dismissCurrentAlarm(context: Context) {
        val settings = SettingsRepository(context)
        if (!settings.hasActiveAlarm()) return

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Будильник отменён")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отмены будильника", e)
        }
        settings.clearCurrentAlarm()
    }

    fun dismissIfLessonStarted(context: Context): Boolean {
        val settings = SettingsRepository(context)
        if (!settings.hasActiveAlarm()) return false

        val lessonStartMs = settings.getCurrentLessonStartMs()
        if (lessonStartMs <= 0L) return false

        if (System.currentTimeMillis() >= lessonStartMs) {
            Log.d(TAG, "Пара началась, удаляем будильник")
            dismissCurrentAlarm(context)
            return true
        }
        return false
    }

    private fun isSystemAlarmSet(context: Context, expectedTimeMs: Long): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextAlarm = alarmManager.nextAlarmClock ?: return false
            val diff = kotlin.math.abs(nextAlarm.triggerTime - expectedTimeMs)
            Log.d(TAG, "Системный будильник через $diff мс от ожидаемого")
            diff <= TIME_TOLERANCE_MS
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения системных будильников", e)
            false
        }
    }

    private fun findNextLesson(
        weeks: List<WeekSchedule>,
        nowMs: Long
    ): Pair<Long, Lesson>? {
        val candidates = mutableListOf<Pair<Long, Lesson>>()
        for (week in weeks) {
            for (day in week.days) {
                val dayBase = parseDay(day.date, nowMs) ?: continue
                for (lesson in day.lessons) {
                    val (h, m) = parseTime(lesson.start) ?: continue
                    val cal = dayBase.clone() as Calendar
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, m)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    if (cal.timeInMillis > nowMs) {
                        candidates.add(cal.timeInMillis to lesson)
                    }
                }
            }
        }
        return candidates.minByOrNull { it.first }
    }

    private fun parseDay(ddmm: String, nowMs: Long): Calendar? {
        val parts = ddmm.split(".")
        if (parts.size != 2) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull()?.minus(1) ?: return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.MONTH, month)
        }
        if (cal.before(now) && !isSameDay(cal, now)) {
            val todayDoy = now.get(Calendar.DAY_OF_YEAR)
            val calDoy = cal.get(Calendar.DAY_OF_YEAR)
            if (calDoy < todayDoy) cal.add(Calendar.YEAR, 1)
        }
        return cal
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun parseTime(hhmm: String): Pair<Int, Int>? {
        val parts = hhmm.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h to m
    }
}