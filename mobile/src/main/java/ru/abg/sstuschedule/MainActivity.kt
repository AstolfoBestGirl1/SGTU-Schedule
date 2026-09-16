package ru.abg.sstuschedule

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import ru.abg.sstuschedule.data.GroupsRepository
import ru.abg.sstuschedule.data.ScheduleRepository
import ru.abg.sstuschedule.data.SettingsRepository
import ru.abg.sstuschedule.ui.GroupSelectionScreen
import ru.abg.sstuschedule.ui.ScheduleScreen
import ru.abg.sstuschedule.ui.theme.SGTUScheduleTheme
import ru.abg.sstuschedule.work.WorkScheduler

class MainActivity : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()

        // Запрашиваем разрешения
        requestPermissionsIfNeeded()

        WorkScheduler.scheduleAll(applicationContext)

        val settings = SettingsRepository(applicationContext)
        val scheduleRepository = ScheduleRepository(applicationContext)
        val groupsRepository = GroupsRepository(applicationContext)

        setContent {
            SGTUScheduleTheme {
                AppRoot(
                    settings = settings,
                    scheduleRepository = scheduleRepository,
                    groupsRepository = groupsRepository
                )
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        // Уведомления (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Точные будильники (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}

@Composable
fun AppRoot(
    settings: SettingsRepository,
    scheduleRepository: ScheduleRepository,
    groupsRepository: GroupsRepository
) {
    var groupId by remember { mutableStateOf(settings.getGroupId()) }

    if (groupId == null) {
        GroupSelectionScreen(groupsRepository) { group ->
            settings.setGroup(group.id, group.name)
            scheduleRepository.clearCache()
            groupId = group.id
        }
    } else {
        ScheduleScreen(
            repository = scheduleRepository,
            settings = settings,
            groupName = settings.getGroupName() ?: "",
            onChangeGroup = {
                settings.clearGroup()
                scheduleRepository.clearCache()
                groupId = null
            }
        )
    }
}