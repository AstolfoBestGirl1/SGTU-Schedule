package ru.abg.sstuschedule.model

/**
 * Расписание на одну учебную неделю.
 */
data class WeekSchedule(
    val startDate: String,   // "14.09"
    val days: List<DaySchedule>
)