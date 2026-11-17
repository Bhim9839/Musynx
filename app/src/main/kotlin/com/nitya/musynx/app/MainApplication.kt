package com.nitya.musynx.app

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.app.enums.CoilDiskCacheMaxSize
import com.nitya.musynx.app.utils.coilDiskCacheMaxSizeKey
import com.nitya.musynx.app.utils.getEnum
import com.nitya.musynx.app.utils.pauseImageCacheKey
import com.nitya.musynx.app.utils.preferences
import org.schabi.newpipe.extractor.NewPipe


class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Thread {
            DatabaseInitializer.get(this)
            NewPipe.init(NewPipeDownloader.getInstance())
            com.google.android.gms.ads.MobileAds.initialize(this) {}

            Innertube.visitorData = preferences.getString("visitor_data", null)
            Innertube.onVisitorDataChanged = { visitorData: String? ->
                preferences.edit { putString("visitor_data", visitorData) }
            }
        }.start()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .components {
                add { chain ->
                    val request = if (preferences.getBoolean(pauseImageCacheKey, false)) {
                        chain.request.newBuilder()
                            .diskCachePolicy(CachePolicy.READ_ONLY)
                            .build()
                    } else {
                        chain.request
                    }
                    chain.withRequest(request).proceed()
                }
            }
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(
                        preferences.getEnum(
                            coilDiskCacheMaxSizeKey,
                            CoilDiskCacheMaxSize.`128MB`
                        ).bytes
                    )
                    .build()
            )
            .build()
    }

    companion object {
        private var instance: MainApplication? = null
        val appContext: Context get() = instance!!.applicationContext
    }
}
