package com.nitya.musynx.app.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.nitya.musynx.app.LocalPlayerServiceBinder
import com.nitya.musynx.app.db
import com.nitya.musynx.app.enums.SongSortBy
import com.nitya.musynx.app.enums.SortOrder
import com.nitya.musynx.app.models.LocalMenuState
import com.nitya.musynx.app.models.Song
import com.nitya.musynx.app.ui.components.NonQueuedMediaItemMenu
import com.nitya.musynx.app.ui.components.SongListContent
import com.nitya.musynx.app.utils.asMediaItem
import com.nitya.musynx.app.utils.forcePlayAtIndex

@OptIn(ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun LocalArtistSongs(
    browseId: String,
    onGoToAlbum: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var songs: List<Song> by remember { mutableStateOf(emptyList()) }
    var sortBy by remember { mutableStateOf(SongSortBy.Title) }
    var sortOrder by remember { mutableStateOf(SortOrder.Ascending) }

    LaunchedEffect(browseId, sortBy, sortOrder) {
        db.artistSongs(browseId).collect { fetchedSongs ->
            val sortedList = when (sortBy) {
                SongSortBy.Title -> fetchedSongs.sortedBy { it.title.lowercase() }
                else -> fetchedSongs
            }
            songs = if (sortOrder == SortOrder.Descending) sortedList.reversed() else sortedList
        }
    }
    SongListContent(
        songs = songs,
        sortBy = sortBy,
        onSortByChange = { sortBy = it },
        sortByEntries = listOf(SongSortBy.Title),
        onSongClick = { index ->
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(
                songs.map(Song::asMediaItem),
                index
            )
        },
        onSongLongClick = { song ->
            menuState.display {
                NonQueuedMediaItemMenu(
                    onDismiss = menuState::hide,
                    mediaItem = song.asMediaItem,
                    onGoToAlbum = onGoToAlbum
                )
            }
        },
        onPlayAll = {
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(songs.map(Song::asMediaItem), 0)
        },
        onShuffleAll = {
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(songs.shuffled().map(Song::asMediaItem), 0)
        },
        modifier = Modifier.fillMaxSize()
    )
}
