package ru.abg.sstuschedule.work

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TileWorkScheduler {

    private const val TAG = "TileWorkScheduler"
    private const val RETRY_DELAY_MINUTES = 5L

    fun scheduleNextDaily(context: Context) {
        val delayMs = millisUntilNext1()
        Log.d(TAG, "Следующее обновление плитки через ${delayMs / 60000} мин")

        val request = OneTimeWorkRequestBuilder<TileRefreshWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TileRefreshWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleRetry(context: Context) {
        Log.d(TAG, "Повтор обновления плитки через $RETRY_DELAY_MINUTES мин")

        val request = OneTimeWorkRequestBuilder<TileRefreshWorker>()
            .setInitialDelay(RETRY_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TileRefreshWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun millisUntilNext1(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}