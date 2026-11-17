package com.nitya.musynx.app.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nitya.musynx.app.appContext
import com.nitya.musynx.app.db
import com.nitya.musynx.app.enums.SongSortBy
import com.nitya.musynx.app.enums.SortOrder
import com.nitya.musynx.app.models.Song
import com.nitya.musynx.app.query
import com.nitya.musynx.app.utils.preferences
import com.nitya.musynx.app.utils.queryMediaStoreSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeSongsViewModel : ViewModel() {
    private var storedSongs: List<Song> by mutableStateOf(emptyList())
    private var availableSongIds: Set<String> by mutableStateOf(emptySet())
    private var hiddenSongIds: Set<String> by mutableStateOf(
        appContext.preferences.getStringSet(HIDDEN_LOCAL_SONG_IDS, emptySet())?.toSet() ?: emptySet()
    )

    val items: List<Song>
        get() = storedSongs.filter { it.id in availableSongIds && it.id !in hiddenSongIds }

    private var scanJob: Job? = null
    private var dbJob: Job? = null

    fun refreshMediaStore() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val result = appContext.queryMediaStoreSongs()
            if (!result.scanCompleted) return@launch

            availableSongIds = result.songs.mapTo(mutableSetOf()) { it.id }
            if (result.songs.isNotEmpty()) {
                query {
                    result.songs.forEach {
                        db.insert(it)
                        db.updateThumbnailUrl(it.id, it.thumbnailUrl)
                    }
                    db.insertAlbums(result.albums)
                    db.insertArtists(result.artists)
                    db.insertSongAlbumMaps(result.songAlbumMaps)
                    db.insertSongArtistMaps(result.songArtistMaps)
                }
            }
        }
    }

    fun hideSong(id: String) {
        val updated = hiddenSongIds + id
        appContext.preferences.edit().putStringSet(HIDDEN_LOCAL_SONG_IDS, updated).apply()
        hiddenSongIds = updated
    }

    fun loadSongs(
        sortBy: SongSortBy,
        sortOrder: SortOrder
    ) {
        dbJob?.cancel()

        dbJob = viewModelScope.launch {
            db.localSongs(sortBy, sortOrder)
                .collect { sortedSongs ->
                    storedSongs = sortedSongs
                }
        }
    }

    private companion object {
        const val HIDDEN_LOCAL_SONG_IDS = "hidden_local_song_ids"
    }
}
