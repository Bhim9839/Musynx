package com.nitya.musynx.innertube.utils

import io.ktor.client.plugins.compression.ContentEncodingConfig

fun ContentEncodingConfig.brotli(quality: Float? = null) {
    customEncoder(BrotliEncoder, quality)
}
