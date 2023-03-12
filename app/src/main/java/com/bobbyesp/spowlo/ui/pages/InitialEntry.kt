package com.bobbyesp.spowlo.ui.pages

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.bobbyesp.library.SpotDL
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.BuildConfig
import com.bobbyesp.spowlo.MainActivity
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.mod_downloader.data.remote.ModsDownloaderAPI
import com.bobbyesp.spowlo.features.spotify_api.SpotifyApiRequests
import com.bobbyesp.spowlo.ui.common.LocalWindowWidthState
import com.bobbyesp.spowlo.ui.common.Route
import com.bobbyesp.spowlo.ui.common.animatedComposable
import com.bobbyesp.spowlo.ui.common.animatedComposableVariant
import com.bobbyesp.spowlo.ui.common.slideInVerticallyComposable
import com.bobbyesp.spowlo.ui.dialogs.UpdaterBottomDrawer
import com.bobbyesp.spowlo.ui.dialogs.bottomsheets.MoreOptionsHomeBottomSheet
import com.bobbyesp.spowlo.ui.pages.downloader.DownloaderPage
import com.bobbyesp.spowlo.ui.pages.downloader.DownloaderViewModel
import com.bobbyesp.spowlo.ui.pages.history.DownloadsHistoryPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.PlaylistPage
import com.bobbyesp.spowlo.ui.pages.mod_downloader.ModsDownloaderPage
import com.bobbyesp.spowlo.ui.pages.mod_downloader.ModsDownloaderViewModel
import com.bobbyesp.spowlo.ui.pages.playlist.PlaylistMetadataPage
import com.bobbyesp.spowlo.ui.pages.searcher.SearcherPage
import com.bobbyesp.spowlo.ui.pages.settings.SettingsPage
import com.bobbyesp.spowlo.ui.pages.settings.about.AboutPage
import com.bobbyesp.spowlo.ui.pages.settings.appearance.AppThemePreferencesPage
import com.bobbyesp.spowlo.ui.pages.settings.appearance.AppearancePage
import com.bobbyesp.spowlo.ui.pages.settings.appearance.LanguagePage
import com.bobbyesp.spowlo.ui.pages.settings.cookies.CookieProfilePage
import com.bobbyesp.spowlo.ui.pages.settings.cookies.CookiesSettingsViewModel
import com.bobbyesp.spowlo.ui.pages.settings.cookies.WebViewPage
import com.bobbyesp.spowlo.ui.pages.settings.directories.DownloadsDirectoriesPage
import com.bobbyesp.spowlo.ui.pages.settings.documentation.DocumentationPage
import com.bobbyesp.spowlo.ui.pages.settings.format.AudioQualityDialog
import com.bobbyesp.spowlo.ui.pages.settings.format.SettingsFormatsPage
import com.bobbyesp.spowlo.ui.pages.settings.general.GeneralSettingsPage
import com.bobbyesp.spowlo.ui.pages.settings.spotify.SpotifySettingsPage
import com.bobbyesp.spowlo.ui.pages.settings.updater.UpdaterPage
import com.bobbyesp.spowlo.utils.PreferencesUtil.getBoolean
import com.bobbyesp.spowlo.utils.PreferencesUtil.getString
import com.bobbyesp.spowlo.utils.SPOTDL
import com.bobbyesp.spowlo.utils.SPOTDL_UPDATE
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.UpdateUtil
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "InitialEntry"

