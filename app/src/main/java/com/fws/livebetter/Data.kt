package com.fws.livebetter

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** Một mục trong sách: chỉ giữ tiêu đề, xếp hạng, nói đơn giản, chi phí, lợi ích. */
data class Item(
    val id: Int, val chapter: Int, val num: Int,
    val title: String, val rank: String,
    val simple: String, val cost: String, val benefit: String,
    val search: String,
)

data class Group(val id: Int, val names: Map<String, String>)
data class Chapter(val id: Int, val group: Int, val count: Int, val names: Map<String, String>)

class Content(val items: List<Item>, val groups: List<Group>, val chapters: List<Chapter>) {
    val itemsById: Map<Int, Item> = items.associateBy { it.id }
    val chapterById: Map<Int, Chapter> = chapters.associateBy { it.id }
    val byChapter: Map<Int, List<Item>> = items.groupBy { it.chapter }
    val byGroup: List<List<Item>> = groups.map { g -> items.filter { chapterById[it.chapter]?.group == g.id } }
    fun groupOf(item: Item): Int = chapterById[item.chapter]?.group ?: 0
}

/** Khóa ngôn ngữ dùng cho tên chủ đề: vi, en, zh, es, pt, ru, ja, ko. */
fun langKey(locale: Locale): String = when (locale.language) {
    "vi", "en", "zh", "es", "pt", "ru", "ja", "ko" -> locale.language
    else -> "en"
}

fun Map<String, String>.pick(lang: String): String = this[lang] ?: this["en"] ?: this["vi"] ?: ""

private val MARKS = Regex("\\p{M}+")

/** Bỏ dấu tiếng Việt để tìm kiếm có dấu/không dấu đều được. */
fun normalize(s: String): String =
    MARKS.replace(Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD), "").replace('đ', 'd')

object ContentLoader {
    private fun names(o: JSONObject): Map<String, String> =
        o.keys().asSequence().associateWith { o.getString(it) }

    fun load(ctx: Context): Content {
        val topics = JSONObject(ctx.assets.open("topics.json").bufferedReader().use { it.readText() })
        val groups = topics.getJSONArray("groups").let { a ->
            (0 until a.length()).map { val o = a.getJSONObject(it); Group(o.getInt("id"), names(o.getJSONObject("names"))) }
        }
        val chapters = topics.getJSONArray("chapters").let { a ->
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Chapter(o.getInt("id"), o.getInt("group"), o.getInt("count"), names(o.getJSONObject("names")))
            }
        }
        val arr = JSONArray(ctx.assets.open("items.json").bufferedReader().use { it.readText() })
        val items = (0 until arr.length()).map {
            val r = arr.getJSONArray(it)
            val title = r.getString(3)
            val simple = r.getString(5)
            Item(r.getInt(0), r.getInt(1), r.getInt(2), title, r.getString(4), simple, r.getString(6), r.getString(7),
                normalize("$title $simple"))
        }
        return Content(items, groups, chapters)
    }
}
