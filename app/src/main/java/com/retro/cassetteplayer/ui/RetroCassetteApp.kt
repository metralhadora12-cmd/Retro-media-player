package com.retro.cassetteplayer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.retro.cassetteplayer.MainViewModel
import com.retro.cassetteplayer.ui.components.MiniPlayer
import com.retro.cassetteplayer.ui.components.RetroBottomBar
import com.retro.cassetteplayer.ui.navigation.Routes
import com.retro.cassetteplayer.ui.screens.CollectionsScreen
import com.retro.cassetteplayer.ui.screens.HomeScreen
import com.retro.cassetteplayer.ui.screens.PlayerScreen
import com.retro.cassetteplayer.ui.screens.SearchScreen
import com.retro.cassetteplayer.ui.theme.Charcoal

private val audioPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private val requestedPermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun Context.hasAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED

@Composable
fun RetroCassetteApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    // --- Permissions (READ_MEDIA_AUDIO on Android 13+) ----------------------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onPermissionResult(result[audioPermission] == true || context.hasAudioPermission())
    }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (context.hasAudioPermission()) {
            viewModel.onPermissionResult(true)
        } else if (!askedOnce) {
            askedOnce = true
            permissionLauncher.launch(requestedPermissions)
        }
    }
    // The user may grant access from the system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasPermission != true && context.hasAudioPermission()) viewModel.onPermissionResult(true)
    }

    // --- Navigation ------------------------------------------------------------------
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val openPlayer = { navController.navigate(Routes.PLAYER) { launchSingleTop = true } }

    Scaffold(
        containerColor = Charcoal,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute != Routes.PLAYER,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Column {
                    if (playback.hasMedia) {
                        MiniPlayer(
                            state = playback,
                            onOpen = openPlayer,
                            onTogglePlay = viewModel::togglePlayPause,
                            onNext = viewModel::skipNext,
                        )
                    }
                    RetroBottomBar(
                        destinations = Routes.bottomDestinations,
                        currentRoute = currentRoute,
                        onSelect = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    songs = songs,
                    playback = playback,
                    isLoading = isLoading,
                    hasPermission = hasPermission,
                    onRequestPermission = { permissionLauncher.launch(requestedPermissions) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                        )
                    },
                    onReload = viewModel::loadSongs,
                    onShufflePlay = { viewModel.shufflePlay() },
                    onSongClick = { song -> viewModel.play(songs, song) },
                    onPlayNext = viewModel::playNext,
                    onAddToQueue = viewModel::addToQueue,
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    results = searchResults,
                    playback = playback,
                    onSongClick = { song -> viewModel.play(searchResults, song) },
                    onPlayNext = viewModel::playNext,
                    onAddToQueue = viewModel::addToQueue,
                )
            }
            composable(Routes.COLLECTIONS) {
                CollectionsScreen(
                    albums = albums,
                    onPlayAlbum = { album -> viewModel.play(album.songs, album.songs.first()) },
                    onShuffleAlbum = { album -> viewModel.shufflePlay(album.songs) },
                )
            }
            composable(
                route = Routes.PLAYER,
                enterTransition = { slideInVertically { it } },
                exitTransition = { slideOutVertically { it } },
                popExitTransition = { slideOutVertically { it } },
            ) {
                PlayerScreen(
                    playback = playback,
                    onBack = { navController.popBackStack() },
                    onTogglePlay = viewModel::togglePlayPause,
                    onPrevious = viewModel::skipPrevious,
                    onNext = viewModel::skipNext,
                    onRewind = viewModel::rewind,
                    onFastForward = viewModel::fastForward,
                    onSeek = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                )
            }
        }
    }
}
