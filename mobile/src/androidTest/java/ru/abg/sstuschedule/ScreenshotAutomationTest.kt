package ru.abg.sstuschedule

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.abg.sstuschedule.data.GroupsRepository
import ru.abg.sstuschedule.data.ScheduleRepository
import ru.abg.sstuschedule.data.SettingsRepository
import ru.abg.sstuschedule.ui.ScheduleScreen
import ru.abg.sstuschedule.ui.theme.SGTUScheduleTheme
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScreenshotAutomationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun createAndSaveLatestVersionScreenshot() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(appContext)
        val groupsRepository = GroupsRepository(appContext)
        val scheduleRepository = ScheduleRepository(appContext)
        val settingsRepository = SettingsRepository(appContext)

        runBlocking {
            // 1. Получаем список групп и берем самую первую
            val groups = groupsRepository.getGroups(forceRefresh = false)
            if (groups.isEmpty()) {
                Log.e("ScreenshotTest", "Список групп пуст")
                return@runBlocking
            }
            val firstGroup = groups.first()
            settingsRepository.setGroup(firstGroup.id, firstGroup.name)

            // 2. Скачиваем и обновляем расписание для этой группы
            scheduleRepository.refresh()
            val weeks = scheduleRepository.loadCached() ?: emptyList()

            // 3. Ищем день с самым большим количеством пар
            var targetWeekIndex = 0
            var targetDayIndex = 0
            var maxLessonsCount = -1
            var targetDay: ru.abg.sstuschedule.model.DaySchedule? = null

            for ((wIdx, week) in weeks.withIndex()) {
                for ((dIdx, day) in week.days.withIndex()) {
                    if (day.lessons.size > maxLessonsCount) {
                        maxLessonsCount = day.lessons.size
                        targetWeekIndex = wIdx
                        targetDayIndex = dIdx
                        targetDay = day
                    }
                }
            }

            if (targetDay == null || targetDay.lessons.size < 2) {
                Log.e("ScreenshotTest", "Недостаточно пар для скриншота второй существующей пары")
                return@runBlocking
            }

            // 4. Берем именно вторую существующую пару (индекс 1 в списке существующих пар)
            val secondLesson = targetDay.lessons[1]
            
            // Вычисляем середину этой пары
            val startParts = secondLesson.start.split(":")
            val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
            
            val endParts = secondLesson.end.split(":")
            val endMin = endParts[0].toInt() * 60 + endParts[1].toInt()
            
            val middleMin = startMin + (endMin - startMin) / 2

            // Устанавливаем тестовые оверрайды времени и даты
            ru.abg.sstuschedule.ui.previewMinutesOverride = middleMin
            ru.abg.sstuschedule.ui.previewDateOverride = targetDay.date

            // 5. Отображаем экран расписания с нужным состоянием
            composeTestRule.setContent {
                SGTUScheduleTheme(darkTheme = true, dynamicColor = false) {
                    ScheduleScreen(
                        repository = scheduleRepository,
                        settings = settingsRepository,
                        groupName = firstGroup.name,
                        onChangeGroup = {}
                    )
                }
            }

            // Ждем завершения отрисовки и эффектов
            composeTestRule.waitForIdle()

            // 6. Делаем скриншот и сохраняем его в файлы на устройстве
            val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
            
            val internalFile = File(appContext.cacheDir, "latest_version_screenshot.png")
            internalFile.outputStream().use { 
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) 
            }
            Log.d("ScreenshotTest", "Скриншот сохранен во внутренний кэш: ${internalFile.absolutePath}")

            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val externalFile = File(downloadsDir, "latest_version_screenshot.png")
                externalFile.outputStream().use { 
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) 
                }
                Log.d("ScreenshotTest", "Скриншот сохранен в Downloads: ${externalFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("ScreenshotTest", "Не удалось сохранить во внешнее хранилище", e)
            }
        }
    }
}
