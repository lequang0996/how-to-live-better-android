package com.fws.howtolivebetter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fws.howtolivebetter.*
import com.fws.howtolivebetter.R
import kotlinx.coroutines.launch

@Composable
fun AppRoot(
    vm: MainViewModel, adsReady: Boolean, updateReady: Boolean, privacyRequired: Boolean,
    onRestartUpdate: () -> Unit, onPrivacyOptions: () -> Unit,
) {
    val content by vm.content.collectAsState()
    val c = content
    val showLanguagePicker by vm.showLanguagePicker.collectAsState()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Khi có banner, banner đã chừa chỗ cho thanh điều hướng → nội dung phía trên không cộng thêm inset đáy.
        Box(Modifier.weight(1f).fillMaxWidth().then(if (adsReady) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)) {
            if (c == null) Text(
                "How To Live Better", Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary,
            )
            else Navigation(vm, c, updateReady, privacyRequired, onRestartUpdate, onPrivacyOptions)
        }
        if (adsReady) AdBanner(Modifier.navigationBarsPadding())
    }
    // Lần đầu mở app: hỏi ngôn ngữ trước khi dùng.
    if (showLanguagePicker) LanguagePickerDialog(onConfirm = vm::confirmLanguage)
}

@Composable
private fun Navigation(
    vm: MainViewModel, c: Content, updateReady: Boolean, privacyRequired: Boolean,
    onRestartUpdate: () -> Unit, onPrivacyOptions: () -> Unit,
) {
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val updMsg = stringResource(R.string.update_downloaded)
    val restart = stringResource(R.string.restart)
    val favs by vm.favorites.collectAsState()

    // In-App Update (Flexible): tải nền xong thì mời khởi động lại.
    LaunchedEffect(updateReady) {
        if (updateReady && snack.showSnackbar(updMsg, restart, duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) {
            onRestartUpdate()
        }
    }
    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    fun go(r: String) { scope.launch { drawer.close() }; nav.navigate(r) { launchSingleTop = true } }

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = route == "home" || drawer.isOpen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                        DrawerHeader(stringResource(R.string.stats, c.items.size, c.chapters.size, c.groups.size))
                        Spacer(Modifier.height(16.dp))
                        DrawerLabel(stringResource(R.string.browse))
                        TopicTree(c, onChapter = { go("chapter/$it") }, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(Modifier.height(20.dp))
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    DrawerAction(Icons.Filled.Favorite, Heart, stringResource(R.string.favorites), badge = favs.size) { go("favorites") }
                                    DrawerAction(AppIcons.MenuBook, Color(0xFF1E40AF), stringResource(R.string.guide)) { go("guide") }
                                    DrawerAction(Icons.Filled.Settings, Color(0xFF0F766E), stringResource(R.string.settings)) { go("settings") }
                                    DrawerAction(Icons.Filled.Info, Color(0xFFB45309), stringResource(R.string.about)) { go("about") }
                                }
                            }
                            Text(
                                "v${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth().padding(top = 16.dp),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            },
        ) {
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        vm, c,
                        onMenu = { scope.launch { drawer.open() } },
                        onItem = { nav.navigate("item/$it") },
                        onChapter = { nav.navigate("chapter/$it") },
                        onFavorites = { nav.navigate("favorites") },
                    )
                }
                composable("chapter/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
                    ChapterScreen(vm, c, it.arguments!!.getInt("id"), { nav.popBackStack() }, { id -> nav.navigate("item/$id") })
                }
                composable("item/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
                    ItemScreen(vm, c, it.arguments!!.getInt("id")) { nav.popBackStack() }
                }
                composable("favorites") { FavoritesScreen(vm, c, { nav.popBackStack() }, { id -> nav.navigate("item/$id") }) }
                composable("guide") { GuideScreen { nav.popBackStack() } }
                composable("settings") { SettingsScreen(vm, privacyRequired, onPrivacyOptions) { nav.popBackStack() } }
                composable("about") { AboutScreen { nav.popBackStack() } }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun DrawerHeader(stats: String) {
    val gold = Color(0xFFE5BE6F)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0E1A33), Color(0xFF1F3A68), Color(0xFF2B4C8C))))
    ) {
        // Vòng tròn trang trí mờ
        Box(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp).size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = 36.dp).size(90.dp).clip(CircleShape).background(gold.copy(alpha = 0.10f)))
        Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 26.dp)) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(gold.copy(alpha = 0.32f), gold.copy(alpha = 0.12f)))),
                contentAlignment = Alignment.Center,
            ) { Icon(AppIcons.AutoAwesome, null, Modifier.size(28.dp), tint = gold) }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
                Text(
                    stats, Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DrawerLabel(text: String) = Text(
    text.uppercase(), Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp,
)

@Composable
private fun DrawerAction(icon: ImageVector, tint: Color, label: String, badge: Int = 0, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(22.dp), tint = tint) }
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (badge > 0) {
            Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.16f)) {
                Text("$badge", Modifier.padding(horizontal = 9.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = tint)
            }
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
