package com.retro.cassetteplayer.ui.screens

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.retro.cassetteplayer.data.ArtworkChange
import com.retro.cassetteplayer.data.CoverResult
import com.retro.cassetteplayer.data.CoverSearch
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongTags
import com.retro.cassetteplayer.data.TagEditor
import com.retro.cassetteplayer.data.WriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TagEditorState(
    val loading: Boolean = true,
    /** The file's tags could not be read (unsupported format); editing still allowed. */
    val readFailed: Boolean = false,
    val tags: SongTags = SongTags(),
    /** Cover currently shown (existing, or the new one picked). */
    val artworkPreview: ByteArray? = null,
    val artworkChange: ArtworkChange = ArtworkChange.Keep,
    val applyArtworkToAlbum: Boolean = false,
    val downloadingArtwork: Boolean = false,
    val saving: Boolean = false,
    // Online cover search
    val searching: Boolean = false,
    val searchResults: List<CoverResult>? = null,
    val searchFailed: Boolean = false,
)

/** Loads a song's tags, keeps the edits and writes them back to the audio file(s). */
class TagEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val editor = TagEditor(application)
    private val _state = MutableStateFlow(TagEditorState())
    val state: StateFlow<TagEditorState> = _state.asStateFlow()

    private var song: Song? = null
    private var albumSongs: List<Song> = emptyList()

    fun start(song: Song, albumSongs: List<Song>, applyArtworkToAlbum: Boolean) {
        if (this.song != null) return
        this.song = song
        this.albumSongs = albumSongs
        _state.update { it.copy(applyArtworkToAlbum = applyArtworkToAlbum && albumSongs.size > 1) }
        viewModelScope.launch {
            val tags = editor.read(song)
            _state.update {
                it.copy(
                    loading = false,
                    readFailed = tags == null,
                    // Fall back to what MediaStore knows when the tags can't be read
                    tags = tags ?: SongTags(title = song.title, artist = song.artist, album = song.album),
                    artworkPreview = tags?.artwork,
                )
            }
        }
    }

    fun updateTags(transform: (SongTags) -> SongTags) = _state.update { it.copy(tags = transform(it.tags)) }

    fun setApplyArtworkToAlbum(value: Boolean) = _state.update { it.copy(applyArtworkToAlbum = value) }

    fun removeArtwork() = _state.update {
        it.copy(artworkPreview = null, artworkChange = ArtworkChange.Remove)
    }

    private fun setArtwork(bytes: ByteArray, mime: String) = _state.update {
        it.copy(artworkPreview = bytes, artworkChange = ArtworkChange.Set(bytes, mime), downloadingArtwork = false)
    }

    fun loadArtworkFrom(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(downloadingArtwork = true) }
            val resolver = getApplication<Application>().contentResolver
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    bytes?.let { it to (resolver.getType(uri) ?: "image/jpeg") }
                }.getOrNull()
            }
            if (result != null) setArtwork(result.first, result.second)
            else _state.update { it.copy(downloadingArtwork = false) }
        }
    }

    fun searchCovers(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(searching = true, searchFailed = false) }
            val results = CoverSearch.search(query)
            _state.update {
                it.copy(searching = false, searchResults = results.orEmpty(), searchFailed = results == null)
            }
        }
    }

    /** Downloads the chosen online cover; returns false through [onDone] on failure. */
    fun pickCover(result: CoverResult, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(downloadingArtwork = true) }
            val image = CoverSearch.download(result.imageUrl) ?: CoverSearch.download(result.thumbnailUrl)
            if (image != null) setArtwork(image.first, image.second)
            else _state.update { it.copy(downloadingArtwork = false) }
            onDone(image != null)
        }
    }

    /** Files that will be modified by saving. */
    private fun targets(): List<Song> {
        val current = song ?: return emptyList()
        val s = _state.value
        val alsoAlbum = s.applyArtworkToAlbum && s.artworkChange !is ArtworkChange.Keep
        return if (alsoAlbum) (listOf(current) + albumSongs).distinctBy { it.id } else listOf(current)
    }

    /** System confirmation needed before writing (Android 11+), or null. */
    fun writeRequest(): IntentSender? = runCatching { editor.writeRequest(targets()) }.getOrNull()

    /**
     * Writes the tags. [onResult] receives the outcome; on [WriteResult.NeedsPermission]
     * the UI launches the confirmation and calls [save] again.
     */
    fun save(onResult: (WriteResult) -> Unit) {
        val current = song ?: return
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val others = if (s.applyArtworkToAlbum) albumSongs else emptyList()
            val result = editor.write(current, s.tags, s.artworkChange, others)
            if (result == WriteResult.Saved) {
                // Old covers may still be cached in memory
                getApplication<Application>().imageLoader.memoryCache?.clear()
            }
            _state.update { it.copy(saving = false) }
            onResult(result)
        }
    }
}
