package com.nitya.musynx.innertube.models.bodies

import com.nitya.musynx.innertube.models.Context
import com.nitya.musynx.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class ContinuationBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val continuation: String,
)
