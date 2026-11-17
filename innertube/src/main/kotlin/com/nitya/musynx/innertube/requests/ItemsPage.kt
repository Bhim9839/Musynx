package com.nitya.musynx.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.models.BrowseResponse
import com.nitya.musynx.innertube.models.ContinuationResponse
import com.nitya.musynx.innertube.models.GridRenderer
import com.nitya.musynx.innertube.models.MusicPlaylistShelfRenderer
import com.nitya.musynx.innertube.models.MusicResponsiveListItemRenderer
import com.nitya.musynx.innertube.models.MusicShelfRenderer
import com.nitya.musynx.innertube.models.MusicTwoRowItemRenderer
import com.nitya.musynx.innertube.models.SectionListRenderer
import com.nitya.musynx.innertube.models.bodies.BrowseBody
import com.nitya.musynx.innertube.models.bodies.ContinuationBody
import com.nitya.musynx.innertube.utils.runCatchingNonCancellable

suspend fun <T : Innertube.Item> Innertube.itemsPage(
    browseId: String,
    params: String?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null },
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                params = params
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = (response.contents?.singleColumnBrowseResultsRenderer?.tabs
        ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs)
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer

    itemsPageFromSectionListRenderer(
        sectionListRenderer = sectionListRenderer,
        fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
        fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer
    )
}

suspend fun <T : Innertube.Item> Innertube.itemsPageContinuation(
    continuation: String,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null },
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(ContinuationBody(continuation = continuation))
    }.body<ContinuationResponse>()

    val itemsFromIndividualShelves = itemsPageFromMusicShelRendererOrGridRenderer(
        musicShelfRenderer = response
            .continuationContents
            ?.musicShelfContinuation,
        musicPlaylistShelfRenderer = response
            .continuationContents
            ?.musicPlaylistShelfContinuation,
        gridRenderer = null,
        fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
        fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer,
    )

    if (itemsFromIndividualShelves != null) return@runCatchingNonCancellable itemsFromIndividualShelves

    itemsPageFromSectionListRenderer(
        sectionListRenderer = response.continuationContents?.sectionListContinuation,
        fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
        fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer
    )
}

private fun <T : Innertube.Item> itemsPageFromSectionListRenderer(
    sectionListRenderer: SectionListRenderer?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T?,
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T?,
): Innertube.ItemsPage<T>? {
    if (sectionListRenderer == null) return null

    val allItems = mutableListOf<T>()
    var continuation: String? = sectionListRenderer.continuations?.firstOrNull()?.nextContinuationData?.continuation

    sectionListRenderer.contents?.forEach { content ->
        itemsPageFromMusicShelRendererOrGridRenderer(
            musicShelfRenderer = content.musicShelfRenderer,
            musicPlaylistShelfRenderer = content.musicPlaylistShelfRenderer,
            gridRenderer = content.gridRenderer,
            fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
            fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer
        )?.let { page ->
            page.items?.let { allItems.addAll(it) }
            if (continuation == null) continuation = page.continuation
        }
    }

    return Innertube.ItemsPage(
        items = allItems.takeIf { it.isNotEmpty() },
        continuation = continuation
    )
}

private fun <T : Innertube.Item> itemsPageFromMusicShelRendererOrGridRenderer(
    musicShelfRenderer: MusicShelfRenderer?,
    musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer? = null,
    gridRenderer: GridRenderer?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T?,
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T?,
): Innertube.ItemsPage<T>? {
    return if (musicShelfRenderer != null) {
        Innertube.ItemsPage(
            continuation = musicShelfRenderer
                .continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation,
            items = musicShelfRenderer
                .contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(fromMusicResponsiveListItemRenderer)
        )
    } else if (musicPlaylistShelfRenderer != null) {
        Innertube.ItemsPage(
            continuation = musicPlaylistShelfRenderer
                .continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation,
            items = musicPlaylistShelfRenderer
                .contents
                ?.mapNotNull(MusicPlaylistShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(fromMusicResponsiveListItemRenderer)
        )
    } else if (gridRenderer != null) {
        Innertube.ItemsPage(
            continuation = null,
            items = gridRenderer
                .items
                ?.mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                ?.mapNotNull(fromMusicTwoRowItemRenderer)
        )
    } else null
}