package ru.abg.sstuschedule.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Отправляет JSON расписания на подключённые часы через Wearable Data Layer API.
 */
object WearSyncManager {

    private const val TAG = "WearSyncManager"
    private const val PATH_SCHEDULE = "/schedule"

    suspend fun sendSchedule(context: Context, json: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начинаем отправку JSON (${json.length} байт) на часы...")

                val request = PutDataMapRequest.create(PATH_SCHEDULE).apply {
                    dataMap.putString("json", json)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

                Tasks.await(Wearable.getDataClient(context).putDataItem(request))
                Log.d(TAG, "JSON успешно отправлен на часы (${json.length} байт)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отправки на часы: ${e.message}", e)
                false
            }
        }
}