package com.nitya.musynx.app.enums

import androidx.annotation.StringRes
import com.nitya.musynx.app.R

enum class ProgressBar(
    @get:StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.defualt,
    ),
    Wave(
        resourceId = R.string.wave,
    ),
    Paperboat(
    resourceId = R.string.paperboat,
    )
}