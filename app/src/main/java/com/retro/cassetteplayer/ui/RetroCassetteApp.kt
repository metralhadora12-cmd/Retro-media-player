package com.retro.cassetteplayer.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.navigation.NavHostController
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
import com.retro.cassetteplayer.ui.components.FavoritesState
import com.retro.cassetteplayer.ui.components.LocalDeleteSong
import com.retro.cassetteplayer.ui.components.LocalFavorites
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
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.playback.EqualizerManager
import com.retro.cassetteplayer.ui.components.titleLabel
import com.retro.cassetteplayer.ui.screens.ChangelogScreen
import com.retro.cassetteplayer.ui.screens.EqualizerScreen
import com.retro.cassetteplayer.ui.screens.SettingsScreen
import com.retro.cassetteplayer.UiMessage
import com.retro.cassetteplayer.ui.components.LocalEditSong
import com.retro.cassetteplayer.ui.screens.EditTagsScreen
import com.retro.cassetteplayer.ui.screens.StatusMessage
import com.retro.cassetteplayer.LyricsState
import com.retro.cassetteplayer.ui.components.LyricsSheet
import com.retro.cassetteplayer.ui.components.LocalRenameSong
import com.retro.cassetteplayer.ui.components.RenameSongDialog
import androidx.compose.runtime.rememberCoroutineScope
import com.retro.cassetteplayer.data.AppSettings
import com.retro.cassetteplayer.data.PlayStats
import com.retro.cassetteplayer.playback.SleepTimer
import com.retro.cassetteplayer.ui.components.SleepTimerSheet
import com.retro.cassetteplayer.ui.components.KeyClick
import com.retro.cassetteplayer.ui.components.SpeedSheet
import com.retro.cassetteplayer.ui.components.displayName
import com.retro.cassetteplayer.ui.screens.StatsScreen
import kotlinx.coroutines.launch

