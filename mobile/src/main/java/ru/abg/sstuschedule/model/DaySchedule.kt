package ru.abg.sstuschedule.model

/**
 * Расписание на один день.
 */
data class DaySchedule(
    val date: String,        // "14.09"
    val weekday: String,     // "Понедельник"
    val lessons: List<Lesson>
)