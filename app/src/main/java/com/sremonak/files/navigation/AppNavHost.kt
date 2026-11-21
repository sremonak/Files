package com.sremonak.files.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sremonak.files.FilesApp
import com.sremonak.files.R
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.ui.screens.analyzer.AnalyzerScreen
import com.sremonak.files.ui.screens.browse.BrowseScreen
import com.sremonak.files.ui.screens.details.FileDetailsScreen
import com.sremonak.files.ui.screens.editor.TextEditorScreen
import com.sremonak.files.ui.screens.home.HomeScreen
import com.sremonak.files.ui.screens.lock.LockScreen
import com.sremonak.files.ui.screens.onboarding.PermissionScreen
import com.sremonak.files.ui.screens.recycle.RecycleBinScreen
import com.sremonak.files.ui.screens.search.SearchScreen
import com.sremonak.files.ui.screens.settings.SettingsScreen
import com.sremonak.files.util.PermissionUtils
import kotlinx.coroutines.flow.map

private val TAB_ROUTES = setOf("home", "browse", "search", "settings")

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    val settings = FilesApp.instance.settings
    val navController = rememberNavController()

    var hasAccess by remember { mutableStateOf(PermissionUtils.hasAllFilesAccess(context)) }
    val lockEnabled by remember(settings) { settings.lockEnabled.map { it as Boolean? } }
        .collectAsStateWithLifecycle(initialValue = null)
    var unlocked by remember { mutableStateOf(false) }

    // Re-check permission whenever the app returns to foreground.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = PermissionUtils.hasAllFilesAccess(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (!hasAccess) {
        PermissionScreen(onGranted = { hasAccess = PermissionUtils.hasAllFilesAccess(context) })
        return
    }

    if (lockEnabled == null) return

    if (lockEnabled == true && !unlocked) {
        LockScreen(onUnlocked = { unlocked = true })
        return
    }

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val showBottomBar = currentRoute?.substringBefore("?") in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute?.startsWith("home") == true,
                        onClick = { navController.navigate("home") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute?.startsWith("browse") == true,
                        onClick = { navController.navigate("browse") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_browse)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute?.startsWith("search") == true,
                        onClick = { navController.navigate("search") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_search)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute?.startsWith("settings") == true,
                        onClick = { navController.navigate("settings") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onOpenBrowse = { path -> navController.navigate("browse?path=${Uri.encode(path)}") },
                    onOpenCategory = { category -> navController.navigate("category/${category.name}") },
                    onOpenDetails = { path -> navController.navigate("details/${Uri.encode(path)}") },
                    onOpenTrash = { navController.navigate("trash") },
                    onOpenAnalyzer = { navController.navigate("analyzer") }
                )
            }
            composable(
                route = "browse?path={path}",
                arguments = listOf(navArgument("path") { defaultValue = "" })
            ) { entry ->
                val path = entry.arguments?.getString("path") ?: ""
                BrowseScreen(
                    initialPath = path,
                    onOpenDetails = { p -> navController.navigate("details/${Uri.encode(p)}") },
                    onOpenEditor = { p -> navController.navigate("editor/${Uri.encode(p)}") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("search") {
                SearchScreen(
                    onOpenDetails = { p -> navController.navigate("details/${Uri.encode(p)}") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onOpenTrash = { navController.navigate("trash") },
                    onOpenAnalyzer = { navController.navigate("analyzer") }
                )
            }
            composable("category/{category}") { entry ->
                val category = runCatching {
                    FileCategory.valueOf(entry.arguments?.getString("category") ?: "OTHER")
                }.getOrDefault(FileCategory.OTHER)
                BrowseScreen(
                    initialPath = "",
                    categoryFilter = category,
                    onOpenDetails = { p -> navController.navigate("details/${Uri.encode(p)}") },
                    onOpenEditor = { p -> navController.navigate("editor/${Uri.encode(p)}") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "details/{path}",
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { entry ->
                val path = entry.arguments?.getString("path") ?: ""
                FileDetailsScreen(
                    path = path,
                    onOpenEditor = { p -> navController.navigate("editor/${Uri.encode(p)}") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "editor/{path}",
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { entry ->
                val path = entry.arguments?.getString("path") ?: ""
                TextEditorScreen(path = path, onBack = { navController.popBackStack() })
            }
            composable("trash") {
                RecycleBinScreen(onBack = { navController.popBackStack() })
            }
            composable("analyzer") {
                AnalyzerScreen(
                    onOpenDetails = { p -> navController.navigate("details/${Uri.encode(p)}") },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
