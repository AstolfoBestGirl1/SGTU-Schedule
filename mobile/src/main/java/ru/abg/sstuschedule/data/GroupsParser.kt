package ru.abg.sstuschedule.data

import android.util.Log
import org.jsoup.Jsoup
import ru.abg.sstuschedule.model.GroupInfo

/**
 * Парсит HTML-страницу https://rasp.sstu.ru/ с деревом групп.
 *
 * Структура:
 *   .card → институт
 *     .institute — название
 *     .edu-form — форма обучения
 *     .group-type — тип программы
 *     .row.groups → .group a[href=/rasp/group/ID] — название группы
 */
object GroupsParser {

    private const val TAG = "GroupsParser"
    private const val GROUP_URL_PREFIX = "/rasp/group/"

    fun parse(html: String): List<GroupInfo> {
        val result = mutableListOf<GroupInfo>()

        try {
            val doc = Jsoup.parse(html)

            for (card in doc.select(".card")) {
                val institute = card.selectFirst(".institute")?.text()?.trim() ?: continue

                var eduForm = ""
                var groupType = ""

                // Идём по всем прямым потомкам .card-body в порядке следования
                val cardBody = card.selectFirst(".card-body") ?: continue
                for (element in cardBody.children()) {
                    when {
                        element.hasClass("edu-form") -> {
                            eduForm = element.text().trim()
                        }
                        element.hasClass("group-type") -> {
                            groupType = element.text().trim()
                        }
                        element.hasClass("row") && element.hasClass("groups") -> {
                            for (a in element.select(".group a")) {
                                val href = a.attr("href")
                                if (!href.startsWith(GROUP_URL_PREFIX)) continue
                                val id = href.removePrefix(GROUP_URL_PREFIX)
                                    .trimEnd('/')
                                    .toIntOrNull() ?: continue
                                val name = a.text().trim()
                                if (name.isEmpty()) continue

                                result.add(
                                    GroupInfo(
                                        id = id,
                                        name = name,
                                        institute = institute,
                                        eduForm = eduForm,
                                        groupType = groupType
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга HTML", e)
        }

        Log.d(TAG, "Найдено групп: ${result.size}")
        return result
    }
}