package com.nitya.musynx.innertube.models.bodies

import com.nitya.musynx.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String? = null,
    val serviceIntegrityDimensions: ServiceIntegrityDimensions? = null
)