package com.nitya.musynx.app.enums

import androidx.annotation.StringRes
import com.nitya.musynx.app.R

enum class QuickPicksSource(
    @StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.defualt,
    ),
    Custom(
        resourceId = R.string.custom,
    )
}
