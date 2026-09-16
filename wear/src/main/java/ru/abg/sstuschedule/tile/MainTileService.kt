package ru.abg.sstuschedule.tile

import android.content.Context
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.layout.basicText
import androidx.wear.protolayout.layout.fontStyle
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import ru.abg.sstuschedule.R
import ru.abg.sstuschedule.data.ScheduleStorage
import ru.abg.sstuschedule.model.DaySchedule
import ru.abg.sstuschedule.model.Lesson
import ru.abg.sstuschedule.model.WeekSchedule
import java.util.Calendar
import java.util.Locale

class MainTileService : Material3TileService() {

    companion object {
        // Отступы по бокам. primaryLayout больше не добавляет свои,
        // поэтому крайнему ряду нужно много, средним — почти ноль.
        private const val SIDE_PADDING_EDGE = 30f
        private const val SIDE_PADDING_MIDDLE = 2f

        private const val TIME_COLUMN_WIDTH = 44f
    }

    override suspend fun MaterialScope.tileResponse(
        requestParams: RequestBuilders.TileRequest
    ): Tile {
        val context = applicationContext
        val weeks = ScheduleStorage.load(context)
        val today = findToday(weeks)

        // Сами строим корневой Column. Никакого primaryLayout —
        // значит, никаких скрытых паддингов.
        val root = Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(
                        Padding.Builder()
                            .setTop(dp(18f))
                            .setBottom(dp(18f))
                            .build()
                    )
                    .build()
            )
            .apply {
                // Заголовок
                addContent(
                    paddedRow(
                        sidePadding = 24f,
                        content = basicText(
                            text = headerText(today, context).layoutString,
                            fontStyle = fontStyle(
                                size = 11f,
                                color = colorScheme.onSurface
                            )
                        )
                    )
                )
                addContent(vSpacer(dp(4f)))

                when {
                    weeks == null -> {
                        addContent(
                            paddedRow(
                                sidePadding = SIDE_PADDING_EDGE,
                                content = basicText(
                                    text = context.getString(R.string.state_empty).layoutString,
                                    fontStyle = fontStyle(
                                        size = 11f,
                                        color = colorScheme.onSurface
                                    )
                                )
                            )
                        )
                    }

                    today == null || today.lessons.isEmpty() -> {
                        addContent(
                            paddedRow(
                                sidePadding = SIDE_PADDING_EDGE,
                                content = basicText(
                                    text = context.getString(R.string.state_empty_day).layoutString,
                                    fontStyle = fontStyle(
                                        size = 11f,
                                        color = colorScheme.onSurface
                                    )
                                )
                            )
                        )
                    }

                    else -> {
                        val lessons = today.lessons
                        val nowMin = currentMinutes()
                        val lastIdx = lessons.size - 1

                        for ((index, lesson) in lessons.withIndex()) {
                            val isCurrent = isCurrent(lesson, nowMin)
                            val isEdge = index == 0 || index == lastIdx
                            val sidePadding = if (isEdge) SIDE_PADDING_EDGE else SIDE_PADDING_MIDDLE

                            addContent(
                                buildLessonCard(context, lesson, isCurrent, sidePadding)
                            )

                            if (index < lastIdx) {
                                addContent(vSpacer(dp(4f)))
                            }
                        }
                    }
                }
            }
            .build()

