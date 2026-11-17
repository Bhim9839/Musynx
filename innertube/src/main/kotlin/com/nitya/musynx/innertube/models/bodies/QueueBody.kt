package com.nitya.musynx.innertube.models.bodies

import com.nitya.musynx.innertube.models.Context
import com.nitya.musynx.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class QueueBody(
    val context: Context = YouTubeClient.MAC_SAFARI_WEB_REMIX.toContext(),
    val videoIds: List<String>? = null,
    val playlistId: String? = null,
)
