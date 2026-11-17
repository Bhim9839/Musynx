package com.nitya.musynx.app.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.nitya.musynx.app.db
import com.nitya.musynx.app.models.QueuedMediaItem
import com.nitya.musynx.app.query
import com.nitya.musynx.app.utils.asMediaItem
import com.nitya.musynx.app.utils.mediaItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class QueuePersistenceManager(
    private val player: Player,
    private val coroutineScope: CoroutineScope,
    private val onQueueRestored: () -> Unit
) {

    fun saveQueue(isEnabled: Boolean) {
        if (!isEnabled) return

        val mediaItems = player.currentTimeline.mediaItems
        val mediaItemIndex = player.currentMediaItemIndex
        val mediaItemPosition = player.currentPosition

        val queuedMediaItems = mediaItems.mapIndexed { index, mediaItem ->
            QueuedMediaItem(
                mediaItem = mediaItem,
                position = if (index == mediaItemIndex) mediaItemPosition else null
            )
        }

        query {
            db.clearQueue()
            db.insert(queuedMediaItems)
        }
    }

    fun clearQueue() {
        query {
            db.clearQueue()
        }
    }

    @OptIn(UnstableApi::class)
    fun restoreQueue(isEnabled: Boolean) {
        if (!isEnabled) return

        query {
            val queuedSong = db.queue()
            db.clearQueue()

            if (queuedSong.isEmpty()) {
                val latestSong = runBlocking {
                    db.history(limit = 1, minPlayTimeMs = 0).firstOrNull()?.firstOrNull()
                        ?: db.favorites().firstOrNull()?.firstOrNull()
                }
                if (latestSong != null) {
                    val mediaItem = latestSong.asMediaItem.buildUpon()
                        .setUri(latestSong.id)
                        .setCustomCacheKey(latestSong.id)
                        .build().apply {
                            mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                        }
                    coroutineScope.launch(Dispatchers.Main) {
                        player.setMediaItems(listOf(mediaItem), 0, C.TIME_UNSET)
                        player.prepare()
                        onQueueRestored()
                    }
                }
                return@query
            }

            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)

            coroutineScope.launch(Dispatchers.Main) {
                player.setMediaItems(
                    queuedSong.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(mediaItem.mediaItem.mediaId)
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    index,
                    queuedSong[index].position ?: C.TIME_UNSET
                )
                player.prepare()
                onQueueRestored()
            }
        }
    }
}