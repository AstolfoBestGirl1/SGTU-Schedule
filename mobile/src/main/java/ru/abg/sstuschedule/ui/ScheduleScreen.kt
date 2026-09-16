package ru.abg.sstuschedule.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.abg.sstuschedule.R
import ru.abg.sstuschedule.data.AlarmScheduler
import ru.abg.sstuschedule.data.ScheduleRepository
import ru.abg.sstuschedule.data.SettingsRepository
import ru.abg.sstuschedule.model.DaySchedule
import ru.abg.sstuschedule.model.Lesson
import ru.abg.sstuschedule.model.WeekSchedule
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    repository: ScheduleRepository,
    settings: SettingsRepository,
    groupName: String,
    onChangeGroup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Читаем строку один раз в composable-контексте,
    // чтобы использовать её внутри suspend-блоков.
    val errorMessage = stringResource(R.string.state_error)

    var weeks by remember { mutableStateOf<List<WeekSchedule>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var alarmEnabled by remember { mutableStateOf(settings.isAlarmEnabled()) }

    var selectedWeek by remember { mutableStateOf(0) }
    var selectedDayInWeek by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        loading = true
        val cached = repository.loadCached()
        if (cached.isNullOrEmpty()) {
            val ok = repository.refresh()
            if (ok) {
                weeks = repository.loadCached() ?: emptyList()
            } else {
                error = errorMessage
            }
        } else {
            weeks = cached
        }
        val today = findTodayLocation(weeks)
        if (today != null) {
            selectedWeek = today.first
            selectedDayInWeek = today.second
        }
        if (weeks.isNotEmpty() && settings.isAlarmEnabled()) {
            AlarmScheduler.scheduleNextAlarm(context, weeks)
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.title_schedule),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = groupName.ifEmpty { stringResource(R.string.title_select_group) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onChangeGroup)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAlarmDialog = true }) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = stringResource(R.string.action_alarm),
                            tint = if (alarmEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        enabled = !refreshing,
                        onClick = {
                            refreshing = true
                            scope.launch {
                                val ok = repository.refresh()
                                if (ok) {
                                    weeks = repository.loadCached() ?: weeks
                                    val today = findTodayLocation(weeks)
                                    if (today != null) {
                                        selectedWeek = today.first
                                        selectedDayInWeek = today.second
                                    }
                                }
                                refreshing = false
                            }
                        }
                    ) {
                        if (refreshing) {
                            val infiniteTransition = rememberInfiniteTransition(label = "spin")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 900, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "angle"
                            )
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_refreshing),
                                modifier = Modifier.rotate(angle)
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_refresh)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(error!!) {
                    scope.launch {
                        loading = true
                        val ok = repository.refresh()
                        if (ok) {
                            weeks = repository.loadCached() ?: emptyList()
                            error = null
                        } else {
                            error = errorMessage
                        }
                        loading = false
                    }
                }
                weeks.isEmpty() -> EmptyState()
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        WeekTabs(
                            weeks = weeks,
                            selectedWeek = selectedWeek,
                            selectedDayInWeek = selectedDayInWeek,
                            onSelect = { w, d ->
                                selectedWeek = w
                                selectedDayInWeek = d
                            }
                        )

                        val currentWeek = weeks.getOrNull(selectedWeek)
                        val currentDay = currentWeek?.days?.getOrNull(selectedDayInWeek)

                        if (currentDay == null || currentDay.lessons.isEmpty()) {
                            EmptyDay()
                        } else {
                            val isToday = isToday(currentDay)
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(currentDay.lessons) { idx, lesson ->
                                    LessonCard(
                                        lesson = lesson,
                                        isCurrent = isToday && isCurrentLesson(lesson),
                                        isNext = isToday && isNextLesson(currentDay.lessons, idx)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAlarmDialog) {
        AlarmSettingsDialog(
            settings = settings,
            onDismiss = { showAlarmDialog = false },
            onSave = { enabled, offset ->
                settings.setAlarmEnabled(enabled)
                settings.setAlarmOffsetMinutes(offset)
                alarmEnabled = enabled
                if (enabled && weeks.isNotEmpty()) {
                    AlarmScheduler.scheduleNextAlarm(context, weeks)
                }
                showAlarmDialog = false
            }
        )
    }
}

// ============================================================
// Диалог настроек будильника
// ============================================================

@Composable
private fun AlarmSettingsDialog(
    settings: SettingsRepository,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, offsetMinutes: Int) -> Unit
) {
    var enabled by remember { mutableStateOf(settings.isAlarmEnabled()) }
    var offsetText by remember { mutableStateOf(settings.getAlarmOffsetMinutes().toString()) }

    val parsedOffset = offsetText.toIntOrNull()
    val isValid = parsedOffset != null && parsedOffset in 5..180

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alarm_dialog_title)) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.alarm_dialog_enable),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = offsetText,
                    onValueChange = { new ->
                        offsetText = new.filter { it.isDigit() }.take(3)
                    },
                    label = { Text(stringResource(R.string.alarm_dialog_minutes_label)) },
                    singleLine = true,
                    isError = offsetText.isNotEmpty() && !isValid,
                    supportingText = {
                        Text(
                            if (offsetText.isEmpty() || isValid) {
                                stringResource(R.string.alarm_dialog_hint)
                            } else {
                                stringResource(R.string.alarm_dialog_error)
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val offset = parsedOffset ?: 60
                    onSave(enabled, offset.coerceIn(5, 180))
                }
            ) {
                Text(stringResource(R.string.alarm_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.alarm_dialog_cancel))
            }
        }
    )
}

