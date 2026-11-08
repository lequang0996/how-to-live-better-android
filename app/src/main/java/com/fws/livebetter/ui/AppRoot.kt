package com.fws.livebetter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.fws.livebetter.*
import com.fws.livebetter.R
import kotlinx.coroutines.launch

@Composable
fun AppRoot(
    vm: MainViewModel, adsReady: Boolean, updateReady: Boolean, privacyRequired: Boolean,
    onRestartUpdate: () -> Unit, onPrivacyOptions: () -> Unit,
) {
    val content by vm.content.collectAsState()
    val c = content
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
                ModalDrawerSheet {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                        Box(
                            Modifier.fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Color(0xFF14213D), Color(0xFF2B4C8C))))
                                .statusBarsPadding().padding(24.dp)
                        ) {
                            Column {
                                Text("✦ " + stringResource(R.string.app_name), color = Color(0xFFE5BE6F), style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.stats, c.items.size, c.chapters.size, c.groups.size),
                                    color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        TopicTree(c, onChapter = { go("chapter/$it") }, modifier = Modifier.padding(horizontal = 12.dp))
                        HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 16.dp))
                        NavigationDrawerItem({ Text(stringResource(R.string.favorites)) }, false, { go("favorites") },
                            icon = { Icon(Icons.Filled.Favorite, null, tint = Heart) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                        NavigationDrawerItem({ Text(stringResource(R.string.guide)) }, false, { go("guide") },
                            icon = { Icon(AppIcons.MenuBook, null) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                        NavigationDrawerItem({ Text(stringResource(R.string.settings)) }, false, { go("settings") },
                            icon = { Icon(Icons.Filled.Settings, null) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                        NavigationDrawerItem({ Text(stringResource(R.string.about)) }, false, { go("about") },
                            icon = { Icon(Icons.Filled.Info, null) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
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
