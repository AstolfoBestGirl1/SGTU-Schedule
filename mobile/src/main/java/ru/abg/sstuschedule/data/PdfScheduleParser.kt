package ru.abg.sstuschedule.data

import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import ru.abg.sstuschedule.model.DaySchedule
import ru.abg.sstuschedule.model.Lesson
import ru.abg.sstuschedule.model.WeekSchedule
import java.io.File
import kotlin.math.abs

/**
 * Парсер PDF расписания СГТУ (итерация 11).
 *
 * Что нового:
 * - Поддержка подгрупп: "Иностранный язык" (Савченко / Дунаева).
 *   teacher = первый преподаватель, teacher2 = второй.
 */
class PdfScheduleParser {

    companion object {
        private const val TAG = "PdfScheduleParser"

        private val DATE_REGEX = Regex("""^\d{2}\.\d{2}$""")
        private val TIME_REGEX = Regex("""^\d{1,2}:\d{2}$""")
        private val ROOM_REGEX = Regex("""^[А-Яа-я0-9]+[/\-][0-9А-Яа-я]+$""")
        private val TYPE_REGEX = Regex("""\((лекц|прак|лаб|экзам|зач[а-я]*|курс[а-я]*)\)""")
        private val SUBGROUP_REGEX = Regex("""Подгр\.\s*(\d)""")

        private val WEEKDAYS = listOf(
            "Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота"
        )

        private const val DATE_CENTER_OFFSET = 10f
        private const val INTRA_CELL_GAP = 22f
    }

    data class Fragment(
        val text: String,
        val x: Float,
        val y: Float,
        val page: Int
    )

    private class FragmentCollector : PDFTextStripper() {
        val fragments = mutableListOf<Fragment>()

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            if (textPositions.isEmpty()) return

            var currentStart = 0
            var lastRight = textPositions[0].xDirAdj + textPositions[0].widthDirAdj

            for (i in 1 until textPositions.size) {
                val pos = textPositions[i]
                if (pos.xDirAdj - lastRight > INTRA_CELL_GAP) {
                    if (i > currentStart) addChunk(textPositions, currentStart, i)
                    currentStart = i
                }
                lastRight = maxOf(lastRight, pos.xDirAdj + pos.widthDirAdj)
            }
            if (currentStart < textPositions.size) {
                addChunk(textPositions, currentStart, textPositions.size)
            }
        }

