package com.nitya.musynx.app.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.nitya.musynx.core.ui.LocalAppearance
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.requests.playlistPage
import com.nitya.musynx.app.R
import com.nitya.musynx.app.db
import com.nitya.musynx.app.models.Playlist
import com.nitya.musynx.app.models.SongPlaylistMap
import com.nitya.musynx.app.query
import com.nitya.musynx.app.transaction
import com.nitya.musynx.app.ui.components.ParallaxScreenLayout
import com.nitya.musynx.app.ui.components.TextFieldDialog
import com.nitya.musynx.app.utils.ScreenCache
import com.nitya.musynx.app.utils.asMediaItem
import com.nitya.musynx.app.utils.completed
import com.nitya.musynx.app.utils.isScreenCacheEnabledKey
import com.nitya.musynx.app.utils.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun OnlinePlaylistScreen(
    browseId: String,
    onBack: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    var playlistPage: Innertube.PlaylistOrAlbumPage? by remember(browseId) { mutableStateOf(null) }
    var localPlaylist: Playlist? by remember { mutableStateOf(null) }
    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(browseId) {
        db.playlist(browseId).collect { localPlaylist = it }
    }

    LaunchedEffect(browseId) {
        val isScreenCacheEnabled = context.preferences.getBoolean(isScreenCacheEnabledKey, true)
        val cacheKey = "playlist_$browseId"

        if (playlistPage == null && isScreenCacheEnabled) {
            playlistPage = ScreenCache.load(cacheKey)
        }

        withContext(Dispatchers.IO) {
            Innertube.playlistPage(browseId = browseId)
                ?.completed()
                ?.getOrNull()
                ?.let { page ->
                    withContext(Dispatchers.Main) {
                        playlistPage = page
                        if (isScreenCacheEnabled) {
                            ScreenCache.save(cacheKey, page)
                        }
                    }
                }
        }
    }

    BackHandler(onBack = onBack)

    ParallaxScreenLayout(
        title = {
            Text(
                text = playlistPage?.title.orEmpty(),
                color = colorPalette.text,
                style = typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            if (playlistPage != null) {
                val isBookmarked = localPlaylist != null
                IconButton(
                    onClick = {
                        if (isBookmarked) {
                            query { localPlaylist?.let(db::delete) }
                        } else {
                            isImportingPlaylist = true
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isBookmarked) R.drawable.heart else R.drawable.heart_outline
                        ),
                        contentDescription = stringResource(
                            if (isBookmarked) R.string.remove_from_favorites else R.string.add_to_playlist
                        ),
                        tint = if (isBookmarked) colorPalette.accent else colorPalette.text,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        dropDownMenuContent = { dismissMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.share),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    val url = playlistPage?.url
                        ?: "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                    dismissMenu()
                }
            )
        },
        isLoading = playlistPage == null,
        thumbnailUrl = playlistPage?.thumbnail?.url,
        headerTitle = playlistPage?.title.orEmpty(),
        content = {
            OnlinePlaylistSongs(
                playlistPage = playlistPage,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )
        },
        onBackClick = onBack,
    )

    if (isImportingPlaylist) {
        TextFieldDialog(
            title = stringResource(id = R.string.import_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = playlistPage?.title ?: "",
            onDismiss = { isImportingPlaylist = false },
            onDone = { text ->
                scope.launch(Dispatchers.IO) {
                    query {
                        transaction {
                            val playlistId = db.insert(
                                Playlist(
                                    name = text,
                                    browseId = browseId
                                )
                            )

                            playlistPage?.songsPage?.items
                                ?.map(Innertube.SongItem::asMediaItem)
                                ?.onEach(db::insert)
                                ?.mapIndexed { index, mediaItem ->
                                    SongPlaylistMap(
                                        songId = mediaItem.mediaId,
                                        playlistId = playlistId,
                                        position = index
                                    )
                                }?.let(db::insertSongPlaylistMaps)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        isImportingPlaylist = false
                    }
                }
            }
        )
    }
}
