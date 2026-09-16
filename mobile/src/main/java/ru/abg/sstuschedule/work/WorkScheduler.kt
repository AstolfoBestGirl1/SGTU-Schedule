package ru.abg.sstuschedule.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Планировщик фоновых задач:
 * - ежедневное обновление расписания в 22:00;
 * - периодическая проверка и удаление отыгравших будильников.
 */
object WorkScheduler {

    private const val TAG = "WorkScheduler"

    private const val RETRY_DELAY_MINUTES = 5L

    /**
     * Планирует первую задачу на ближайшие 22:00, если ещё не запущено.
     */
    fun scheduleAll(context: Context) {
        scheduleNextDailyUpdate(context)
        scheduleAlarmCleanup(context)
    }

    /**
     * Ставит OneTimeWorkRequest на следующие 22:00.
     * После выполнения воркер сам вызовет этот метод снова.
     */
    fun scheduleNextDailyUpdate(context: Context) {
        val delayMs = millisUntilNext22()
        Log.d(TAG, "Следующее обновление через ${delayMs / 60000} мин")

        val request = OneTimeWorkRequestBuilder<ScheduleUpdateWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ScheduleUpdateWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Ретрай через 5 минут после неудачной попытки.
     */
    fun scheduleRetry(context: Context) {
        Log.d(TAG, "Повтор через $RETRY_DELAY_MINUTES мин")

        val request = OneTimeWorkRequestBuilder<ScheduleUpdateWorker>()
            .setInitialDelay(RETRY_DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ScheduleUpdateWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Периодическая проверка будильников — каждые 15 минут.
     */
    private fun scheduleAlarmCleanup(context: Context) {
        val request = PeriodicWorkRequestBuilder<AlarmCleanupWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AlarmCleanupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Миллисекунды до ближайших 22:00 (сегодня или завтра).
     */
    private fun millisUntilNext22(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}