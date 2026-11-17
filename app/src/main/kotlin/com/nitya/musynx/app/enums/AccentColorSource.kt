package com.nitya.musynx.app.enums

import androidx.annotation.StringRes
import com.nitya.musynx.app.R

enum class AccentColorSource(
    @get:StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.default_source
    ),
    Dynamic(
        resourceId = R.string.dynamic,
    ),
    MaterialYou(
        resourceId = R.string.material_you,
    )
}