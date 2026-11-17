package com.nitya.musynx.innertube.utils

import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.models.PlaylistPanelVideoRenderer

fun Innertube.SongItem.Companion.from(renderer: PlaylistPanelVideoRenderer): Innertube.SongItem? {
    return Innertube.SongItem(
        info = Innertube.Info(
            name = Innertube.Info.cleanName(renderer
                .title
                ?.text),
            endpoint = renderer
                .navigationEndpoint
                ?.watchEndpoint
        ),
        authors = renderer
            .longBylineText
            ?.splitBySeparator()
            ?.getOrNull(0)
            ?.map(Innertube::Info),
        album = renderer
            .longBylineText
            ?.splitBySeparator()
            ?.getOrNull(1)
            ?.getOrNull(0)
            ?.let(Innertube::Info),
        thumbnail = renderer
            .thumbnail
            ?.thumbnails
            ?.lastOrNull(),
        durationText = renderer
            .lengthText
            ?.text
    ).takeIf { it.info?.endpoint?.videoId != null }
}
