package com.nitya.musynx.innertube.models.bodies

//import com.nitya.musynx.innertube.InnerTubeContext
import com.nitya.musynx.innertube.models.Context
import com.nitya.musynx.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val localized: Boolean = true,
//    val context: InnerTubeContext = InnerTubeContext(),
    val context: Context = YouTubeClient.WEB_REMIX.toContext(localized = localized),
    val browseId: String,
    val params: String? = null
)
