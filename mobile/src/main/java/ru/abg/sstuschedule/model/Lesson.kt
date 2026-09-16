package ru.abg.sstuschedule.model

data class Lesson(
    val start: String,          // "08:00"
    val end: String,            // "09:30"
    val room: String,           // "1/118"
    val subject: String,        // "Сопротивление материалов"
    val type: String,           // "лекц", "прак", "лаб"
    val teacher: String,        // "Пименов Дмитрий Алексеевич"
    val teacher2: String? = null // второй преподаватель (подгруппа)
)