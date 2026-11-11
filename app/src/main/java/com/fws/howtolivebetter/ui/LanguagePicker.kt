package com.fws.howtolivebetter.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fws.howtolivebetter.R
import java.util.Locale

/** key: khóa ngôn ngữ nội bộ (langKey) · tag: mã locale áp dụng cho app · native: tên ngôn ngữ viết bằng chính nó. */
private class PickerLang(val key: String, val tag: String, val flag: String, val native: String)

private val PICKER_LANGS = listOf(
    PickerLang("en", "en", "🇬🇧", "English"),
    PickerLang("zh", "zh-CN", "🇨🇳", "简体中文"),
    PickerLang("es", "es", "🇪🇸", "Español"),
    PickerLang("pt", "pt-BR", "🇧🇷", "Português (Brasil)"),
    PickerLang("ru", "ru", "🇷🇺", "Русский"),
    PickerLang("ja", "ja", "🇯🇵", "日本語"),
    PickerLang("ko", "ko", "🇰🇷", "한국어"),
    PickerLang("vi", "vi", "🇻🇳", "Tiếng Việt"),
)

/** Lấy chuỗi theo ngôn ngữ [tag] mà không đổi ngôn ngữ của app → tiêu đề/nút đổi theo ngay khi chạm chọn. */
@Composable
private fun localized(tag: String, @StringRes id: Int): String {
    val ctx = LocalContext.current
    val cfg = LocalConfiguration.current
    return remember(tag, cfg) {
        val c = Configuration(cfg)
        c.setLocale(Locale.forLanguageTag(tag))
        ctx.createConfigurationContext(c).getString(id)
    }
}

/**
 * Popup chọn ngôn ngữ hiện ở lần mở app đầu tiên.
 * Chạm một ngôn ngữ để xem trước (chưa áp dụng), bấm "Tiếp tục" mới áp dụng và đóng popup.
 */
@Composable
fun LanguagePickerDialog(onConfirm: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val current = currentLang()
    var selected by rememberSaveable { mutableStateOf(current) }
    val sel = PICKER_LANGS.firstOrNull { it.key == selected } ?: PICKER_LANGS.first()
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val gold = Color(0xFFE5BE6F)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.92f),
        ) {
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp).widthIn(max = 420.dp).fillMaxWidth(),
                shape = RoundedCornerShape(32.dp), color = cs.surface, tonalElevation = 6.dp, shadowElevation = 16.dp,
            ) {
                Column {
                    // ── Đầu popup: gradient xanh navy giống header menu, biểu tượng quả địa cầu màu vàng đồng ──
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Color(0xFF0E1A33), Color(0xFF1F3A68), Color(0xFF2B4C8C))))
                    ) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 36.dp, y = (-28).dp).size(130.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
                        Box(Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = 30.dp).size(80.dp).clip(CircleShape).background(gold.copy(alpha = 0.10f)))
                        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 22.dp)) {
                            Box(
                                Modifier.size(52.dp).clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(gold.copy(alpha = 0.32f), gold.copy(alpha = 0.12f)))),
                                contentAlignment = Alignment.Center,
                            ) { Icon(AppIcons.Language, null, Modifier.size(28.dp), tint = gold) }
                            Spacer(Modifier.height(14.dp))
                            Crossfade(sel.key, animationSpec = tween(220), label = "pickerTitle") { k ->
                                val tag = PICKER_LANGS.first { it.key == k }.tag
                                Column {
                                    Text(localized(tag, R.string.lang_picker_title), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        localized(tag, R.string.lang_picker_subtitle),
                                        color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }

                    // ── Danh sách ngôn ngữ (cuộn được trên màn hình nhỏ) ──
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PICKER_LANGS.forEach { l ->
                            PickerRow(l, selected = l.key == selected) { selected = l.key }
                        }
                    }

                    // ── Nút Tiếp tục ──
                    Button(
                        onClick = {
                            onConfirm()
                            if (sel.key != current) {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(sel.tag))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 18.dp).height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(localized(sel.tag, R.string.lang_picker_continue), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(l: PickerLang, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.45f), tween(180), label = "rowBg")
    val line by animateColorAsState(if (selected) cs.primary else cs.outlineVariant, tween(180), label = "rowLine")
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = bg, border = BorderStroke(if (selected) 1.5.dp else 1.dp, line),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(cs.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) { Text(l.flag, style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.width(14.dp))
            Text(
                l.native, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) cs.onPrimaryContainer else cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            if (selected) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = cs.onPrimary)
                }
            } else {
                Box(Modifier.size(24.dp).border(1.5.dp, cs.outline, CircleShape))
            }
        }
    }
}