@OptIn(
    ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun InitialEntry(
    downloaderViewModel: DownloaderViewModel,
    modsDownloaderViewModel: ModsDownloaderViewModel,
    isUrlShared: Boolean
) {
    //bottom sheet remember state
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberAnimatedNavController(bottomSheetNavigator)
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRootRoute = remember(navBackStackEntry) {
        mutableStateOf(
            navBackStackEntry?.destination?.parent?.route ?: Route.DownloaderNavi
        )
    }

    //navController.currentBackStack.value.getOrNull(1)?.destination?.route
    val shouldHideBottomNavBar = remember(navBackStackEntry) {
        navBackStackEntry?.destination?.hierarchy?.any { it.route == Route.SPOTIFY_SETUP } == true
    }

    val isLandscape = remember { MutableTransitionState(false) }

    val windowWidthState = LocalWindowWidthState.current

    LaunchedEffect(windowWidthState) {
        isLandscape.targetState = windowWidthState == WindowWidthSizeClass.Expanded
    }

    val context = LocalContext.current
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var currentDownloadStatus by remember { mutableStateOf(UpdateUtil.DownloadStatus.NotYet as UpdateUtil.DownloadStatus) }
    val scope = rememberCoroutineScope()
    var updateJob: Job? = null
    var latestRelease by remember { mutableStateOf(UpdateUtil.LatestRelease()) }
    val settings =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            UpdateUtil.installLatestApk()
        }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            UpdateUtil.installLatestApk()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls())
                    settings.launch(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}"),
                        )
                    )
                else
                    UpdateUtil.installLatestApk()
            }
        }
    }

    val cookiesViewModel: CookiesSettingsViewModel = viewModel()
    val onBackPressed: () -> Unit = { navController.popBackStack() }

    if (isUrlShared) {
        if (navController.currentDestination?.route != Route.HOME) {
            navController.popBackStack(route = Route.HOME, inclusive = false, saveState = true)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val navRootUrl = "android-app://androidx.navigation/"
        ModalBottomSheetLayout(
            bottomSheetNavigator,
            sheetShape = MaterialTheme.shapes.medium.copy(
                bottomStart = CornerSize(0.dp),
                bottomEnd = CornerSize(0.dp)
            ),
            scrimColor = MaterialTheme.colorScheme.scrim.copy(0.5f),
            sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        ) {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = !shouldHideBottomNavBar,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        NavigationBar(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                                .navigationBarsPadding(),
                        ) {
                            MainActivity.showInBottomNavigation.forEach { (route, icon) ->
                                val text = when (route) {
                                    Route.DownloaderNavi -> App.context.getString(R.string.downloader)
                                    Route.SearcherNavi -> App.context.getString(R.string.searcher)
                                    else -> ""
                                }

                                val selected = currentRootRoute.value == route

                                val onClick = remember(selected, navController, route) {
                                    {
                                        if (!selected) {
                                            navController.navigate(route) {
                                                popUpTo(Route.NavGraph) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                                NavigationBarItem(
                                    selected = currentRootRoute.value == route,
                                    onClick = onClick,
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    })
                            }
                        }
                    }
                }, modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) { paddingValues ->
                AnimatedNavHost(
                    modifier = Modifier
                        .fillMaxWidth(
                            when (LocalWindowWidthState.current) {
                                WindowWidthSizeClass.Compact -> 1f
                                WindowWidthSizeClass.Expanded -> 1f
                                else -> 0.8f
                            }
                        )
                        .align(Alignment.Center)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    navController = navController,
                    startDestination = Route.DownloaderNavi,
                    route = Route.NavGraph
                ) {
                    navigation(startDestination = Route.HOME, route = Route.DownloaderNavi) {
                        //TODO: Add all routes
                        animatedComposable(Route.HOME) { //TODO: Change this route to Route.DOWNLOADER, but by now, keep it as Route.HOME
                            DownloaderPage(
                                navigateToDownloads = { navController.navigate(Route.DOWNLOADS_HISTORY) },
                                navigateToSettings = { navController.navigate(Route.MORE_OPTIONS_HOME) },
                                navigateToPlaylistPage = { navController.navigate(Route.PLAYLIST) },
                                onSongCardClicked = {
                                    navController.navigate(Route.PLAYLIST_METADATA_PAGE)
                                },
                                onNavigateToTaskList = { navController.navigate(Route.TASK_LIST) },
                                navigateToMods = { navController.navigate(Route.MODS_DOWNLOADER) },
                                downloaderViewModel = downloaderViewModel
                            )
                        }
                        animatedComposable(Route.SETTINGS) {
                            SettingsPage(
                                navController = navController
                            )
                        }
                        animatedComposable(Route.GENERAL_DOWNLOAD_PREFERENCES) {
                            GeneralSettingsPage(
                                onBackPressed = onBackPressed
                            )
                        }
                        animatedComposable(Route.DOWNLOADS_HISTORY) {
                            DownloadsHistoryPage(
                                onBackPressed = onBackPressed,
                            )
                        }
                        animatedComposable(Route.DOWNLOAD_DIRECTORY) {
                            DownloadsDirectoriesPage {
                                onBackPressed()
                            }
                        }
                        animatedComposable(Route.APPEARANCE) {
                            AppearancePage(navController = navController)
                        }
                        animatedComposable(Route.APP_THEME) {
                            AppThemePreferencesPage {
                                onBackPressed()
                            }
                        }
                        animatedComposable(Route.DOWNLOAD_FORMAT) {
                            SettingsFormatsPage {
                                onBackPressed()
                            }
                        }
                        animatedComposable(Route.SPOTIFY_PREFERENCES) {
                            SpotifySettingsPage {
                                onBackPressed()
                            }
                        }
                        slideInVerticallyComposable(Route.PLAYLIST_METADATA_PAGE) {
                            PlaylistMetadataPage(
                                onBackPressed,
                                //TODO: ADD THE ABILITY TO PASS JUST SONGS AND NOT GET THEM FROM THE MUTABLE STATE
                            )
                        }
                        animatedComposable(Route.MODS_DOWNLOADER) {
                            ModsDownloaderPage(
                                onBackPressed,
                                modsDownloaderViewModel
                            )
                        }
                        animatedComposable(Route.COOKIE_PROFILE) {
                            CookieProfilePage(
                                cookiesViewModel = cookiesViewModel,
                                navigateToCookieGeneratorPage = { navController.navigate(Route.COOKIE_GENERATOR_WEBVIEW) },
                            ) { onBackPressed() }
                        }
                        animatedComposable(
                            Route.COOKIE_GENERATOR_WEBVIEW
                        ) {
                            WebViewPage(cookiesViewModel) { onBackPressed() }
                        }
                        animatedComposable(Route.UPDATER_PAGE) {
                            UpdaterPage(
                                onBackPressed
                            )
                        }
                        animatedComposable(Route.DOCUMENTATION) {
                            DocumentationPage(
                                onBackPressed,
                                navController
                            )
                        }

                        animatedComposable(Route.ABOUT) {
                            AboutPage {
                                onBackPressed()
                            }
                        }

                        animatedComposable(Route.LANGUAGES) {
                            LanguagePage {
                                onBackPressed()
                            }
                        }


                        navDeepLink {
                            // Want to go to "markdown_viewer/{markdownFileName}"
                            uriPattern =
                                "android-app://androidx.navigation/markdown_viewer/{markdownFileName}"
                        }

                        animatedComposable(
                            "markdown_viewer/{markdownFileName}",
                            arguments = listOf(
                                navArgument(
                                    "markdownFileName"
                                ) {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val mdFileName =
                                backStackEntry.arguments?.getString("markdownFileName") ?: ""
                            Log.d("MainActivity", mdFileName)
                            MarkdownViewerPage(
                                markdownFileName = mdFileName,
                                onBackPressed = onBackPressed
                            )
                        }

                        //DIALOGS -------------------------------
                        //TODO: ADD DIALOGS
                        dialog(Route.AUDIO_QUALITY_DIALOG) {
                            AudioQualityDialog(
                                onBackPressed
                            )
                        }

                        //BOTTOM SHEETS --------------------------
                        bottomSheet(Route.MORE_OPTIONS_HOME) {
                            MoreOptionsHomeBottomSheet(
                                onBackPressed,
                                navController
                            )
                        }
                    }

                    //Can add the downloads history bottom sheet here using `val downloadsHistoryViewModel = hiltViewModel()`
                    navigation(startDestination = Route.SEARCHER, route = Route.SearcherNavi) {
                        animatedComposableVariant(Route.SEARCHER) {
                            SearcherPage(
                                navController = navController
                            )
                        }


                        //create a deeplink to the playlist page passing the id of the playlist
                        navDeepLink {
                            // Want to go to "markdown_viewer/{markdownFileName}"
                            uriPattern =
                                StringBuilder().append(navRootUrl).append(Route.PLAYLIST_PAGE)
                                    .append("/{id}").toString()
                            Log.d("TST_NAV", uriPattern!!)
                        }

                        val navArgument = navArgument("id") {
                            type = NavType.StringType
                        }
                        val routeWithIdPattern: String =
                            StringBuilder().append(Route.PLAYLIST_PAGE).append("/{id}").toString()
                        animatedComposableVariant(
                            routeWithIdPattern,
                            arguments = listOf(navArgument)
                        ) { backStackEntry ->
                            val id =
                                backStackEntry.arguments?.getString("id") ?: "SOMETHING WENT WRONG"
                            PlaylistPage(
                                onBackPressed,
                                id = id
                            )
                        }


                    }
                }
            }
        }
    }

    if (BuildConfig.DEBUG) LaunchedEffect(Unit) {
        runCatching {
            SpotifyApiRequests.buildApi()
        }.onFailure {
            it.printStackTrace()
            ToastUtil.makeToastSuspend(context.getString(R.string.spotify_api_error))
        }.onSuccess {
            val req = SpotifyApiRequests.trackSearch("Faded Alan Walker")
            Log.d("InitialEntry", "Name:" + req.tracks!![0].name)
            Log.d("InitialEntry", "Artist:" + req.tracks!![0].artists[0].name)
            Log.d("InitialEntry", "Album:" + req.tracks!![0].album.name)
            Log.d("InitialEntry", "Album Image:" + req.tracks!![0].album.images[0].url)
            Log.d("InitialEntry", "Duration:" + req.tracks!![0].durationMs)
            Log.d("InitialEntry", "Popularity:" + req.tracks!![0].popularity)
            Log.d("InitialEntry", "-------------------------------------------")
            Log.d("InitialEntry", "Full response: $req")
        }
    }

    LaunchedEffect(Unit) {
        if (!SPOTDL_UPDATE.getBoolean()) return@LaunchedEffect
        runCatching {
            withContext(Dispatchers.IO) {
                val res = UpdateUtil.updateSpotDL()
                if (res == SpotDL.UpdateStatus.DONE) {
                    ToastUtil.makeToastSuspend(context.getString(R.string.spotDl_uptodate) + " (${SPOTDL.getString()})")
                }
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            runCatching {
                //TODO: Add check for updates of spotDL
                UpdateUtil.checkForUpdate()?.let {
                    latestRelease = it
                    showUpdateDialog = true
                }
                if (showUpdateDialog) {
                    UpdateUtil.showUpdateDrawer()
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "InitialEntry: Checking for updates")
        ModsDownloaderAPI.callModsAPI().onFailure {
            ToastUtil.makeToastSuspend(App.context.getString(R.string.api_call_failed))
        }.onSuccess {
            modsDownloaderViewModel.updateApiResponse(it)
        }
    }

    if (showUpdateDialog) {
        /*UpdateDialogImpl(
            onDismissRequest = {
                showUpdateDialog = false
                updateJob?.cancel()
            },
            title = latestRelease.name.toString(),
            onConfirmUpdate = {
                updateJob = scope.launch(Dispatchers.IO) {
                    runCatching {
                        UpdateUtil.downloadApk(latestRelease = latestRelease)
                            .collect { downloadStatus ->
                                currentDownloadStatus = downloadStatus
                                if (downloadStatus is UpdateUtil.DownloadStatus.Finished) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        launcher.launch(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                                    }
                                }
                            }
                    }.onFailure {
                        it.printStackTrace()
                        currentDownloadStatus = UpdateUtil.DownloadStatus.NotYet
                        ToastUtil.makeToastSuspend(context.getString(R.string.app_update_failed))
                        return@launch
                    }
                }
            },
            releaseNote = latestRelease.body.toString(),
            downloadStatus = currentDownloadStatus
        )*/
        UpdaterBottomDrawer(latestRelease = latestRelease)
    }

}

@OptIn(ExperimentalAnimationApi::class)
private fun buildAnimationForward(scope: AnimatedContentScope<NavBackStackEntry>): Boolean {
    val isRoute = getStartingRoute(scope.initialState.destination)
    val tsRoute = getStartingRoute(scope.targetState.destination)

    val isIndex = MainActivity.showInBottomNavigation.keys.indexOfFirst { it == isRoute }
    val tsIndex = MainActivity.showInBottomNavigation.keys.indexOfFirst { it == tsRoute }

    return tsIndex == -1 || isRoute == tsRoute || tsIndex > isIndex
}

private fun getStartingRoute(destination: NavDestination): String {
    return destination.hierarchy.toList().let { it[it.lastIndex - 1] }.route.orEmpty()
}

//TODO: Separate the SettingsPage into a different NavGraph (like Seal)