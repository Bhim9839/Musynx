package com.nitya.musynx.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DownloadedSong(
    @PrimaryKey val id: String,
    val downloadedAt: Long = System.currentTimeMillis()
)