private val audioPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private val requestedPermissions: Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    // Android 8-9 also need write access to delete songs
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val tapeName by viewModel.tapeName.collectAsStateWithLifecycle()
    val sleepTimer by SleepTimer.state.collectAsStateWithLifecycle()
    val lyricsIndex by viewModel.lyricsIndex.collectAsStateWithLifecycle()
    val lyricsDownload by viewModel.lyricsDownload.collectAsStateWithLifecycle()
    val lyricsQuery by viewModel.lyricsQuery.collectAsStateWithLifecycle()
    val lyricsResults by viewModel.lyricsResults.collectAsStateWithLifecycle()
    val speed by AppSettings.speed.flow.collectAsStateWithLifecycle()
    val pitch by AppSettings.pitch.flow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Load the key sounds as soon as the option is on, so the first press already clacks
    LaunchedEffect(Unit) {
        AppSettings.keyClicks.flow.collect { if (it) KeyClick.preload(context) }
    }
    val shareMixtape: (String, List<Song>) -> Unit = { name, tracks ->
        scope.launch {
            if (!Mixtape.share(context, name, tracks)) viewModel.showMessage(UiMessage.Text(R.string.msg_share_failed))
        }
    }
    val playCollection: (SongCollection, Boolean) -> Unit = { collection, shuffle ->
        val name = collection.displayName(context)
        if (shuffle) viewModel.shufflePlay(collection.songs, name) else viewModel.playAll(collection.songs, name)
    }

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
    val currentTab = remember(backStackEntry) { navController.currentTabRoute() }
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
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.action_create),
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
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    // --- Lyrics sheet ("LETRA"): follows the current song while open ---------------
    val lyricsState by viewModel.lyrics.collectAsStateWithLifecycle()
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    val currentSong = remember(songs, playback.mediaId) { songs.firstOrNull { it.id.toString() == playback.mediaId } }
    LaunchedEffect(showLyrics, currentSong?.id) {
        if (showLyrics) currentSong?.let { viewModel.requestLyrics(it) }
    }
    if (showLyrics) {
        LyricsSheet(
            title = titleLabel(playback.title),
            state = lyricsState.takeIf { it.songId == currentSong?.id } ?: LyricsState.Idle,
            positionMs = playback.positionMs,
            onSeek = viewModel::seekTo,
            onRetry = { currentSong?.let { viewModel.requestLyrics(it, forceRefresh = true) } },
            onDismiss = { showLyrics = false },
        )
    }

    var showSleepTimer by rememberSaveable { mutableStateOf(false) }
    if (showSleepTimer) {
        SleepTimerSheet(
            state = sleepTimer,
            onStart = { viewModel.startSleepTimer(it); showSleepTimer = false },
            onEndOfTrack = { viewModel.sleepAtEndOfTrack(); showSleepTimer = false },
            onCancel = { viewModel.cancelSleepTimer(); showSleepTimer = false },
            onDismiss = { showSleepTimer = false },
        )
    }
    var showSpeed by rememberSaveable { mutableStateOf(false) }
    if (showSpeed) {
        SpeedSheet(
            speed = speed,
            pitch = pitch,
            onSpeedChange = { AppSettings.speed.set(it) },
            onPitchChange = { AppSettings.pitch.set(it) },
            onDismiss = { showSpeed = false },
        )
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
        when {
            // Início always goes back to the home screen itself (never to a saved album page).
            route == Routes.HOME -> navController.popBackStack(Routes.HOME, inclusive = false)
            // Reselecting the tab you're in returns to its root (e.g. album -> Biblioteca).
            route == navController.currentTabRoute() -> navController.popBackStack(route, inclusive = false)
            else -> navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
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

    // --- Deleting songs: system confirmation (Android 10+) or our own dialog (8-9) -------
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteConfirmation(result.resultCode == Activity.RESULT_OK)
    }
    var songToConfirmDelete by remember { mutableStateOf<Song?>(null) }
    val startDelete: (Song) -> Unit = { song ->
        viewModel.requestDelete(song)?.let { sender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }
    val deleteSong: (Song) -> Unit = { song ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startDelete(song) else songToConfirmDelete = song
    }
    songToConfirmDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songToConfirmDelete = null },
            containerColor = InkSurface,
            title = { Text(stringResource(R.string.delete_song_title), color = TextPrimary) },
            text = { Text(stringResource(R.string.delete_song_message, titleLabel(song.title)), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    songToConfirmDelete = null
                    startDelete(song)
                }) { Text(stringResource(R.string.action_delete), color = TapeOrange) }
            },
            dismissButton = {
                TextButton(onClick = { songToConfirmDelete = null }) { Text(stringResource(R.string.action_cancel), color = TextPrimary) }
            },
        )
    }

    val favoritesState = remember(favorites) {
        FavoritesState(favorites.toSet()) { song -> viewModel.toggleFavorite(song.id) }
    }

    val editSong: (Song) -> Unit = { song -> navController.navigate(Routes.editTags(song.id)) }

    // --- Renaming songs (title tag + file name, with the system write confirmation) ----
    var songToRename by remember { mutableStateOf<Song?>(null) }
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> viewModel.onWritePermissionResult(result.resultCode == Activity.RESULT_OK) }
    LaunchedEffect(Unit) {
        viewModel.writePermissionRequests.collect { sender ->
            writePermissionLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }
    songToRename?.let { song ->
        RenameSongDialog(
            initialName = song.title,
            onConfirm = { name, renameFile ->
                songToRename = null
                viewModel.renameSong(song, name, renameFile)
            },
            onDismiss = { songToRename = null },
        )
    }

    CompositionLocalProvider(
        LocalFavorites provides favoritesState,
        LocalDeleteSong provides deleteSong,
        LocalEditSong provides editSong,
        LocalRenameSong provides { song -> songToRename = song },
    ) {
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
                            onTogglePlay = { KeyClick.play(context); viewModel.togglePlayPause() },
                            onNext = { KeyClick.play(context); viewModel.skipNext() },
                        )
                    }
                    RetroBottomBar(
                        destinations = Routes.bottomDestinations,
                        // Album/playlist pages keep their tab highlighted
                        currentRoute = currentTab,
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
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenSystemSettings = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    )
                                )
                            },
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
                            lyricsQuery = lyricsQuery,
                            onLyricsQueryChange = viewModel::onLyricsQueryChange,
                            lyricsResults = lyricsResults,
                            lyricsIndexed = lyricsIndex.size,
                            totalSongs = songs.size,
                            lyricsDownload = lyricsDownload,
                            onToggleLyricsDownload = viewModel::toggleLyricsDownload,
                            onEnterLyricsMode = viewModel::refreshLyricsIndex,
                            onLyricsResultClick = { song -> viewModel.play(lyricsResults.map { it.song }, song) },
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
                            onPlayAll = viewModel::playAll,
                            onAddAllToQueue = viewModel::addAllToQueue,
                            onSaveAll = saveToPlaylist,
                            onRenamePlaylist = viewModel::renamePlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onEditAlbumCover = { song -> navController.navigate(Routes.editTags(song.id, wholeAlbum = true)) },
                            onPlayCollection = playCollection,
                            onShareMixtape = { collection -> shareMixtape(collection.displayName(context), collection.songs) },
                        )
                    }
                }
                composable(
                    route = Routes.EDIT_TAGS,
                    arguments = listOf(
                        navArgument(Routes.EDIT_SONG_ARG) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument(Routes.EDIT_ALBUM_ARG) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { entry ->
                    val songId = entry.arguments?.getLong(Routes.EDIT_SONG_ARG) ?: -1L
                    val wholeAlbum = entry.arguments?.getBoolean(Routes.EDIT_ALBUM_ARG) ?: false
                    val song = remember(songId) { songs.firstOrNull { it.id == songId } }
                    tabContent {
                        if (song == null) {
                            StatusMessage(stringResource(R.string.collection_not_found))
                        } else {
                            EditTagsScreen(
                                song = song,
                                albumSongs = remember(song) { songs.filter { it.albumId == song.albumId } },
                                applyArtworkToAlbum = wholeAlbum,
                                onBack = { navController.popBackStack() },
                                onSaved = {
                                    viewModel.showMessage(UiMessage.Text(R.string.msg_tags_saved))
                                    viewModel.loadSongs()
                                    navController.popBackStack()
                                },
                                onFailed = { viewModel.showMessage(UiMessage.Text(R.string.msg_tags_failed)) },
                                onCoverDownloadFailed = {
                                    viewModel.showMessage(UiMessage.Text(R.string.msg_cover_failed))
                                },
                            )
                        }
                    }
                }
                composable(Routes.SETTINGS) {
                    val equalizer by EqualizerManager.state.collectAsStateWithLifecycle()
                    tabContent {
                        SettingsScreen(
                            equalizerEnabled = equalizer.enabled,
                            onBack = { navController.popBackStack() },
                            onOpenEqualizer = { navController.navigate(Routes.EQUALIZER) },
                            onOpenChangelog = { navController.navigate(Routes.CHANGELOG) },
                            onReloadLibrary = viewModel::loadSongs,
                            onExportBackup = viewModel::exportBackup,
                            onImportBackup = viewModel::importBackup,
                            onOpenStats = { navController.navigate(Routes.STATS) },
                            lyricsIndexed = lyricsIndex.size,
                            totalSongs = songs.size,
                            lyricsDownload = lyricsDownload,
                            onToggleLyricsDownload = viewModel::toggleLyricsDownload,
                            onEnter = viewModel::refreshLyricsIndex,
                        )
                    }
                }
                composable(Routes.STATS) {
                    val stats by PlayStats.state.collectAsStateWithLifecycle()
                    tabContent {
                        StatsScreen(
                            songs = songs,
                            stats = stats,
                            onBack = { navController.popBackStack() },
                            onSongClick = { list, song -> viewModel.play(list, song) },
                        )
                    }
                }
                composable(Routes.EQUALIZER) {
                    tabContent { EqualizerScreen(onBack = { navController.popBackStack() }) }
                }
                composable(Routes.CHANGELOG) {
                    tabContent { ChangelogScreen(onBack = { navController.popBackStack() }) }
                }
                composable(
                    route = Routes.COLLECTION,
                    arguments = listOf(navArgument(Routes.COLLECTION_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    }),
                ) { entry ->
                    val id = entry.arguments?.getString(Routes.COLLECTION_ARG).orEmpty()
                    val collection = library.find(id)
                    val name = collection?.displayName(context)
                    tabContent {
                        CollectionScreen(
                            collection = collection,
                            playback = playback,
                            onBack = { navController.popBackStack() },
                            onPlayAll = { list -> viewModel.playAll(list, name) },
                            onShufflePlay = { list -> viewModel.shufflePlay(list, name) },
                            onSongClick = { list, song -> viewModel.play(list, song, name) },
                            onPlayNext = viewModel::playNext,
                            onAddToQueue = viewModel::addToQueue,
                            onAddToPlaylist = saveSong,
                            onSaveAll = saveToPlaylist,
                            onRenamePlaylist = viewModel::renamePlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onRemoveFromPlaylist = viewModel::removeFromPlaylist,
                            onReorderPlaylist = viewModel::reorderPlaylist,
                            onShareMixtape = { if (collection != null && name != null) shareMixtape(name, collection.songs) },
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
                        isFavorite = favorites.any { it.toString() == playback.mediaId },
                        onToggleFavorite = {
                            playback.mediaId?.toLongOrNull()?.let(viewModel::toggleFavorite)
                        },
                        onOpenLyrics = { showLyrics = true },
                        sleepTimer = sleepTimer,
                        onOpenSleepTimer = { showSleepTimer = true },
                        onOpenSpeed = { showSpeed = true },
                        tapeName = tapeName,
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
}

private val tabRoutes = Routes.bottomDestinations.map { it.route }.toSet()

/** The bottom-bar tab that owns the current screen (the last tab root in the back stack). */
private fun NavHostController.currentTabRoute(): String? =
    currentBackStack.value.lastOrNull { it.destination.route in tabRoutes }?.destination?.route

private const val PLAYER_ANIM_MS = 350
private const val TAB_FADE_MS = 200
