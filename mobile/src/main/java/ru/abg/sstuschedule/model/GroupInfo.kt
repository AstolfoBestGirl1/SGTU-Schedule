package ru.abg.sstuschedule.model

data class GroupInfo(
    val id: Int,
    val name: String,
    val institute: String,
    val eduForm: String,   // "Очная", "Заочная", "Очно-заочная"
    val groupType: String  // "Бакалавриат", "Магистратура", "Специалитет"
)