// ============================================================
// Табы: 2 ряда дней
// ============================================================

@Composable
private fun WeekTabs(
    weeks: List<WeekSchedule>,
    selectedWeek: Int,
    selectedDayInWeek: Int,
    onSelect: (week: Int, dayInWeek: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weeks.forEachIndexed { wIdx, week ->
            WeekRow(
                week = week,
                isSelectedWeek = wIdx == selectedWeek,
                selectedDayInWeek = if (wIdx == selectedWeek) selectedDayInWeek else -1,
                onSelect = { dIdx -> onSelect(wIdx, dIdx) }
            )
        }
    }
}

@Composable
private fun WeekRow(
    week: WeekSchedule,
    isSelectedWeek: Boolean,
    selectedDayInWeek: Int,
    onSelect: (dayInWeek: Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val availableWidth = maxWidth
        val gap: Dp = 6.dp
        val count = week.days.size.coerceAtLeast(1)
        val gapsTotal = gap * (count - 1)

        val minChipWidth: Dp = 60.dp
        val minChipHeight: Dp = 42.dp
        val maxChipHeight: Dp = 58.dp

        val calculatedWidth = (availableWidth - gapsTotal) / count
        val chipWidth = maxOf(minChipWidth, calculatedWidth)

        val chipHeight = chipWidth.coerceIn(minChipHeight, maxChipHeight)

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            itemsIndexed(week.days) { dIdx, day ->
                DayChip(
                    day = day,
                    isSelected = isSelectedWeek && dIdx == selectedDayInWeek,
                    isToday = isToday(day),
                    width = chipWidth,
                    height = chipHeight,
                    onClick = { onSelect(dIdx) }
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    day: DaySchedule,
    isSelected: Boolean,
    isToday: Boolean,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier
            .width(width)
            .height(height)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weekdayShort(day.weekday),
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = day.date,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

// ============================================================
// Карточка пары
// ============================================================

@Composable
private fun LessonCard(
    lesson: Lesson,
    isCurrent: Boolean,
    isNext: Boolean
) {
    val border = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isNext -> MaterialTheme.colorScheme.tertiary
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrent) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.width(64.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = lesson.start,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = lesson.end,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.height(6.dp))
                TypeBadge(
                    type = lesson.type,
                    isHighlighted = isCurrent
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                .width(3.dp)
                .height(72.dp)
                .clip(CircleShape)
                .background(border)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                if (lesson.room.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.room_prefix, lesson.room),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
                if (lesson.teacher.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = lesson.teacher,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (!lesson.teacher2.isNullOrEmpty()) {
                    Text(
                        text = lesson.teacher2,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: String, isHighlighted: Boolean = false) {
    if (type.isEmpty()) return

    val labelRes: Int? = when (type.lowercase(Locale.ROOT)) {
        "лекц" -> R.string.type_lecture
        "прак" -> R.string.type_practice
        "лаб" -> R.string.type_lab
        else -> null
    }
    val color = when (type.lowercase(Locale.ROOT)) {
        "лекц" -> MaterialTheme.colorScheme.primary
        "прак" -> MaterialTheme.colorScheme.tertiary
        "лаб" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    val onColor = when (type.lowercase(Locale.ROOT)) {
        "лекц" -> MaterialTheme.colorScheme.onPrimary
        "прак" -> MaterialTheme.colorScheme.onTertiary
        "лаб" -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val label = labelRes?.let { stringResource(it) } ?: type

    val backgroundColor = if (isHighlighted) color else color.copy(alpha = 0.15f)
    val textColor = if (isHighlighted) onColor else color

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ============================================================
// Состояния
// ============================================================

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    stringResource(R.string.action_retry),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.state_empty),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EmptyDay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.state_empty_day),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// Утилиты
// ============================================================

@Composable
private fun weekdayShort(full: String): String = when (full.lowercase(Locale.ROOT)) {
    "понедельник" -> stringResource(R.string.weekday_mon_short)
    "вторник" -> stringResource(R.string.weekday_tue_short)
    "среда" -> stringResource(R.string.weekday_wed_short)
    "четверг" -> stringResource(R.string.weekday_thu_short)
    "пятница" -> stringResource(R.string.weekday_fri_short)
    "суббота" -> stringResource(R.string.weekday_sat_short)
    "воскресенье" -> stringResource(R.string.weekday_sun_short)
    else -> full.take(2)
}

private fun findTodayLocation(weeks: List<WeekSchedule>): Pair<Int, Int>? {
    val cal = Calendar.getInstance()
    val today = "%02d.%02d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1
    )
    weeks.forEachIndexed { wIdx, week ->
        week.days.forEachIndexed { dIdx, day ->
            if (day.date == today) return wIdx to dIdx
        }
    }
    return null
}

private fun isToday(day: DaySchedule): Boolean {
    val cal = Calendar.getInstance()
    val today = "%02d.%02d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1
    )
    return day.date == today
}

private fun currentMinutes(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

private fun parseMinutes(hhmm: String): Int {
    val parts = hhmm.split(":")
    if (parts.size != 2) return -1
    return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
}

private fun isCurrentLesson(lesson: Lesson): Boolean {
    val now = currentMinutes()
    val start = parseMinutes(lesson.start)
    val end = parseMinutes(lesson.end)
    return now in start..end
}

private fun isNextLesson(lessons: List<Lesson>, idx: Int): Boolean {
    val now = currentMinutes()
    for (i in idx until lessons.size) {
        val start = parseMinutes(lessons[i].start)
        if (start > now) return i == idx
    }
    return false
}