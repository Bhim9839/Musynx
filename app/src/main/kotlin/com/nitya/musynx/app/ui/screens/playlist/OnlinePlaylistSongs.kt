package com.nitya.musynx.app.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.app.LocalPlayerPadding
import com.nitya.musynx.app.LocalPlayerServiceBinder
import com.nitya.musynx.app.R
import com.nitya.musynx.app.models.ActionInfo
import com.nitya.musynx.app.models.LocalMenuState
import com.nitya.musynx.app.ui.components.NonQueuedMediaItemMenu
import com.nitya.musynx.app.ui.components.ShimmerHost
import com.nitya.musynx.app.ui.components.SwipeToActionBox
import com.nitya.musynx.app.ui.items.ListItemPlaceholder
import com.nitya.musynx.app.ui.items.SongItem
import com.nitya.musynx.app.utils.asMediaItem
import com.nitya.musynx.app.utils.enqueue
import com.nitya.musynx.app.utils.forcePlayAtIndex


@ExperimentalAnimationApi
@UnstableApi
@Composable
fun OnlinePlaylistSongs(
    playlistPage: Innertube.PlaylistOrAlbumPage?,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    LaunchedEffect(playlistPage) {
        playlistPage?.songsPage?.items?.take(5)?.map(Innertube.SongItem::asMediaItem)?.let { mediaItems ->
            binder?.preCacheManager?.preCache(mediaItems)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(
            items = playlistPage?.songsPage?.items ?: emptyList(),
            key = { _, song -> song.key }
        ) { index, song ->
            SwipeToActionBox(
                primaryAction = ActionInfo(
                    onClick = { binder?.player?.enqueue(song.asMediaItem) },
                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    description = R.string.enqueue
                )
            ) {
                SongItem(
                    song = song,
                    onClick = {
                        playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)
                            ?.let { mediaItems ->
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(mediaItems, index)
                            }
                    },
                    onLongClick = {
                        menuState.display {
                            NonQueuedMediaItemMenu(
                                onDismiss = menuState::hide,
                                mediaItem = song.asMediaItem,
                                onGoToAlbum = onGoToAlbum,
                                onGoToArtist = onGoToArtist
                            )
                        }
                    }
                )
            }
        }

        if (playlistPage == null) {
            item(key = "loading") {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceholder()
                    }
                }
            }
        }
    }
}
