package ru.abg.sstuschedule.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.abg.sstuschedule.data.ScheduleRepository

/**
 * Ежедневное обновление расписания в 22:00.
 * При ошибке возвращает Result.retry() — WorkManager перезапустит через 5 минут.
 */
class ScheduleUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ScheduleUpdateWorker"
        const val UNIQUE_NAME = "schedule_update_daily"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Запуск ежедневного обновления, attempt=$runAttemptCount")

        val repository = ScheduleRepository(applicationContext)
        val ok = repository.refresh()

        return if (ok) {
            Log.d(TAG, "Расписание обновлено успешно")
            WorkScheduler.scheduleNextDailyUpdate(applicationContext)
            Result.success()
        } else {
            Log.w(TAG, "Не удалось обновить, повтор через 5 минут")
            WorkScheduler.scheduleRetry(applicationContext)
            Result.success() // следующий запуск ставим сами, чтобы контролировать интервал
        }
    }
}