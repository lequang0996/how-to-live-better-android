package com.fws.livebetter.ui

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.fws.livebetter.*
import com.fws.livebetter.R
import com.fws.livebetter.BuildConfig
import java.text.NumberFormat
import java.util.Locale

val LocalSpeech = staticCompositionLocalOf<SpeechController> { error("SpeechController not provided") }

@Composable
fun BackButton(onBack: () -> Unit) = IconButton(onClick = onBack) {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
}

// ───────────────────────────── HOME ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel, content: Content,
    onMenu: () -> Unit, onItem: (Int) -> Unit, onChapter: (Int) -> Unit, onFavorites: () -> Unit,
) {
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val picks by vm.picks.collectAsState()
    val remaining by vm.remainingSec.collectAsState()
    val favs by vm.favorites.collectAsState()
    val focus = LocalFocusManager.current
    val lang = currentLang()
    val dark = isSystemInDarkTheme()
    val pager = rememberPagerState(pageCount = { content.groups.size })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.menu)) } },
                actions = { IconButton(onFavorites) { Icon(Icons.Filled.Favorite, stringResource(R.string.favorites), tint = Heart) } },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { vm.query.value = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton({ vm.query.value = "" }) { Icon(Icons.Filled.Close, stringResource(R.string.clear)) }
                    },
                    singleLine = true, shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            if (query.isNotBlank()) {
                if (results.isEmpty()) item {
                    Text(stringResource(R.string.no_results), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else items(results, key = { it.id }) {
                    ItemRow(it, content, it.id in favs, { onItem(it.id) }, { vm.toggleFavorite(it.id) }, Modifier.padding(horizontal = 16.dp))
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.today_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                stringResource(R.string.next_change, "%02d:%02d".format(remaining / 60, remaining % 60)),
                                Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                item {
                    HorizontalPager(
                        state = pager, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        val item = picks.getOrNull(page)?.let { content.itemsById[it] }
                        Box(Modifier.fillMaxWidth().height(300.dp)) {
                            if (item != null) Crossfade(item, label = "hero") {
                                HeroCard(it, content, { onItem(it.id) }, it.id in favs, { vm.toggleFavorite(it.id) })
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(content.groups.size) { i ->
                            val sel = pager.currentPage == i
                            Box(
                                Modifier.padding(3.dp).size(width = if (sel) 22.dp else 8.dp, height = 8.dp).clip(CircleShape)
                                    .background(if (sel) groupGradient(i, dark).last() else MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.stats, content.items.size, content.chapters.size, content.groups.size),
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                item {
                    Text(stringResource(R.string.browse), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge)
                }
                item { TopicTree(content, onChapter, Modifier.padding(horizontal = 16.dp)) }
                item {
                    Text(
                        stringResource(R.string.content_note) + "\n" + stringResource(R.string.disclaimer),
                        Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(item: Item, content: Content, onClick: () -> Unit, isFav: Boolean, onFav: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val lang = currentLang()
    val money = rememberMoney()
    val g = content.groupOf(item)
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(groupGradient(g, dark)))
            .clickable(onClick = onClick).padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 16.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.18f)) {
                        Text(
                            content.groups[g].names.pick(lang), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    FavButton(isFav, onFav, tint = Color.White)
                }
                Text(money(item.title), Modifier.padding(end = 12.dp), color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(money(item.simple), Modifier.padding(end = 12.dp), color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RankBadge(item.rank, 28)
                Spacer(Modifier.width(8.dp))
                Text(rankName(item.rank), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    content.chapterById[item.chapter]?.names?.pick(lang).orEmpty(),
                    color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp),
                )
            }
        }
    }
}

// ───────────────────────────── CHAPTER / FAVORITES ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(vm: MainViewModel, content: Content, chapterId: Int, onBack: () -> Unit, onItem: (Int) -> Unit) {
    val favs by vm.favorites.collectAsState()
    val lang = currentLang()
    val items = content.byChapter[chapterId].orEmpty()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(content.chapterById[chapterId]?.names?.pick(lang).orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { BackButton(onBack) },
        )
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { ItemRow(it, content, it.id in favs, { onItem(it.id) }, { vm.toggleFavorite(it.id) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(vm: MainViewModel, content: Content, onBack: () -> Unit, onItem: (Int) -> Unit) {
    val favs by vm.favorites.collectAsState()
    val list = content.items.filter { it.id in favs }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.favorites), style = MaterialTheme.typography.titleLarge) }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        if (list.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_favorites), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
        } else LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { ItemRow(it, content, true, { onItem(it.id) }, { vm.toggleFavorite(it.id) }) }
        }
    }
}

// ───────────────────────────── ITEM DETAIL ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(vm: MainViewModel, content: Content, itemId: Int, onBack: () -> Unit) {
    val item = content.itemsById[itemId] ?: return
    val speech = LocalSpeech.current
    val ttsState by speech.state.collectAsState()
    val favs by vm.favorites.collectAsState()
    val lang = currentLang()
    val cur = Money.curFor(lang)
    val loc = Money.localeFor(lang)
    val money = rememberMoney()
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val msg = stringResource(R.string.tts_unavailable)
    val installLabel = stringResource(R.string.install)
    val g = content.groupOf(item)

    DisposableEffect(itemId) { onDispose { speech.stop() } }
    LaunchedEffect(ttsState) {
        if (ttsState == SpeechController.State.UNAVAILABLE) {
            val r = snack.showSnackbar(msg, installLabel, duration = SnackbarDuration.Long)
            speech.clearUnavailable()
            if (r == SnackbarResult.ActionPerformed) {
                try { ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
            }
        }
    }
    val speaking = ttsState == SpeechController.State.SPEAKING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = {
                        if (speaking) speech.stop() else {
                            speech.rate = vm.prefs.ttsRate
                            speech.speak(
                                listOf(
                                    Money.speech(item.title, cur),
                                    "Nói đơn giản. " + Money.speech(item.simple, cur),
                                    "Chi phí. " + Money.speech(item.cost, cur),
                                    "Lợi ích. " + Money.speech(item.benefit, cur),
                                ).joinToString(". ").replace("..", ".")
                            )
                        }
                    }) {
                        Icon(
                            if (speaking) AppIcons.Stop else AppIcons.VolumeUp,
                            stringResource(if (speaking) R.string.stop_listening else R.string.listen),
                            tint = if (speaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    FavButton(itemId in favs, { vm.toggleFavorite(itemId) }, tint = MaterialTheme.colorScheme.onSurface)
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Box(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(groupGradient(g, dark))).padding(20.dp)
            ) {
                Column {
                    Text(content.groups[g].names.pick(lang).uppercase(), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                    Text(content.chapterById[item.chapter]?.names?.pick(lang).orEmpty(), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(money(item.title), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RankBadge(item.rank, 30)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.rank_label) + ": " + rankName(item.rank), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Section(stringResource(R.string.sec_simple), money(item.simple), MaterialTheme.colorScheme.tertiary)
                Section(stringResource(R.string.sec_cost), money(item.cost), MaterialTheme.colorScheme.secondary)
                Section(stringResource(R.string.sec_benefit), money(item.benefit), MaterialTheme.colorScheme.primary)
                if (cur != Cur.CNY && listOf(item.title, item.simple, item.cost, item.benefit).any { "⟦" in it }) {
                    val nf = NumberFormat.getNumberInstance(loc).apply { maximumFractionDigits = 4 }
                    Text(
                        stringResource(R.string.currency_note, "1 CNY ≈ ${nf.format(cur.perCny)} ${cur.code}, ${stringResource(R.string.fx_label)}"),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 4.dp, height = 20.dp).clip(RoundedCornerShape(2.dp)).background(accent))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ───────────────────────────── GUIDE ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    val steps: List<Triple<ImageVector, Int, Int>> = listOf(
        Triple(AppIcons.AutoAwesome, R.string.guide_1_t, R.string.guide_1_d),
        Triple(Icons.Filled.Search, R.string.guide_2_t, R.string.guide_2_d),
        Triple(Icons.Filled.Menu, R.string.guide_3_t, R.string.guide_3_d),
        Triple(AppIcons.VolumeUp, R.string.guide_4_t, R.string.guide_4_d),
        Triple(AppIcons.Payments, R.string.guide_5_t, R.string.guide_5_d),
        Triple(Icons.Filled.Favorite, R.string.guide_6_t, R.string.guide_6_d),
    )
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.guide), style = MaterialTheme.typography.titleLarge) }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            steps.forEachIndexed { i, (icon, t, d) ->
                Surface(
                    shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("${i + 1}. " + stringResource(t), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(d), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── SETTINGS ─────────────────────────────
/** tag: mã locale áp dụng cho app · key: khóa ngôn ngữ nội bộ (langKey) · label: tên ngôn ngữ này trong ngôn ngữ giao diện đang dùng. */
private class Lang(val tag: String, val key: String, val flag: String, val native: String, @StringRes val label: Int)

private val LANGUAGES = listOf(
    Lang("en", "en", "🇬🇧", "English", R.string.lang_en),
    Lang("zh-CN", "zh", "🇨🇳", "简体中文", R.string.lang_zh),
    Lang("es", "es", "🇪🇸", "Español", R.string.lang_es),
    Lang("pt-BR", "pt", "🇧🇷", "Português (Brasil)", R.string.lang_pt),
    Lang("ru", "ru", "🇷🇺", "Русский", R.string.lang_ru),
    Lang("ja", "ja", "🇯🇵", "日本語", R.string.lang_ja),
    Lang("ko", "ko", "🇰🇷", "한국어", R.string.lang_ko),
    Lang("vi", "vi", "🇻🇳", "Tiếng Việt", R.string.lang_vi),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, privacyRequired: Boolean, onPrivacyOptions: () -> Unit, onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    val speech = LocalSpeech.current
    var langTag by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags()) }
    var theme by remember { mutableStateOf(vm.prefs.theme) }
    var rate by remember { mutableFloatStateOf(vm.prefs.ttsRate) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge) }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsTitle(stringResource(R.string.language))
            // "🇬🇧 - English (Tiếng Anh)": tên bản ngữ + tên theo ngôn ngữ giao diện hiện tại (bỏ phần ngoặc nếu trùng ngôn ngữ đang dùng).
            val uiLang = currentLang()
            val rows = listOf("" to ("🌐 - " + stringResource(R.string.system_default))) + LANGUAGES.map { l ->
                l.tag to (
                    if (l.key == uiLang) "${l.flag} - ${l.native}"
                    else "${l.flag} - ${l.native} (${stringResource(l.label)})"
                )
            }
            rows.forEach { (tag, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                        langTag = tag
                        AppCompatDelegate.setApplicationLocales(
                            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
                        )
                    }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = langTag == tag, onClick = null, modifier = Modifier.padding(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            SettingsTitle(stringResource(R.string.theme))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to R.string.system_default, "light" to R.string.theme_light, "dark" to R.string.theme_dark).forEach { (k, label) ->
                    FilterChip(
                        selected = theme == k,
                        onClick = { theme = k; vm.prefs.theme = k; LiveBetterApp.applyNightMode(k) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            SettingsTitle(stringResource(R.string.tts_speed) + " · " + String.format(Locale.US, "%.1f×", rate))
            Slider(
                value = rate, onValueChange = { rate = it },
                onValueChangeFinished = { vm.prefs.ttsRate = rate; speech.rate = rate },
                valueRange = 0.5f..4f, steps = 13, // bước 0,25×; tối đa 4×
            )

            SettingsTitle(stringResource(R.string.privacy_policy))
            OutlinedButton(onClick = { uri.openUri(BuildConfig.PRIVACY_URL) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Lock, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.privacy_policy))
            }
            if (privacyRequired) OutlinedButton(onClick = onPrivacyOptions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privacy_options))
            }
            Spacer(Modifier.height(8.dp))
            Text("How To Live Better v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsTitle(text: String) = Text(
    text, Modifier.padding(top = 14.dp, bottom = 2.dp),
    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary,
)

// ───────────────────────────── ABOUT / CREDITS ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.about), style = MaterialTheme.typography.titleLarge) }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.about_intro), style = MaterialTheme.typography.bodyLarge)
            Surface(
                shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.about_attr), style = MaterialTheme.typography.titleMedium)
                    LinkRow(stringResource(R.string.about_original), "「高性价比人生指南」 · eternity4719\nhttps://github.com/eternity4719/HowToLiveBetter", "https://github.com/eternity4719/HowToLiveBetter")
                    LinkRow(stringResource(R.string.about_license), "CC BY 4.0\nhttps://creativecommons.org/licenses/by/4.0/", "https://creativecommons.org/licenses/by/4.0/")
                }
            }
            Text(stringResource(R.string.about_changes), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.content_note), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkRow(label: String, text: String, url: String) {
    val uri = LocalUriHandler.current
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(
            text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { uri.openUri(url) },
        )
    }
}
