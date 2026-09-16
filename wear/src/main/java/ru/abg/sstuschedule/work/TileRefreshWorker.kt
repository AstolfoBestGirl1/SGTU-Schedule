package ru.abg.sstuschedule.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.wear.tiles.TileService
import ru.abg.sstuschedule.data.ScheduleStorage
import ru.abg.sstuschedule.tile.MainTileService

/**
 * Ежедневное обновление плитки в 01:00.
 * Если данных нет — retry через 5 минут.
 */
class TileRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TileRefreshWorker"
        const val UNIQUE_NAME = "tile_refresh_daily"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Обновляем плитку, attempt=$runAttemptCount")

        // Проверяем, есть ли данные расписания
        val weeks = ScheduleStorage.load(applicationContext)
        if (weeks.isNullOrEmpty()) {
            Log.w(TAG, "Данных нет, повтор через 5 минут")
            TileWorkScheduler.scheduleRetry(applicationContext)
            return Result.success()
        }

        return try {
            TileService.getUpdater(applicationContext)
                .requestUpdate(MainTileService::class.java)
            Log.d(TAG, "Запрос на обновление плитки отправлен")
            TileWorkScheduler.scheduleNextDaily(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления плитки", e)
            TileWorkScheduler.scheduleRetry(applicationContext)
            Result.success()
        }
    }
}