package com.nitya.musynx.app.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.nitya.musynx.app.db
import com.nitya.musynx.app.enums.AlbumSortBy
import com.nitya.musynx.app.enums.SortOrder
import com.nitya.musynx.app.models.Album

class HomeAlbumsViewModel : ViewModel() {
    var items: List<Album> by mutableStateOf(emptyList())

    suspend fun loadAlbums(
        sortBy: AlbumSortBy,
        sortOrder: SortOrder
    ) {
        db
            .localAlbums(sortBy, sortOrder)
            .collect { items = it }
    }
}