package com.nitya.musynx.app.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.nitya.musynx.app.db
import com.nitya.musynx.app.enums.ArtistSortBy
import com.nitya.musynx.app.enums.SortOrder
import com.nitya.musynx.app.models.Artist

class HomeArtistsViewModel : ViewModel() {
    var items: List<Artist> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: ArtistSortBy,
        sortOrder: SortOrder
    ) {
        db
            .localArtists(sortBy, sortOrder)
            .collect { items = it }
    }
}