package com.nitya.musynx.app.service

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.requests.player
import com.nitya.musynx.app.utils.PlaybackSource
import com.nitya.musynx.app.utils.getEnum
import com.nitya.musynx.app.utils.pauseSongCacheKey
import com.nitya.musynx.app.utils.playbackSourceKey
import com.nitya.musynx.app.utils.preferences
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.ServiceList
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@UnstableApi
class PlayerMediaSourceProvider(
    private val context: Context,
    private val cacheManager: PlayerCacheManager
) {
    private val urlCache = ConcurrentHashMap<String, Pair<Uri, Long>>()
    private val resolutionLocks = ConcurrentHashMap<String, ReentrantLock>()
    private val sourceTracker = ConcurrentHashMap<String, Int>()

    fun getPlaybackSource(videoId: String): Int? = sourceTracker[videoId]

    fun injectUrl(videoId: String, uri: Uri) {
        urlCache[videoId] = Pair(uri, System.currentTimeMillis())
    }

    fun evictUrl(videoId: String) {
        urlCache.remove(videoId)
    }

    companion object {
        const val CACHE_EXPIRATION_MS = 4 * 3600000L
        const val DEFAULT_USER_AGENT = "com.google.android.apps.youtube.vr.oculus/1.74.31 (Linux; U; Android 14; eureka-user Build/SQ3A.220605.009.A1) gzip"
    }

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
            .setLoadErrorHandlingPolicy(YouTube403ErrorPolicy(urlCache))
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val playbackSource = context.preferences.getEnum(playbackSourceKey, PlaybackSource.NewPipe)
        
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(if (playbackSource == PlaybackSource.NewPipe) "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" else DEFAULT_USER_AGENT)

        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val resolvingUpstreamFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val videoId = dataSpec.key ?: throw IOException("A key must be set")
            
            // If it's already a full playback URL, we need to ensure headers are attached
            if (dataSpec.uri.toString().contains("videoplayback")) {
                val headers = buildMap {
                    put("Referer", "https://www.youtube.com/")
                    put("Origin", "https://www.youtube.com")
                    Innertube.visitorData?.let { put("X-Goog-Visitor-Id", it) }
                    Innertube.poToken?.let { put("X-YouTube-Po-Token", it) }
                    Innertube.cookies?.let { 
                        put("Cookie", it)
                        Innertube.generateAuthHeader(it)?.let { auth ->
                            put("Authorization", auth)
                        }
                    }
                }
                return@Factory dataSpec.buildUpon()
                    .setHttpRequestHeaders(headers)
                    .build()
            }

            Log.d("Musynx-DataSource", "Resolving URI for key: $videoId")
            if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
                dataSpec
            } else {
                val uri = resolveUrl(videoId)
                dataSpec.withUri(uri)
            }
        }

        return DataSource.Factory {
            val pauseSongCache = context.preferences.getBoolean(pauseSongCacheKey, false)

            val cacheDataSource = CacheDataSource.Factory()
                .setCache(cacheManager.cache)
                .setUpstreamDataSourceFactory(resolvingUpstreamFactory)
                .apply {
                    if (pauseSongCache) {
                        setCacheWriteDataSinkFactory(null)
                    } else {
                        setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cacheManager.cache))
                    }
                }
                .createDataSource()

            cacheDataSource
        }
    }

    fun resolveUrl(videoId: String): Uri {
        if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
            return videoId.toUri()
        }

        // Check if fully cached first to avoid network calls in offline mode
        val metadata = cacheManager.cache.getContentMetadata(videoId)
        val length = ContentMetadata.getContentLength(metadata)
        if (length > 0 && cacheManager.cache.isCached(videoId, 0, length)) {
            Log.d("Musynx-DataSource", "Cache HIT for $videoId ($length bytes), bypassing resolution")
            sourceTracker[videoId] = com.nitya.musynx.app.R.string.cache
            return urlCache[videoId]?.first ?: "https://www.youtube.com/watch?v=$videoId".toUri()
        }

        urlCache[videoId]?.let { (uri, timestamp) ->
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                Log.d("Musynx-DataSource", "URL cache hit for $videoId")
                return uri
            }
        }

        val lock = resolutionLocks.getOrPut(videoId) { ReentrantLock() }

        lock.withLock {
            urlCache[videoId]?.let { (uri, timestamp) ->
                if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                    return uri
                }
            }

            val playbackSource = context.preferences.getEnum(playbackSourceKey, PlaybackSource.NewPipe)

            if (playbackSource == PlaybackSource.Automatic) {
                // TRY INNERTUBE FIRST (MUCH FASTER)
                val fastUri: Uri? = runCatching {
                    val response = runBlocking { Innertube.player(videoId)?.getOrNull() }
                    if (response == null) {
                        Log.e("Musynx-DataSource", "Innertube player response is null for $videoId")
                    } else {
                        Log.d("Musynx-DataSource", "Innertube response status: ${response.playabilityStatus?.status}")
                        if (response.streamingData?.highestQualityFormat == null) {
                            Log.e("Musynx-DataSource", "Highest quality format is null for $videoId. Formats count: ${response.streamingData?.adaptiveFormats?.size ?: 0}")
                            response.streamingData?.adaptiveFormats?.forEach { format ->
                                Log.d("Musynx-DataSource", "Format: itag=${format.itag}, mimeType=${format.mimeType}, hasUrl=${format.url != null}")
                            }
                        }
                    }
                    response?.streamingData?.highestQualityFormat?.url?.toUri()
                }.getOrElse { e ->
                    Log.e("Musynx-DataSource", "Innertube resolution error for $videoId", e)
                    null
                }

                    if (fastUri != null) {
                        Log.d("Musynx-DataSource", "Innertube resolution SUCCESS for $videoId: $fastUri")
                        urlCache[videoId] = Pair(fastUri, System.currentTimeMillis())
                        sourceTracker[videoId] = com.nitya.musynx.app.R.string.innertube
                        return fastUri
                    }
            }

            // FALLBACK TO NEWPIPE (SLOWER)
            val rawUrl = runCatching {
                val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                streamExtractor.fetchPage()

                val audioStreams = streamExtractor.audioStreams

                val bestAudio = audioStreams
                    .filter { it.codec?.lowercase(Locale.ROOT) == "opus" }
                    .maxByOrNull { it.averageBitrate }
                    ?: audioStreams.maxByOrNull { it.averageBitrate }
                    ?: streamExtractor.videoStreams.maxByOrNull { it.bitrate }
                    ?: throw Exception("No playable streams found by NewPipe for $videoId")

                bestAudio.content
            }.getOrElse { e ->
                Log.e("Musynx-Debug", "NewPipe resolution failed for $videoId (Network likely down)", e)
                // If we're offline, return a generic URI. CacheDataSource will still check the cache key.
                return "https://www.youtube.com/watch?v=$videoId".toUri()
            }

            val newUri = rawUrl.toUri()
            urlCache[videoId] = Pair(newUri, System.currentTimeMillis())
            sourceTracker[videoId] = com.nitya.musynx.app.R.string.newpipe

            return newUri
        }
    }
}

@UnstableApi
private class YouTube403ErrorPolicy(
    private val urlCache: ConcurrentHashMap<String, Pair<Uri, Long>>
) : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception

        if (exception is HttpDataSource.InvalidResponseCodeException && exception.responseCode == 403) {
            val videoId = loadErrorInfo.loadEventInfo.dataSpec.key
            Log.w("Musynx-Debug", "Hit a 403 Forbidden for $videoId! Evicting URL cache and retrying...")

            if (videoId != null) {
                urlCache.remove(videoId)
            } else {
                urlCache.clear()
            }
            return 1000L
        }

        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}
