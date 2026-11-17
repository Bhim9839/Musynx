package com.nitya.musynx.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.models.BrowseResponse
import com.nitya.musynx.innertube.models.MusicCarouselShelfRenderer
import com.nitya.musynx.innertube.models.YouTubeClient
import com.nitya.musynx.innertube.models.bodies.BrowseBody
import com.nitya.musynx.innertube.utils.findSectionByTitle
import com.nitya.musynx.innertube.utils.from
import com.nitya.musynx.innertube.utils.runCatchingNonCancellable
import java.util.Locale

suspend fun Innertube.recommendations(): Result<List<Innertube.SongItem>?>? = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = "FEmusic_home",
                context = YouTubeClient.WEB_REMIX.toContext(
                    hl = Locale.getDefault().language.ifBlank { "en" },
                    gl = Locale.getDefault().country.ifBlank { "US" },
                    visitorData = visitorData
                )
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = response.contents?.sectionListRenderer
        ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs
            ?.firstNotNullOfOrNull { it.tabRenderer?.content?.sectionListRenderer }
        ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            ?.firstNotNullOfOrNull { it.tabRenderer?.content?.sectionListRenderer }

    val preferredSections = listOfNotNull(
        sectionListRenderer?.findSectionByTitle("Quick picks"),
        sectionListRenderer?.findSectionByTitle("Recommended"),
        sectionListRenderer?.findSectionByTitle("Listen again")
    )

    (preferredSections + sectionListRenderer?.contents.orEmpty()).firstNotNullOfOrNull { section ->
        section.musicCarouselShelfRenderer?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from)
            ?.takeIf { it.isNotEmpty() }
    }
}
