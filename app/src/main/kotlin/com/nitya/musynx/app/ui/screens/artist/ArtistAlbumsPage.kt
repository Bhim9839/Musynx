package com.nitya.musynx.app.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.requests.itemsPage
import com.nitya.musynx.innertube.requests.itemsPageContinuation
import com.nitya.musynx.innertube.utils.from
import com.nitya.musynx.app.ui.items.AlbumItem
import com.nitya.musynx.app.ui.items.ListItemPlaceholder
import com.nitya.musynx.app.ui.screens.search.ItemsPage
import com.nitya.musynx.app.viewmodels.ItemsPageViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun ArtistAlbumsPage(
    browseId: String,
    params: String? = null,
    onAlbumClick: (String) -> Unit,
    initialItems: List<Innertube.AlbumItem>? = null
) {
    val tag = "artistAlbums/$browseId/${params ?: ""}/grid"
    val viewModel: ItemsPageViewModel<Innertube.AlbumItem> = viewModel()

    LaunchedEffect(tag, initialItems) {
        if (initialItems != null && !viewModel.itemsMap.containsKey(tag)) {
            viewModel.setItems(tag, Innertube.ItemsPage(items = initialItems, continuation = null))
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        ItemsPage(
            tag = tag,
            enableScrollbar = true,
            itemsPageProvider = { continuation ->
                if (continuation == null) {
                    if (initialItems != null) return@ItemsPage null

                    Innertube.itemsPage(
                        browseId = browseId,
                        params = params,
                        fromMusicTwoRowItemRenderer = { Innertube.AlbumItem.from(it) }
                    )
                } else {
                    Innertube.itemsPageContinuation(
                        continuation = continuation,
                        fromMusicTwoRowItemRenderer = { Innertube.AlbumItem.from(it) }
                    )
                }
            },
            itemContent = { album, _, _ ->
                AlbumItem(
                    album = album,
                    onClick = { onAlbumClick(album.key) }
                )
            },
            itemPlaceholderContent = { ListItemPlaceholder() }
        )
    }
}