        private fun addChunk(positions: List<TextPosition>, from: Int, to: Int) {
            val chunk = positions.subList(from, to)
            val text = chunk.joinToString("") { it.unicode }.trim()
            if (text.isEmpty()) return
            val first = chunk.first()
            fragments.add(Fragment(text, first.xDirAdj, first.yDirAdj, currentPageNo))
        }
    }

    fun parse(file: File): List<WeekSchedule>? {
        return try {
            PDDocument.load(file).use { document ->
                val stripper = FragmentCollector()
                stripper.sortByPosition = true
                stripper.getText(document)
                Log.d(TAG, "Всего фрагментов: ${stripper.fragments.size}")
                buildSchedule(stripper.fragments)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга", e)
            null
        }
    }

    private fun buildSchedule(fragments: List<Fragment>): List<WeekSchedule> {
        val anchors = fragments
            .filter { it.text.equals("Понедельник", ignoreCase = true) }
            .sortedWith(compareBy({ it.page }, { it.y }))

        if (anchors.isEmpty()) return emptyList()

        val maxPage = fragments.maxOf { it.page }
        val weeks = mutableListOf<WeekSchedule>()

        for ((i, anchor) in anchors.withIndex()) {
            val next = anchors.getOrNull(i + 1)

            val samePageBottom = if (next != null && next.page == anchor.page) {
                next.y - 5f
            } else {
                (fragments.filter { it.page == anchor.page }.maxOfOrNull { it.y } ?: 800f) + 10f
            }

            val primary = fragments.filter {
                it.page == anchor.page && it.y in (anchor.y - 5f)..samePageBottom
            }

            val contFragments = mutableListOf<Fragment>()
            val nextPageWithAnchor = next?.page ?: (maxPage + 1)
            for (p in (anchor.page + 1) until nextPageWithAnchor) {
                contFragments.addAll(fragments.filter { it.page == p })
            }

            parseWeek(primary, contFragments, i + 1)?.let { weeks.add(it) }
        }

        return weeks
    }

    private fun parseWeek(
        primary: List<Fragment>,
        continuation: List<Fragment>,
        weekNumber: Int
    ): WeekSchedule? {
        val dateFragments = primary
            .filter { DATE_REGEX.matches(it.text) }
            .sortedBy { it.x }

        if (dateFragments.isEmpty()) return null

        val dateCenters = dateFragments.map { it.x + DATE_CENTER_OFFSET }
        val columnWidth = if (dateCenters.size >= 2) {
            dateCenters[1] - dateCenters[0]
        } else 91f

        val dayLessons = MutableList(dateCenters.size) { mutableListOf<Lesson>() }

        processPage(primary, dateCenters, columnWidth, dayLessons)
        if (continuation.isNotEmpty()) {
            continuation.groupBy { it.page }.forEach { (_, frags) ->
                processPage(frags, dateCenters, columnWidth, dayLessons)
            }
        }

        val days = dateCenters.indices.map { i ->
            DaySchedule(
                date = dateFragments.getOrNull(i)?.text ?: "",
                weekday = WEEKDAYS.getOrNull(i) ?: "",
                lessons = dayLessons[i]
            )
        }

        return WeekSchedule(startDate = dateFragments.first().text, days = days)
    }

    private fun processPage(
        pageFragments: List<Fragment>,
        dateCenters: List<Float>,
        columnWidth: Float,
        dayLessons: MutableList<MutableList<Lesson>>
    ) {
        val timeStarts = pageFragments
            .filter { TIME_REGEX.matches(it.text) && it.x < 50f }
            .sortedBy { it.y }
            .filterIndexed { idx, _ -> idx % 2 == 0 }

        if (timeStarts.isEmpty()) return

        val maxY = pageFragments.maxOf { it.y }

        for ((rowIdx, timeFrag) in timeStarts.withIndex()) {
            val time = normalizeTime(timeFrag.text)
            val yStart = timeFrag.y - 5f
            val yEnd = if (rowIdx + 1 < timeStarts.size) {
                timeStarts[rowIdx + 1].y - 5f
            } else maxY + 5f

            val rowFrags = pageFragments.filter { it.x >= 50f && it.y in yStart..yEnd }
            if (rowFrags.isEmpty()) continue

            val byDay = mutableMapOf<Int, MutableList<Fragment>>()
            for (frag in rowFrags) {
                val dayIndex = dateCenters.indices
                    .minByOrNull { abs(dateCenters[it] - frag.x) } ?: continue
                if (abs(dateCenters[dayIndex] - frag.x) <= columnWidth / 2) {
                    byDay.getOrPut(dayIndex) { mutableListOf() }.add(frag)
                }
            }

            for ((dayIdx, frags) in byDay) {
                parseCell(frags, time)?.let { dayLessons[dayIdx].add(it) }
            }
        }
    }

    // ============================================================
    // Разбор одной ячейки
    // ============================================================

    private fun parseCell(fragments: List<Fragment>, timeStart: String): Lesson? {
        val lines = fragments
            .sortedWith(compareBy({ it.y }, { it.x }))
            .map { it.text }
            .filter { !TIME_REGEX.matches(it) && it != "-" && it != "–" && it != "—" }
            .toMutableList()

        if (lines.isEmpty()) return null

        // 1. Аудитория
        val roomIndex = lines.indexOfFirst { ROOM_REGEX.matches(it) }
        val room = if (roomIndex >= 0) lines.removeAt(roomIndex) else ""

        // 2. Тип
        var type = ""
        val typeIndex = lines.indexOfFirst { TYPE_REGEX.containsMatchIn(it) }
        if (typeIndex >= 0) {
            val line = lines[typeIndex]
            val match = TYPE_REGEX.find(line)!!
            type = match.groupValues[1]

            val before = line.substring(0, match.range.first).trim()
            val after = line.substring(match.range.last + 1).trim()

            lines.removeAt(typeIndex)
            if (after.isNotEmpty()) lines.add(typeIndex, after)
            if (before.isNotEmpty()) lines.add(typeIndex, before)
        }

        if (lines.isEmpty() && room.isEmpty()) return null

        // 3. Проверяем наличие подгрупп
        val subgroupIndices = lines.indices.filter { lines[it].contains("Подгр", ignoreCase = true) }

        val subject: String
        val teacher: String
        val teacher2: String?

        if (subgroupIndices.size >= 2) {
            // Есть подгруппы
            val idx1 = subgroupIndices[0]
            val idx2 = subgroupIndices[1]

            subject = lines.subList(0, idx1).joinToString(" ").trim()
            teacher = lines.subList(idx1 + 1, idx2).joinToString(" ").trim()
            teacher2 = lines.subList(idx2 + 1, lines.size).joinToString(" ").trim()
                .takeIf { it.isNotEmpty() }
        } else if (subgroupIndices.size == 1) {
            // Только одна подгруппа (нестандартный случай)
            val idx1 = subgroupIndices[0]
            subject = lines.subList(0, idx1).joinToString(" ").trim()
            teacher = lines.subList(idx1 + 1, lines.size).joinToString(" ").trim()
            teacher2 = null
        } else {
            // Обычный случай
            val tStart = findTeacherStartIndex(lines)
            when {
                tStart > 0 -> {
                    subject = lines.subList(0, tStart).joinToString(" ").trim()
                    teacher = lines.subList(tStart, lines.size).joinToString(" ").trim()
                }
                tStart == 0 -> {
                    subject = ""
                    teacher = lines.joinToString(" ").trim()
                }
                else -> {
                    subject = lines.joinToString(" ").trim()
                    teacher = ""
                }
            }
            teacher2 = null
        }

        if (subject.isEmpty() && teacher.isEmpty() && room.isEmpty()) return null

        return Lesson(
            start = timeStart,
            end = timeToEnd(timeStart),
            room = room,
            subject = subject,
            type = type,
            teacher = teacher,
            teacher2 = teacher2
        )
    }

    private fun findTeacherStartIndex(lines: List<String>): Int {
        val re = Regex(""".*(ич|вна|чна|овна|евна|ична)$""", RegexOption.IGNORE_CASE)

        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (line.contains("Подгр", ignoreCase = true)) continue
            if (re.matches(line)) {
                var start = i
                if (i - 1 >= 0) {
                    val prev = lines[i - 1]
                    val looksLikeName = prev.firstOrNull()?.isUpperCase() == true &&
                            !prev.contains("(") && !prev.contains(":") &&
                            !prev.contains("Подгр", ignoreCase = true)
                    if (looksLikeName) start = i - 1
                }
                return start
            }
        }
        return -1
    }

    private fun normalizeTime(t: String): String {
        val idx = t.indexOf(":")
        if (idx <= 0) return t
        return "${t.substring(0, idx).padStart(2, '0')}:${t.substring(idx + 1).padStart(2, '0')}"
    }

    private fun timeToEnd(t: String): String = when (t) {
        "08:00" -> "09:30"
        "09:45" -> "11:15"
        "11:30" -> "13:00"
        "13:40" -> "15:10"
        "15:20" -> "16:50"
        else -> ""
    }
}