package com.fws.livebetter.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fws.livebetter.*
import com.fws.livebetter.R
import com.fws.livebetter.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Ngôn ngữ giao diện hiện tại (khóa vi/en/zh/...). Đọc từ Configuration nên tự cập nhật khi đổi ngôn ngữ. */
@Composable
fun currentLang(): String = langKey(LocalConfiguration.current.locales[0])

/** Hàm quy đổi token tiền tệ theo ngôn ngữ đang chọn. */
@Composable
fun rememberMoney(): (String) -> String {
    val lang = currentLang()
    return remember(lang) {
        val cur = Money.curFor(lang)
        val loc = Money.localeFor(lang)
        ({ s: String -> Money.render(s, cur, loc) })
    }
}

@Composable
fun rankColor(rank: String): Color = when (rank) { "A" -> RankA; "B" -> RankB; else -> RankC }

@Composable
fun rankName(rank: String): String = stringResource(
    when (rank) { "A" -> R.string.rank_a; "B" -> R.string.rank_b; else -> R.string.rank_c }
)

@Composable
fun RankBadge(rank: String, size: Int = 34, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(rankColor(rank)),
        contentAlignment = Alignment.Center,
    ) { Text(rank, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
}

@Composable
fun ItemRow(item: Item, content: Content, isFav: Boolean, onClick: () -> Unit, onFav: () -> Unit, modifier: Modifier = Modifier) {
    val money = rememberMoney()
    val lang = currentLang()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            RankBadge(item.rank)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(money(item.title), style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(
                    content.chapterById[item.chapter]?.names?.pick(lang).orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            FavButton(isFav, onFav)
        }
    }
}

@Composable
fun FavButton(isFav: Boolean, onClick: () -> Unit, tint: Color? = null) {
    IconButton(onClick = onClick) {
        Icon(
            if (isFav) Icons.Filled.Favorite else AppIcons.FavoriteBorder,
            contentDescription = stringResource(if (isFav) R.string.remove_fav else R.string.add_fav),
            tint = if (isFav) Heart else tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Cây chủ đề: 4 nhóm chính → 34 chủ đề con. Dùng ở Drawer và màn hình chính. */
@Composable
fun TopicTree(content: Content, onChapter: (Int) -> Unit, modifier: Modifier = Modifier) {
    val lang = currentLang()
    val dark = isSystemInDarkTheme()
    var open by rememberSaveable { mutableIntStateOf(-1) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        content.groups.forEach { g ->
            val chapters = content.chapters.filter { it.group == g.id }
            val total = chapters.sumOf { it.count }
            val expanded = open == g.id
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().animateContentSize(),
            ) {
                Column {
                    Row(
                        Modifier.clickable { open = if (expanded) -1 else g.id }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(width = 6.dp, height = 40.dp).clip(RoundedCornerShape(3.dp))
                                .background(Brush.verticalGradient(groupGradient(g.id, dark)))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.names.pick(lang), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.items_count, total) + " · " + chapters.size,
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                    }
                    if (expanded) {
                        chapters.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onChapter(c.id) }.padding(start = 36.dp, end = 14.dp, top = 11.dp, bottom = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${c.id}.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(28.dp))
                                Text(c.names.pick(lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("${c.count}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

/** Banner AdMob thích ứng. Chỉ được gọi sau khi đã có đồng ý (UMP) và MobileAds.initialize. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val cfg = LocalConfiguration.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val adView = remember(ctx, cfg.screenWidthDp) {
        AdView(ctx).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, cfg.screenWidthDp))
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AndroidView(factory = { adView }) }
}

tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
