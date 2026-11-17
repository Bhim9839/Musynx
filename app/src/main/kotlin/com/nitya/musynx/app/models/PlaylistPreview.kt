package com.nitya.musynx.app.models

import androidx.compose.runtime.Immutable

@Immutable
data class PlaylistPreview(
    val id: Long,
    val name: String,
    val songCount: Int,
    val thumbnails: List<String?> = emptyList()
) {
    val playlist by lazy {
        Playlist(
            id = id,
            name = name
        )
    }
}
