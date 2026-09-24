package com.retro.cassetteplayer

import android.app.Application
import kotlinx.coroutines.Job
import com.retro.cassetteplayer.data.LyricsResult
import com.retro.cassetteplayer.data.LyricsRepository
import com.retro.cassetteplayer.data.Lyrics
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retro.cassetteplayer.data.DeleteOutcome
import com.retro.cassetteplayer.data.FavoritesRepository
import com.retro.cassetteplayer.data.LibraryCollections
import com.retro.cassetteplayer.data.MusicRepository
import com.retro.cassetteplayer.data.PlaylistRepository
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.buildLibrary
import com.retro.cassetteplayer.playback.PlaybackConnection
import com.retro.cassetteplayer.playback.PlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import com.retro.cassetteplayer.data.Backup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LyricsState {
    val songId: Long?

    data object Idle : LyricsState {
        override val songId: Long? = null
    }
    data class Loading(override val songId: Long) : LyricsState
    data class Found(override val songId: Long, val lyrics: Lyrics) : LyricsState
    data class NotFound(override val songId: Long) : LyricsState
    data class Offline(override val songId: Long) : LyricsState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val connection = PlaybackConnection(application)
    private val playlistRepository = PlaylistRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val lyricsRepository = LyricsRepository(application)

    /** Lyrics of the song they were requested for; Loading while looking them up. */
    private val _lyrics = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyrics: StateFlow<LyricsState> = _lyrics.asStateFlow()
    private var lyricsJob: Job? = null

    /** Favourite song ids, most recent first. */
    val favorites: StateFlow<List<Long>> = favoritesRepository.favorites

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    /** One-off feedback shown as a snackbar. */
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    val playback: StateFlow<PlaybackState> = connection.state

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasPermission = MutableStateFlow<Boolean?>(null)
    /** null = not asked yet, true = granted, false = denied. */
    val hasPermission: StateFlow<Boolean?> = _hasPermission.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val searchResults: StateFlow<List<Song>> = combine(_songs, _query) { songs, query ->
        val q = query.trim()
        if (q.isEmpty()) songs
        else songs.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val library: StateFlow<LibraryCollections> = combine(
        _songs,
        playlistRepository.playlists,
        favoritesRepository.favorites,
    ) { songs, playlists, favoriteIds ->
        buildLibrary(songs, playlists, favoriteIds)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryCollections())

    init {
        connection.connect()
        viewModelScope.launch {
            connection.errors.collect { title ->
                _messages.tryEmit(UiMessage.Text(R.string.msg_cannot_play, title))
            }
        }
        // Tick the playback position while audio is playing (drives seekbar + reels).
        viewModelScope.launch {
            playback.map { it.isPlaying }.distinctUntilChanged().collectLatest { playing ->
                while (playing) {
                    connection.refreshPosition()
                    delay(250)
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _hasPermission.value = granted
        if (granted && _songs.value.isEmpty()) loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _songs.value = runCatching { repository.loadSongs() }.getOrDefault(emptyList())
            _isLoading.value = false
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun play(songs: List<Song>, song: Song) =
        connection.playSongs(songs, songs.indexOf(song).coerceAtLeast(0))

    fun playAll(songs: List<Song>) = connection.playSongs(songs, 0)

    fun shufflePlay(songs: List<Song> = _songs.value) {
        if (songs.isEmpty()) return
        connection.playSongs(songs, songs.indices.random(), shuffle = true)
    }

    fun playNext(song: Song) = connection.playNext(song)
    fun addToQueue(song: Song) = connection.addToQueue(song)

    fun addAllToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        connection.addAllToQueue(songs)
        _messages.tryEmit(UiMessage.Plural(R.plurals.msg_added_to_queue, songs.size))
    }
    fun togglePlayPause() = connection.togglePlayPause()
    fun skipNext() = connection.skipNext()
    fun skipPrevious() = connection.skipPrevious()
    fun rewind() = connection.rewind()
    fun fastForward() = connection.fastForward()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)
    fun toggleShuffle() = connection.toggleShuffle()
    fun cycleRepeat() = connection.cycleRepeat()
    fun playQueueItem(index: Int) = connection.playQueueItem(index)
    fun removeQueueItem(index: Int) = connection.removeQueueItem(index)

    // --- Deleting songs --------------------------------------------------------------

    private var pendingDelete: Song? = null

    /**
     * Starts deleting [song]. Returns an IntentSender when the system has to confirm;
     * the UI launches it and reports back through [onDeleteConfirmation].
     */
    fun requestDelete(song: Song): IntentSender? =
        when (val outcome = repository.requestDelete(song)) {
            DeleteOutcome.Deleted -> {
                onSongDeleted(song)
                null
            }
            is DeleteOutcome.NeedsConfirmation -> {
                pendingDelete = song
                outcome.intentSender
            }
            DeleteOutcome.Failed -> {
                _messages.tryEmit(UiMessage.Text(R.string.msg_delete_failed))
                null
            }
        }

    fun onDeleteConfirmation(confirmed: Boolean) {
        val song = pendingDelete ?: return
        pendingDelete = null
        if (!confirmed) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // createDeleteRequest already removed the file once the user confirmed
            onSongDeleted(song)
        } else if (repository.requestDelete(song) == DeleteOutcome.Deleted) {
            onSongDeleted(song)
        } else {
            _messages.tryEmit(UiMessage.Text(R.string.msg_delete_failed))
        }
    }

    private fun onSongDeleted(song: Song) {
        _songs.value = _songs.value.filterNot { it.id == song.id }
        connection.removeFromQueue(song.id.toString())
        favoritesRepository.remove(song.id)
        playlistRepository.removeSongEverywhere(song.id)
        _messages.tryEmit(UiMessage.Text(R.string.msg_song_deleted, song.title))
    }

    /** Shows feedback coming from other screens (e.g. the tag editor). */
    fun showMessage(message: UiMessage) {
        _messages.tryEmit(message)
    }

    // --- Lyrics -----------------------------------------------------------------

    fun requestLyrics(song: Song, forceRefresh: Boolean = false) {
        val current = _lyrics.value
        if (!forceRefresh && current.songId == song.id && current !is LyricsState.Idle) return
        lyricsJob?.cancel()
        _lyrics.value = LyricsState.Loading(song.id)
        lyricsJob = viewModelScope.launch {
            _lyrics.value = when (val result = lyricsRepository.lyricsFor(song, forceRefresh)) {
                is LyricsResult.Found -> LyricsState.Found(song.id, result.lyrics)
                LyricsResult.NotFound -> LyricsState.NotFound(song.id)
                LyricsResult.Offline -> LyricsState.Offline(song.id)
            }
        }
    }

    // --- Backup -----------------------------------------------------------------

    /** Writes playlists and favourites as JSON to a file picked by the user. */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val json = Backup.export(_songs.value, playlistRepository.playlists.value, favoritesRepository.favorites.value)
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(json.toByteArray())
                    } != null
                }.getOrDefault(false)
            }
            _messages.tryEmit(UiMessage.Text(if (ok) R.string.msg_backup_exported else R.string.msg_backup_failed))
        }
    }

    /** Restores a backup, merging with what's already there. */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes().decodeToString()
                    }
                }.getOrNull()
            }
            val contents = text?.let(Backup::parse)
            if (contents == null) {
                _messages.tryEmit(UiMessage.Text(R.string.msg_backup_invalid))
                return@launch
            }
            val matcher = Backup.Matcher(_songs.value)
            var missing = 0
            fun resolve(refs: List<Backup.SongRef>): List<Long> = refs.mapNotNull { ref ->
                matcher.find(ref)?.id.also { if (it == null) missing++ }
            }

            favoritesRepository.addAll(resolve(contents.favorites))
            contents.playlists.forEach { (name, refs) ->
                val ids = resolve(refs)
                val existing = playlistRepository.playlists.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (existing != null) playlistRepository.addSongs(existing.id, ids)
                else playlistRepository.create(name, ids)
            }
            _messages.tryEmit(
                if (missing == 0) {
                    UiMessage.Text(R.string.msg_backup_imported, contents.playlists.size, contents.favorites.size)
                } else {
                    UiMessage.Text(
                        R.string.msg_backup_imported_missing,
                        contents.playlists.size,
                        contents.favorites.size,
                        missing,
                    )
                }
            )
        }
    }

    // --- Favourites -----------------------------------------------------------------

    fun toggleFavorite(songId: Long) {
        val nowFavorite = favoritesRepository.toggle(songId)
        _messages.tryEmit(
            UiMessage.Text(if (nowFavorite) R.string.msg_added_favorite else R.string.msg_removed_favorite)
        )
    }

    // --- User playlists -------------------------------------------------------------

    fun createPlaylist(name: String, songs: List<Song> = emptyList()) {
        if (name.isBlank()) return
        val playlist = playlistRepository.create(name, songs.map { it.id })
        _messages.tryEmit(
            UiMessage.Text(if (songs.isEmpty()) R.string.msg_playlist_created else R.string.msg_saved_to, playlist.name)
        )
    }

    fun renamePlaylist(id: String, name: String) {
        if (name.isNotBlank()) playlistRepository.rename(id, name)
    }

    fun deletePlaylist(id: String) {
        playlistRepository.delete(id)
        _messages.tryEmit(UiMessage.Text(R.string.msg_playlist_deleted))
    }

    fun addToPlaylist(playlistId: String, songs: List<Song>) {
        val name = playlistRepository.playlists.value.firstOrNull { it.id == playlistId }?.name ?: return
        val added = playlistRepository.addSongs(playlistId, songs.map { it.id })
        _messages.tryEmit(UiMessage.Text(if (added == 0) R.string.msg_already_in else R.string.msg_saved_to, name))
    }

    fun reorderPlaylist(playlistId: String, songs: List<Song>) =
        playlistRepository.reorder(playlistId, songs.map { it.id })

    fun removeFromPlaylist(playlistId: String, song: Song) = playlistRepository.removeSong(playlistId, song.id)

    override fun onCleared() {
        connection.release()
        super.onCleared()
    }
}
