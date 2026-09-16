package ru.abg.sstuschedule.service

import android.util.Log
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import ru.abg.sstuschedule.data.ScheduleStorage
import ru.abg.sstuschedule.tile.MainTileService

class DataLayerListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "DataLayerListener"
        private const val PATH_SCHEDULE = "/schedule"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEvents.count} событий")

        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue

            val path = event.dataItem.uri.path
            Log.d(TAG, "Событие: path=$path")

            if (path != PATH_SCHEDULE) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val json = dataMap.getString("json")
            if (json.isNullOrEmpty()) {
                Log.w(TAG, "Пустой JSON")
                continue
            }

            ScheduleStorage.save(applicationContext, json)
            Log.d(TAG, "Расписание сохранено локально (${json.length} байт)")

            requestTileUpdate()
        }
    }

    private fun requestTileUpdate() {
        try {
            TileService.getUpdater(applicationContext)
                .requestUpdate(MainTileService::class.java)
            Log.d(TAG, "Запрошено обновление плитки")
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось запросить обновление плитки", e)
        }
    }
}