package ru.abg.sstuschedule.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import ru.abg.sstuschedule.R
import ru.abg.sstuschedule.data.ScheduleStorage
import ru.abg.sstuschedule.model.DaySchedule
import ru.abg.sstuschedule.model.Lesson
import ru.abg.sstuschedule.model.WeekSchedule
import java.util.Calendar
import java.util.Locale

@Composable
fun ScheduleScreen() {
    val context = LocalContext.current
    var weeks by remember { mutableStateOf<List<WeekSchedule>>(emptyList()) }
    var selectedDay by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        weeks = ScheduleStorage.load(context) ?: emptyList()
        val idx = findTodayIndex(weeks)
        if (idx >= 0) selectedDay = idx
        loading = false
    }

    val allDays = remember(weeks) { weeks.flatMap { it.days } }
    val listState = rememberScalingLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = 24.dp,
                bottom = 24.dp,
                start = 8.dp,
                end = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = stringResource(R.string.title_schedule),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            when {
                loading -> {
                    item {
                        Text(
                            text = stringResource(R.string.state_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                allDays.isEmpty() -> {
                    item {
                        Text(
                            text = stringResource(R.string.state_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.state_open_phone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    item {
                        DaySelector(
                            days = allDays,
                            selectedIndex = selectedDay,
                            onSelect = { selectedDay = it }
                        )
                    }

                    val day = allDays.getOrNull(selectedDay)
                    if (day == null || day.lessons.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.state_empty_day),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(day.lessons) { lesson ->
                            LessonCard(lesson)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Выбор дня
// ============================================================

@Composable
private fun DaySelector(
    days: List<DaySchedule>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        itemsIndexed(days) { index, day ->
            DayChip(
                day = day,
                isSelected = index == selectedIndex,
                isToday = isToday(day),
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun DayChip(
    day: DaySchedule,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = weekdayShort(day.weekday),
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
            Text(
                text = day.date,
                style = MaterialTheme.typography.labelSmall,
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
private fun LessonCard(lesson: Lesson) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Колонка времени — шире и с меньшим шрифтом
            Column(modifier = Modifier.width(48.dp)) {
                Text(
                    text = lesson.start,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = lesson.end,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            Spacer(Modifier.width(4.dp))

            // Акцентная полоска-разделитель
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.width(6.dp))

            // Предмет + аудитория
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (lesson.room.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.room_prefix, lesson.room),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============================================================
// Утилиты
// ============================================================

private fun findTodayIndex(weeks: List<WeekSchedule>): Int {
    if (weeks.isEmpty()) return -1
    val cal = Calendar.getInstance()
    val today = "%02d.%02d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1
    )
    var index = 0
    for (week in weeks) {
        for (day in week.days) {
            if (day.date == today) return index
            index++
        }
    }
    return -1
}

private fun isToday(day: DaySchedule): Boolean {
    val cal = Calendar.getInstance()
    val today = "%02d.%02d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1
    )
    return day.date == today
}

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