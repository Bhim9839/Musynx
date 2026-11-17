package com.nitya.musynx.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.models.BrowseResponse
import com.nitya.musynx.innertube.models.MusicCarouselShelfRenderer
import com.nitya.musynx.innertube.models.NextResponse
import com.nitya.musynx.innertube.models.YouTubeClient
import com.nitya.musynx.innertube.models.bodies.BrowseBody
import com.nitya.musynx.innertube.models.bodies.NextBody
import com.nitya.musynx.innertube.utils.findSectionByStrapline
import com.nitya.musynx.innertube.utils.findSectionByTitle
import com.nitya.musynx.innertube.utils.from
import com.nitya.musynx.innertube.utils.runCatchingNonCancellable
import java.util.Locale

suspend fun Innertube.relatedPage(videoId: String) = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    val nextResponse = client.post(NEXT) {
        setBody(NextBody(videoId = videoId))
    }.body<NextResponse>()

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.let { tabs ->
            tabs.find { it.tabRenderer?.endpoint?.browseEndpoint?.browseId?.startsWith("FEmusic_related") == true }
                ?: tabs.find { it.tabRenderer?.title == "Related" }
                ?: tabs.getOrNull(2)
        }
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                context = YouTubeClient.WEB_REMIX.toContext(
                    hl = "en",
                    gl = Locale.getDefault().country.ifBlank { "US" },
                )
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    Innertube.RelatedPage(
        songs = sectionListRenderer
            ?.findSectionByTitle("You might also like")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        playlists = sectionListRenderer
            ?.findSectionByTitle("Recommended playlists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.PlaylistItem::from)
            ?.sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = sectionListRenderer
            ?.findSectionByStrapline("MORE FROM")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        artists = sectionListRenderer
            ?.findSectionByTitle("Similar artists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.ArtistItem::from),
    )
}
