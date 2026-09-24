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
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.retro.cassetteplayer.MainViewModel
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongCollection
import com.retro.cassetteplayer.ui.components.AddToPlaylistSheet
import com.retro.cassetteplayer.ui.components.MiniPlayer
import com.retro.cassetteplayer.ui.components.PlaylistNameDialog
import com.retro.cassetteplayer.ui.components.QueueSheet
import com.retro.cassetteplayer.ui.components.RetroBottomBar
import com.retro.cassetteplayer.ui.navigation.Routes
import com.retro.cassetteplayer.ui.screens.CollectionScreen
import com.retro.cassetteplayer.ui.screens.LibraryScreen
import com.retro.cassetteplayer.ui.screens.HomeScreen
import com.retro.cassetteplayer.ui.screens.PlayerScreen
import com.retro.cassetteplayer.ui.screens.SearchScreen
import com.retro.cassetteplayer.ui.theme.Ink

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
    val library by viewModel.library.collectAsStateWithLifecycle()

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
    // --- Playlists: "Salvar na playlist" sheet and the name dialog ---------------------
    var songsToSave by remember { mutableStateOf<List<Song>?>(null) }
    var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
    val saveToPlaylist: (List<Song>) -> Unit = { if (it.isNotEmpty()) songsToSave = it }
    val saveSong: (Song) -> Unit = { saveToPlaylist(listOf(it)) }

    songsToSave?.let { pending ->
        if (!creatingPlaylist) {
            AddToPlaylistSheet(
                playlists = library.userPlaylists,
                onSelect = { playlist ->
                    playlist.userPlaylistId?.let { viewModel.addToPlaylist(it, pending) }
                    songsToSave = null
                },
                onNewPlaylist = { creatingPlaylist = true },
                onDismiss = { songsToSave = null },
            )
        }
    }
    if (creatingPlaylist) {
        PlaylistNameDialog(
            title = "Nova playlist",
            confirmLabel = "Criar",
            onConfirm = { name ->
                viewModel.createPlaylist(name, songsToSave.orEmpty())
                songsToSave = null
                creatingPlaylist = false
            },
            onDismiss = { creatingPlaylist = false },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    var showQueue by rememberSaveable { mutableStateOf(false) }
    if (showQueue) {
        QueueSheet(
            playback = playback,
            onDismiss = { showQueue = false },
            onPlayItem = viewModel::playQueueItem,
            onRemoveItem = viewModel::removeQueueItem,
        )
    }

    val openPlayer = { navController.navigate(Routes.PLAYER) { launchSingleTop = true } }
    val openTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openCollection: (SongCollection) -> Unit = { collection ->
        navController.navigate(Routes.collection(collection.id))
    }

    // The bars sit under the NavHost: tab screens keep a fixed bottom gap for them, while
    // the full-screen player is drawn on top. Nothing resizes while the player slides in/out.
    val density = LocalDensity.current
    var barsHeight by remember { mutableStateOf(0.dp) }
    val tabContent: @Composable (@Composable () -> Unit) -> Unit = { content ->
        Box(Modifier.padding(bottom = barsHeight)) { content() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        AnimatedVisibility(
            visible = currentRoute != Routes.PLAYER,
            enter = slideInVertically(tween(PLAYER_ANIM_MS)) { it },
            exit = slideOutVertically(tween(PLAYER_ANIM_MS)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(Modifier.onSizeChanged { barsHeight = with(density) { it.height.toDp() } }) {
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
                    onSelect = openTab,
                )
            }
        }

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
            exitTransition = {
                // Keep the screen fully visible underneath while the player slides over it.
                if (targetState.destination.route == Routes.PLAYER) fadeOut(snap(PLAYER_ANIM_MS))
                else fadeOut(tween(TAB_FADE_MS))
            },
            popEnterTransition = {
                if (initialState.destination.route == Routes.PLAYER) fadeIn(snap())
                else fadeIn(tween(TAB_FADE_MS))
            },
            popExitTransition = { fadeOut(tween(TAB_FADE_MS)) },
        ) {
            composable(Routes.HOME) {
                tabContent {
                    HomeScreen(
                        songs = songs,
                        albums = library.albums,
                        playlists = library.playlists,
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
                        onOpenSearch = { openTab(Routes.SEARCH) },
                        onOpenLibrary = { openTab(Routes.LIBRARY) },
                        onOpenCollection = openCollection,
                        onPlayAll = viewModel::playAll,
                        onSongClick = viewModel::play,
                        onPlayNext = viewModel::playNext,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = saveSong,
                    )
                }
            }
            composable(Routes.SEARCH) {
                tabContent {
                    SearchScreen(
                        query = query,
                        onQueryChange = viewModel::onQueryChange,
                        results = searchResults,
                        playback = playback,
                        onSongClick = { song -> viewModel.play(searchResults, song) },
                        onPlayNext = viewModel::playNext,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = saveSong,
                    )
                }
            }
            composable(Routes.LIBRARY) {
                tabContent {
                    LibraryScreen(
                        songs = songs,
                        library = library,
                        playback = playback,
                        onOpenSearch = { openTab(Routes.SEARCH) },
                        onOpenCollection = openCollection,
                        onShufflePlay = viewModel::shufflePlay,
                        onSongClick = viewModel::play,
                        onPlayNext = viewModel::playNext,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = saveSong,
                        onCreatePlaylist = { creatingPlaylist = true },
                    )
                }
            }
            composable(
                route = Routes.COLLECTION,
                arguments = listOf(navArgument(Routes.COLLECTION_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val id = entry.arguments?.getString(Routes.COLLECTION_ARG).orEmpty()
                tabContent {
                    CollectionScreen(
                        collection = library.find(id),
                        playback = playback,
                        onBack = { navController.popBackStack() },
                        onPlayAll = viewModel::playAll,
                        onShufflePlay = viewModel::shufflePlay,
                        onSongClick = viewModel::play,
                        onPlayNext = viewModel::playNext,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = saveSong,
                        onSaveAll = saveToPlaylist,
                        onRenamePlaylist = viewModel::renamePlaylist,
                        onDeletePlaylist = viewModel::deletePlaylist,
                        onRemoveFromPlaylist = viewModel::removeFromPlaylist,
                        onReorderPlaylist = viewModel::reorderPlaylist,
                    )
                }
            }
            composable(
                route = Routes.PLAYER,
                enterTransition = { slideInVertically(tween(PLAYER_ANIM_MS)) { it } },
                exitTransition = { slideOutVertically(tween(PLAYER_ANIM_MS)) { it } },
                popExitTransition = { slideOutVertically(tween(PLAYER_ANIM_MS)) { it } },
            ) {
                PlayerScreen(
                    playback = playback,
                    onBack = { navController.popBackStack() },
                    onTogglePlay = viewModel::togglePlayPause,
                    onPrevious = viewModel::skipPrevious,
                    onNext = viewModel::skipNext,
                    onSeek = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeat,
                    onOpenQueue = { showQueue = true },
                    onSaveToPlaylist = {
                        songs.firstOrNull { it.id.toString() == playback.mediaId }?.let(saveSong)
                    },
                )
            }
        }

        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = barsHeight),
        )
    }
}

private const val PLAYER_ANIM_MS = 350
private const val TAB_FADE_MS = 200
