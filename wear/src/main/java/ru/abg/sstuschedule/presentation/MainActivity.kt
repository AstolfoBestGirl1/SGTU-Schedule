package ru.abg.sstuschedule.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import ru.abg.sstuschedule.presentation.theme.SGTUScheduleTheme
import ru.abg.sstuschedule.work.TileWorkScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Планируем ежедневное обновление плитки в 01:00
        TileWorkScheduler.scheduleNextDaily(applicationContext)

        setContent {
            SGTUScheduleTheme {
                ScheduleScreen()
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun SchedulePreview() {
    SGTUScheduleTheme {
        ScheduleScreen()
    }
}