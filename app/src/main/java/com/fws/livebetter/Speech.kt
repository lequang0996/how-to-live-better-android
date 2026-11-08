package com.fws.livebetter

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Đọc thành tiếng bằng TextToSpeech của hệ thống (giọng tiếng Việt vì nội dung sách là tiếng Việt). */
class SpeechController(context: Context) {
    enum class State { IDLE, SPEAKING, UNAVAILABLE }

    private val appCtx = context.applicationContext
    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private var tts: TextToSpeech? = null
    private var initOk = false
    private var pending: String? = null
    private var gen = 0
    var rate = 2f

    fun speak(text: String) {
        val engine = tts
        if (engine == null) {
            pending = text
            tts = TextToSpeech(appCtx) { status ->
                if (status != TextToSpeech.SUCCESS) { _state.value = State.UNAVAILABLE; tts?.shutdown(); tts = null; return@TextToSpeech }
                initOk = true
                tts?.setOnUtteranceProgressListener(listener)
                pending?.let { p -> pending = null; start(p) }
            }
        } else if (initOk) start(text) else pending = text
    }

    private fun start(text: String) {
        val engine = tts ?: return
        val r = engine.setLanguage(Locale("vi", "VN"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            _state.value = State.UNAVAILABLE
            return
        }
        engine.setSpeechRate(rate)
        gen++
        val id = gen
        val max = minOf(TextToSpeech.getMaxSpeechInputLength() - 100, 3500)
        val chunks = chunk(text, max)
        engine.stop()
        _state.value = State.SPEAKING
        chunks.forEachIndexed { i, c ->
            val uid = if (i == chunks.lastIndex) "g${id}_last" else "g${id}_$i"
            engine.speak(c, TextToSpeech.QUEUE_ADD, null, uid)
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) { if (utteranceId == "g${gen}_last") _state.value = State.IDLE }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) { if (utteranceId?.startsWith("g$gen") == true) _state.value = State.IDLE }
        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            if (utteranceId?.startsWith("g${gen}_") == true) _state.value = State.IDLE
        }
    }

    fun stop() {
        tts?.stop()
        pending = null
        if (_state.value == State.SPEAKING) _state.value = State.IDLE
    }

    fun clearUnavailable() { if (_state.value == State.UNAVAILABLE) _state.value = State.IDLE }

    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null; initOk = false }

    private fun chunk(text: String, max: Int): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (s in text.split(Regex("(?<=[.!?])\\s+"))) {
            if (sb.length + s.length + 1 > max && sb.isNotEmpty()) { out += sb.toString(); sb.clear() }
            if (s.length > max) { s.chunked(max).forEach { out += it } } else { if (sb.isNotEmpty()) sb.append(' '); sb.append(s) }
        }
        if (sb.isNotEmpty()) out += sb.toString()
        return out.ifEmpty { listOf(text) }
    }
}