        // Timestamp в версии ресурсов — чтобы система поняла,
        // что контент изменился, и перерисовала плитку.
        return Tile.Builder()
            .setResourcesVersion(System.currentTimeMillis().toString())
            .setTileTimeline(Timeline.fromLayoutElement(root))
            .build()
    }

    // ============================================================
    // Карточка пары
    // ============================================================

    private fun MaterialScope.buildLessonCard(
        context: Context,
        lesson: Lesson,
        isCurrent: Boolean,
        sidePadding: Float
    ): LayoutElement {
        val containerColor = if (isCurrent) colorScheme.primaryContainer
        else colorScheme.surfaceContainerLow
        val contentColor = if (isCurrent) colorScheme.onPrimaryContainer
        else colorScheme.onSurface
        val accentColor = colorScheme.primary

        // Внешний Row с боковыми отступами
        val outerRow = Row.Builder()
            .setWidth(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(
                        Padding.Builder()
                            .setStart(dp(sidePadding))
                            .setEnd(dp(sidePadding))
                            .build()
                    )
                    .build()
            )

        // Сама карточка — expand() по ширине
        val card = Row.Builder()
            .setWidth(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(containerColor.prop)
                            .setCorner(Corner.Builder().setRadius(dp(10f)).build())
                            .build()
                    )
                    .setPadding(
                        Padding.Builder()
                            .setStart(dp(8f))
                            .setEnd(dp(8f))
                            .setTop(dp(4f))
                            .setBottom(dp(4f))
                            .build()
                    )
                    .build()
            )

        // Колонка времени
        val timeColumn = Column.Builder().setWidth(dp(TIME_COLUMN_WIDTH))
        timeColumn.addContent(
            basicText(
                text = lesson.start.layoutString,
                fontStyle = fontStyle(size = 10f, color = contentColor)
            )
        )
        timeColumn.addContent(
            basicText(
                text = lesson.end.layoutString,
                fontStyle = fontStyle(size = 9f, color = contentColor)
            )
        )
        card.addContent(timeColumn.build())

        card.addContent(hSpacer(dp(4f)))

        // Акцентная полоска
        val divider = Box.Builder()
            .setWidth(dp(2f))
            .setHeight(dp(28f))
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(accentColor.prop)
                            .setCorner(Corner.Builder().setRadius(dp(1f)).build())
                            .build()
                    )
                    .build()
            )
            .build()
        card.addContent(divider)

        card.addContent(hSpacer(dp(6f)))

        // Предмет + аудитория
        val infoColumn = Column.Builder().setWidth(expand())
        infoColumn.addContent(
            basicText(
                text = lesson.subject.layoutString,
                fontStyle = fontStyle(size = 11f, color = contentColor)
            )
        )
        if (lesson.room.isNotEmpty()) {
            infoColumn.addContent(
                basicText(
                    text = context.getString(R.string.room_prefix, lesson.room).layoutString,
                    fontStyle = fontStyle(size = 10f, color = accentColor)
                )
            )
        }
        card.addContent(infoColumn.build())

        outerRow.addContent(card.build())
        return outerRow.build()
    }

    // ============================================================
    // Хелперы
    // ============================================================

    private fun paddedRow(sidePadding: Float, content: LayoutElement): LayoutElement {
        val row = Row.Builder().setWidth(expand())
        row.addContent(hSpacer(dp(sidePadding)))
        val inner = Column.Builder().setWidth(expand())
        inner.addContent(content)
        row.addContent(inner.build())
        row.addContent(hSpacer(dp(sidePadding)))
        return row.build()
    }

    private fun vSpacer(height: DpProp): LayoutElement =
        Spacer.Builder().setHeight(height).build()

    private fun hSpacer(width: DpProp): LayoutElement =
        Spacer.Builder().setWidth(width).build()

    private fun headerText(today: DaySchedule?, context: Context): String =
        if (today != null) "${weekdayShort(today.weekday, context)}, ${today.date}"
        else context.getString(R.string.title_schedule)

    private fun findToday(weeks: List<WeekSchedule>?): DaySchedule? {
        if (weeks == null) return null
        val cal = Calendar.getInstance()
        val today = "%02d.%02d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1
        )
        for (week in weeks) {
            for (day in week.days) {
                if (day.date == today) return day
            }
        }
        return null
    }

    private fun weekdayShort(full: String, context: Context): String =
        when (full.lowercase(Locale.ROOT)) {
            "понедельник" -> context.getString(R.string.weekday_mon_short)
            "вторник" -> context.getString(R.string.weekday_tue_short)
            "среда" -> context.getString(R.string.weekday_wed_short)
            "четверг" -> context.getString(R.string.weekday_thu_short)
            "пятница" -> context.getString(R.string.weekday_fri_short)
            "суббота" -> context.getString(R.string.weekday_sat_short)
            "воскресенье" -> context.getString(R.string.weekday_sun_short)
            else -> full.take(2)
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

    private fun isCurrent(lesson: Lesson, nowMin: Int): Boolean {
        val start = parseMinutes(lesson.start)
        val end = parseMinutes(lesson.end)
        return nowMin in start..end
    }
}