package com.nitya.musynx.app.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.requests.itemsPage
import com.nitya.musynx.innertube.requests.itemsPageContinuation
import com.nitya.musynx.innertube.utils.from
import com.nitya.musynx.app.LocalPlayerServiceBinder
import com.nitya.musynx.app.R
import com.nitya.musynx.app.enums.SongSortBy
import com.nitya.musynx.app.enums.SortOrder
import com.nitya.musynx.app.models.LocalMenuState
import com.nitya.musynx.app.ui.components.NonQueuedMediaItemMenu
import com.nitya.musynx.app.ui.components.SortingHeader
import com.nitya.musynx.app.ui.items.ListItemPlaceholder
import com.nitya.musynx.app.ui.items.SongItem
import com.nitya.musynx.app.ui.screens.search.ItemsPage
import com.nitya.musynx.app.utils.asMediaItem
import com.nitya.musynx.app.utils.forcePlayAtIndex
import com.nitya.musynx.app.viewmodels.ItemsPageViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun ArtistTracksPage(
    browseId: String,
    params: String? = null,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    initialItems: List<Innertube.SongItem>? = null
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val emptyItemsText = stringResource(id = R.string.no_results_found)

    var sortBy by rememberSaveable { mutableStateOf(SongSortBy.Artist) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.Ascending) }

    val tag = "artistSongs/$browseId/${params ?: ""}/list"
    val viewModel: ItemsPageViewModel<Innertube.SongItem> = viewModel()

    LaunchedEffect(tag, initialItems) {
        if (initialItems != null && !viewModel.itemsMap.containsKey(tag)) {
            viewModel.setItems(tag, Innertube.ItemsPage(items = initialItems, continuation = null))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key(sortBy, sortOrder) {
            ItemsPage(
                tag = tag,
                enableScrollbar = true,
                sortKeys = listOf(sortBy, sortOrder),
                sortTransform = { items ->
                    val comparator = when (sortBy) {
                        SongSortBy.Title -> compareBy { it.info?.name?.lowercase() ?: "" }
                        SongSortBy.Artist -> compareBy<Innertube.SongItem> { it -> it.authors?.mapNotNull { it.name }?.joinToString(" • ")?.lowercase() ?: "" }
                        else -> null
                    }
                    val sorted = if (comparator != null) items.sortedWith(comparator) else items
                    if (sortOrder == SortOrder.Descending) sorted.reversed() else sorted
                },
                header = { sortedItems ->
                    SortingHeader(
                        sortBy = sortBy,
                        changeSortBy = { sortBy = it },
                        sortByEntries = SongSortBy.entries.toList(),
                        onPlayClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(sortedItems.map { it.asMediaItem }, 0)
                        },
                        onShuffleClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(sortedItems.shuffled().map { it.asMediaItem }, 0)
                        }
                    )
                },
                itemsPageProvider = { continuation ->
                    if (continuation == null) {
                        if (initialItems != null) return@ItemsPage null

                        Innertube.itemsPage(
                            browseId = browseId,
                            params = params,
                            fromMusicResponsiveListItemRenderer = { Innertube.SongItem.from(it) }
                        )
                    } else {
                        Innertube.itemsPageContinuation(
                            continuation = continuation,
                            fromMusicResponsiveListItemRenderer = { Innertube.SongItem.from(it) }
                        )
                    }
                },
                emptyItemsText = emptyItemsText,
                enablePreCache = true,
                itemContent = { song, index, sortedItems ->
                    SongItem(
                        song = song,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(sortedItems.map { it.asMediaItem }, index)
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
                },
                itemPlaceholderContent = { ListItemPlaceholder() }
            )
        }
    }
}
