package ru.abg.sstuschedule.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.abg.sstuschedule.data.AlarmScheduler

/**
 * Проверяет каждые 15 минут: если пара, на которую стоял будильник,
 * уже началась — удаляет будильник.
 */
class AlarmCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AlarmCleanupWorker"
        const val UNIQUE_NAME = "alarm_cleanup_periodic"
    }

    override suspend fun doWork(): Result {
        val dismissed = AlarmScheduler.dismissIfLessonStarted(applicationContext)
        if (dismissed) Log.d(TAG, "Будильник удалён")
        return Result.success()
    }
}