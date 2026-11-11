package com.fws.howtolivebetter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    /** Trạng thái quảng cáo mở ứng dụng; nằm trong ViewModel để không mất khi xoay màn hình / đổi ngôn ngữ. */
    val adState = AdState()

    /** Popup chọn ngôn ngữ ở lần mở đầu tiên; nằm trong ViewModel để không hiện lại khi xoay màn hình / đổi ngôn ngữ. */
    private val _showLanguagePicker = MutableStateFlow(!prefs.langChosen)
    val showLanguagePicker: StateFlow<Boolean> = _showLanguagePicker

    fun confirmLanguage() {
        prefs.langChosen = true
        _showLanguagePicker.value = false
    }

    private val _content = MutableStateFlow<Content?>(null)
    val content: StateFlow<Content?> = _content

    private val _favorites = MutableStateFlow(prefs.favorites())
    val favorites: StateFlow<Set<Int>> = _favorites

    val query = MutableStateFlow("")

    /** id mục hiển thị cho 4 nhóm trên màn hình chính. */
    private val _picks = MutableStateFlow<List<Int>>(emptyList())
    val picks: StateFlow<List<Int>> = _picks

    private val _remaining = MutableStateFlow(0)
    val remainingSec: StateFlow<Int> = _remaining

    val results: StateFlow<List<Item>> = combine(query, _content) { q, c ->
        if (c == null || q.isBlank()) emptyList() else search(c, q)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch(Dispatchers.Default) { _content.value = ContentLoader.load(app) }
    }

    private fun search(c: Content, q: String): List<Item> {
        val tokens = normalize(q).split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        return c.items.filter { it.search.let { s -> tokens.all { t -> s.contains(t) } } }
            .sortedBy { i -> if (tokens.all { normalize(i.title).contains(it) }) 0 else 1 }
            .take(100)
    }

    fun toggleFavorite(id: Int) {
        val s = _favorites.value.toMutableSet()
        if (!s.add(id)) s.remove(id)
        _favorites.value = s
        prefs.saveFavorites(s)
    }

    // ---- Xoay vòng 4 mục mỗi 5 phút, dựa trên đồng hồ thực → vẫn đúng khi app đã bị tắt ----
    private var ticker: Job? = null

    fun onForeground() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) { tick(); delay(1000) }
        }
    }

    fun onBackground() { ticker?.cancel() }

    private fun tick() {
        val c = _content.value ?: return
        val now = System.currentTimeMillis()
        val last = prefs.rotTs
        val stored = prefs.rotIds()
        val valid = stored.size == c.groups.size && stored.all { c.itemsById.containsKey(it) }
        val expired = !valid || now < last || now - last >= PERIOD_MS
        if (expired) roll(c, now, stored) else if (_picks.value != stored) _picks.value = stored
        _remaining.value = (((prefs.rotTs + PERIOD_MS) - now) / 1000).toInt().coerceIn(0, (PERIOD_MS / 1000).toInt())
    }

    private fun roll(c: Content, now: Long, old: List<Int>) {
        val ids = c.byGroup.mapIndexed { g, pool ->
            var p = pool.random()
            if (pool.size > 1) while (p.id == old.getOrNull(g)) p = pool.random()
            p.id
        }
        prefs.saveRot(ids, now)
        _picks.value = ids
    }

    companion object { const val PERIOD_MS = 5 * 60 * 1000L }
}
