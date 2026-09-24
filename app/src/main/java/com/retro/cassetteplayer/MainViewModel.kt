package com.retro.cassetteplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val connection = PlaybackConnection(application)
    private val playlistRepository = PlaylistRepository(application)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** One-off feedback shown as a snackbar ("Adicionada a …"). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

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

    val library: StateFlow<LibraryCollections> = combine(_songs, playlistRepository.playlists) { songs, playlists ->
        buildLibrary(songs, playlists)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryCollections())

    init {
        connection.connect()
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

    // --- User playlists -------------------------------------------------------------

    fun createPlaylist(name: String, songs: List<Song> = emptyList()) {
        if (name.isBlank()) return
        val playlist = playlistRepository.create(name, songs.map { it.id })
        _messages.tryEmit(
            if (songs.isEmpty()) "Playlist \"${playlist.name}\" criada"
            else "Salvo em \"${playlist.name}\""
        )
    }

    fun renamePlaylist(id: String, name: String) {
        if (name.isNotBlank()) playlistRepository.rename(id, name)
    }

    fun deletePlaylist(id: String) {
        playlistRepository.delete(id)
        _messages.tryEmit("Playlist excluída")
    }

    fun addToPlaylist(playlistId: String, songs: List<Song>) {
        val name = playlistRepository.playlists.value.firstOrNull { it.id == playlistId }?.name ?: return
        val added = playlistRepository.addSongs(playlistId, songs.map { it.id })
        _messages.tryEmit(if (added == 0) "Já está em \"$name\"" else "Salvo em \"$name\"")
    }

    fun removeFromPlaylist(playlistId: String, song: Song) = playlistRepository.removeSong(playlistId, song.id)

    override fun onCleared() {
        connection.release()
        super.onCleared()
    }
}
