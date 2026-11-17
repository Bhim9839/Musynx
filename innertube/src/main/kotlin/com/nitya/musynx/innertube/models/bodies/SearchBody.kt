package com.nitya.musynx.innertube.models.bodies

import com.nitya.musynx.innertube.models.Context
import com.nitya.musynx.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context = YouTubeClient.MAC_SAFARI_WEB_REMIX.toContext(),
    val query: String,
    val params: String
)
