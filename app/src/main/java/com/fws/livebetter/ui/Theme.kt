package com.fws.livebetter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Bảng màu "tri thức": xanh navy đậm + vàng đồng cổ điển trên nền giấy ngà (sáng) / xanh đêm (tối).
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F3A68), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5F7), onPrimaryContainer = Color(0xFF0E2144),
    secondary = Color(0xFF9C7418), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E7C3), onSecondaryContainer = Color(0xFF3A2A00),
    tertiary = Color(0xFF1C7C7D),
    background = Color(0xFFFAF6EE), onBackground = Color(0xFF1B1F2A),
    surface = Color(0xFFFFFDF8), onSurface = Color(0xFF1B1F2A),
    surfaceVariant = Color(0xFFF0E9DA), onSurfaceVariant = Color(0xFF4A4636),
    outline = Color(0xFFCFC4AD), outlineVariant = Color(0xFFE6DDC8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5BE6F), onPrimary = Color(0xFF2B1F00),
    primaryContainer = Color(0xFF3A2F14), onPrimaryContainer = Color(0xFFF6E7C3),
    secondary = Color(0xFF9DB8F5), onSecondary = Color(0xFF0E2144),
    secondaryContainer = Color(0xFF2A3358), onSecondaryContainer = Color(0xFFDCE5F7),
    tertiary = Color(0xFF5FD0C4),
    background = Color(0xFF0A0F1D), onBackground = Color(0xFFECE8DD),
    surface = Color(0xFF111830), onSurface = Color(0xFFECE8DD),
    surfaceVariant = Color(0xFF1A2342), onSurfaceVariant = Color(0xFFC9C4B4),
    outline = Color(0xFF3A4570), outlineVariant = Color(0xFF263052),
)

private val Serif = FontFamily.Serif

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp),
)

/** Gradient riêng cho 4 nhóm: Tuổi thọ (ngọc lam), Thời gian & Năng lượng (hổ phách), Tiền bạc (xanh hoàng gia), Tự do & Pháp lý (tím). */
fun groupGradient(group: Int, dark: Boolean): List<Color> = when (group) {
    0 -> if (dark) listOf(Color(0xFF0B3D3A), Color(0xFF0F766E)) else listOf(Color(0xFF0F766E), Color(0xFF2BB5A3))
    1 -> if (dark) listOf(Color(0xFF5A3406), Color(0xFFB45309)) else listOf(Color(0xFFB45309), Color(0xFFF2A93B))
    2 -> if (dark) listOf(Color(0xFF14296B), Color(0xFF1E40AF)) else listOf(Color(0xFF1E40AF), Color(0xFF4F83F0))
    else -> if (dark) listOf(Color(0xFF3B1782), Color(0xFF5B21B6)) else listOf(Color(0xFF5B21B6), Color(0xFF9A6BF5))
}

val RankA = Color(0xFF2E7D5B)
val RankB = Color(0xFFC58A1B)
val RankC = Color(0xFF7A869A)
val Heart = Color(0xFFE0566B)

@Composable
fun LiveBetterTheme(content: @Composable () -> Unit) {
    // AppCompatDelegate.setDefaultNightMode() quyết định sáng/tối → isSystemInDarkTheme() phản ánh lựa chọn của người dùng.
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
