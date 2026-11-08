package com.fws.livebetter

import android.content.Context
import androidx.core.content.edit

/** Mọi dữ liệu người dùng chỉ lưu cục bộ trên máy (SharedPreferences). */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("live_better", Context.MODE_PRIVATE)

    fun favorites(): Set<Int> = sp.getStringSet("fav", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
    fun saveFavorites(s: Set<Int>) = sp.edit { putStringSet("fav", s.map { it.toString() }.toSet()) }

    /** "system" | "light" | "dark" */
    var theme: String
        get() = sp.getString("theme", "system")!!
        set(v) = sp.edit { putString("theme", v) }

    var ttsRate: Float
        get() = sp.getFloat("tts_rate", 2f)
        set(v) = sp.edit { putFloat("tts_rate", v) }

    /** Thời điểm (epoch ms) lần xoay vòng gần nhất — còn hiệu lực cả khi tắt app. */
    var rotTs: Long
        get() = sp.getLong("rot_ts", 0L)
        set(v) = sp.edit { putLong("rot_ts", v) }

    fun rotIds(): List<Int> = (sp.getString("rot_ids", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }
    fun saveRot(ids: List<Int>, ts: Long) = sp.edit {
        putString("rot_ids", ids.joinToString(","))
        putLong("rot_ts", ts)
    }
}
