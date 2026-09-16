package ru.abg.sstuschedule.model

data class WeekSchedule(
    val startDate: String,
    val days: List<DaySchedule>
)