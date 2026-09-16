package ru.abg.sstuschedule.model

data class Lesson(
    val start: String,
    val end: String,
    val room: String,
    val subject: String,
    val type: String,
    val teacher: String,
    val teacher2: String? = null
)