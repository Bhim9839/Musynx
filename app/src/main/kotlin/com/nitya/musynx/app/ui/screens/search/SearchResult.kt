package com.nitya.musynx.app.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.requests.searchPage
import com.nitya.musynx.innertube.utils.from
import com.nitya.musynx.app.LocalPlayerServiceBinder
import com.nitya.musynx.app.R
import com.nitya.musynx.app.models.ActionInfo
import com.nitya.musynx.app.models.LocalMenuState
import com.nitya.musynx.app.ui.components.NonQueuedMediaItemMenu
import com.nitya.musynx.app.ui.components.SettingsCard
import com.nitya.musynx.app.ui.components.StaticScreenLayout
import com.nitya.musynx.app.ui.components.SwipeToActionBox
import com.nitya.musynx.app.ui.items.AlbumItem
import com.nitya.musynx.app.ui.navigation.Routes
import com.nitya.musynx.app.ui.items.ArtistItem
import com.nitya.musynx.app.ui.items.ItemPlaceholder
import com.nitya.musynx.app.ui.items.ListItemPlaceholder
import com.nitya.musynx.app.ui.items.PlaylistItem
import com.nitya.musynx.app.ui.items.SongItem
import com.nitya.musynx.app.ui.items.VideoItem
import com.nitya.musynx.app.utils.asMediaItem
import com.nitya.musynx.app.utils.enqueue
import com.nitya.musynx.app.utils.forcePlay

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun NewSearchResult(
    navController: NavController,
    query: String,
    resultType: String?
) {
    BackHandler { navController.popBackStack() }

    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val emptyItemsText = stringResource(id = R.string.no_results_found)

    val onAlbumClick: (String) -> Unit = { albumId -> navController.navigate(Routes.Album(id = albumId)) }
    val onArtistClick: (String) -> Unit = { artistId -> navController.navigate(Routes.Artist(id = artistId)) }
    val onPlaylistClick: (String) -> Unit = { playlistId -> navController.navigate(Routes.Playlist(id = playlistId)) }

    StaticScreenLayout(
        title = resultType ?: stringResource(R.string.results),
        onBackClick = { navController.popBackStack() },
        scrollable = false,
        horizontalPadding = 0.dp
    ) {
        SettingsCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
        ){
            when (resultType) {

                stringResource(R.string.songs) -> {
                    ItemsPage(
                        tag = "searchResults/$query/songs",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.Song.value,
                                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        enablePreCache = true,
                        itemContent = { song, _, _ ->
                            SwipeToActionBox(
                                modifier = Modifier.animateItem(),
                                primaryAction = ActionInfo(
                                    onClick = { binder?.player?.enqueue(song.asMediaItem) },
                                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                    description = R.string.enqueue
                                )
                            ) {
                                SongItem(
                                    song = song,
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(song.asMediaItem)
                                        binder?.setupRadio(song.info?.endpoint)
                                    },
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem,
                                                onGoToAlbum = onAlbumClick,
                                                onGoToArtist = onArtistClick
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        itemPlaceholderContent = { ListItemPlaceholder() }
                    )
                }

                stringResource(R.string.videos) -> {
                    ItemsPage(
                        tag = "searchResults/$query/videos",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.Video.value,
                                    fromMusicShelfRendererContent = Innertube.VideoItem::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.VideoItem::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { video, _, _ ->
                            SwipeToActionBox(
                                modifier = Modifier.animateItem(),
                                primaryAction = ActionInfo(
                                    onClick = { binder?.player?.enqueue(video.asMediaItem) },
                                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                    description = R.string.enqueue
                                )
                            ) {
                                VideoItem(
                                    video = video,
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(video.asMediaItem)
                                        binder?.setupRadio(video.info?.endpoint)
                                    },
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = video.asMediaItem,
                                                onGoToAlbum = onAlbumClick,
                                                onGoToArtist = onArtistClick
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        itemPlaceholderContent = { ListItemPlaceholder() }
                    )
                }

                stringResource(R.string.albums) -> {
                    ItemsPage(
                        tag = "searchResults/$query/albums",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.Album.value,
                                    fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { album, _, _ ->
                            AlbumItem(
                                album = album,
                                onClick = { onAlbumClick(album.key) }
                            )
                        },
                        itemPlaceholderContent = { ItemPlaceholder() }
                    )
                }

                stringResource(R.string.artists) -> {
                    ItemsPage(
                        tag = "searchResults/$query/artists",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.Artist.value,
                                    fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { artist, _, _ ->
                            ArtistItem(
                                artist = artist,
                                onClick = { onArtistClick(artist.key) }
                            )
                        },
                        itemPlaceholderContent = { ItemPlaceholder(shape = CircleShape) }
                    )
                }

                stringResource(R.string.playlists) -> {
                    ItemsPage(
                        tag = "searchResults/$query/playlists",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.CommunityPlaylist.value,
                                    fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { playlist, _, _ ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.key) }
                            )
                        },
                        itemPlaceholderContent = { ItemPlaceholder() }
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.unknown_category, resultType ?: "?"),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
