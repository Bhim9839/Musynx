package com.nitya.musynx.app.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nitya.musynx.app.LocalPlayerPadding
import com.nitya.musynx.app.enums.ArtistSortBy
import com.nitya.musynx.app.enums.SortOrder
import com.nitya.musynx.app.models.Artist
import com.nitya.musynx.app.ui.components.SortingHeader
import com.nitya.musynx.app.ui.items.LocalArtistItem
import com.nitya.musynx.app.utils.artistSortByKey
import com.nitya.musynx.app.utils.artistSortOrderKey
import com.nitya.musynx.app.utils.rememberPreference
import com.nitya.musynx.app.viewmodels.home.HomeArtistsViewModel

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeArtistList(
    onArtistClick: (Artist) -> Unit
) {
    val playerPadding = LocalPlayerPadding.current

    var sortBy by rememberPreference(artistSortByKey, ArtistSortBy.Name)
    var sortOrder by rememberPreference(artistSortOrderKey, SortOrder.Ascending)

    val viewModel: HomeArtistsViewModel = viewModel()

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadArtists(
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(
            top = 12.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = 16.dp + playerPadding
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(
            key = "header",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            SortingHeader(
                sortBy = sortBy,
                changeSortBy = { sortBy = it },
                sortByEntries = ArtistSortBy.entries.toList()
            )
        }

        items(items = viewModel.items, key = Artist::id) { artist ->
            LocalArtistItem(
                modifier = Modifier.animateItem(),
                artist = artist,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}