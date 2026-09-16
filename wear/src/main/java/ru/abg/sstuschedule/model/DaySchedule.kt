package ru.abg.sstuschedule.model

data class DaySchedule(
    val date: String,
    val weekday: String,
    val lessons: List<Lesson>
)