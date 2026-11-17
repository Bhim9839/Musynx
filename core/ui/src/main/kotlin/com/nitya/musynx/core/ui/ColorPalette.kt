package com.nitya.musynx.core.ui

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

typealias ParcelableColor = @WriteWith<ColorParceler> Color

@Parcelize
@Immutable
data class ColorPalette(
    val background0: ParcelableColor,
    val background1: ParcelableColor,
    val background2: ParcelableColor,
    val iconColor: ParcelableColor,
    val accent: ParcelableColor,
    val onAccent: ParcelableColor,
    val black: ParcelableColor = Color.Black,
    val red: ParcelableColor = Color(0xffbf4040),
    val blue: ParcelableColor = Color(0xff4472cf),
    val yellow: ParcelableColor = Color(0xfffff176),
    val text: ParcelableColor,
    val textSecondary: ParcelableColor,
    val textDisabled: ParcelableColor,
    val isDefault: Boolean,
    val isDark: Boolean
) : Parcelable {
    @IgnoredOnParcel
    val background3: Color get() = if (isDark) Color.Black else Color.White

    @IgnoredOnParcel
    val background4: Color get() = if (isDark) Color.Black else Color(0xFFF6F6F8)

    @IgnoredOnParcel
    val mainBackground: Color get() = if (isDark) background0 else Color.White

    @IgnoredOnParcel
    val cardColor: Color get() = if (isDark) Color(0xFF151926) else Color(0xFFF1F5F9)

    @IgnoredOnParcel
    val boxColor: Color get() = if (isDark) Color(0xFF1E2333) else Color.White

    @IgnoredOnParcel
    val glass: Color get() = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    companion object
}

private val defaultAccentColor = Color(0xFF8B5CF6).hsl

val defaultLightPalette = ColorPalette(
    background0 = Color(0xFFF8FAFC),
    background1 = Color(0xFFF1F5F9),
    background2 = Color(0xFFE2E8F0),
    iconColor = Color(0xFF0F172A),
    text = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    textDisabled = Color(0xFF94A3B8),
    accent = defaultAccentColor.color,
    onAccent = Color.White,
    isDefault = true,
    isDark = false
)

val defaultDarkPalette = ColorPalette(
    background0 = Color(0xFF0B0D14),
    background1 = Color(0xFF131722),
    background2 = Color(0xFF1D2436),
    iconColor = Color(0xFFF8FAFC),
    text = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textDisabled = Color(0xFF64748B),
    accent = defaultAccentColor.color,
    onAccent = Color.White,
    isDefault = true,
    isDark = true
)

private fun lightColorPalette(accent: Hsl) = ColorPalette(
    background0 = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.1f),
        lightness = 0.925f
    ),
    background1 = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.3f),
        lightness = 0.90f
    ),
    background2 = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.4f),
        lightness = 0.85f
    ),
    iconColor = Color.Black,
    accent = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.5f),
        lightness = 0.5f
    ),
    onAccent = Color.White,
    text = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.02f),
        lightness = 0.12f
    ),
    textSecondary = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.1f),
        lightness = 0.40f
    ),
    textDisabled = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.2f),
        lightness = 0.65f
    ),
    isDefault = false,
    isDark = false
)

private fun darkColorPalette(accent: Hsl, darkness: Darkness) = ColorPalette(
    background0 = if (darkness == Darkness.Normal) Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.1f),
        lightness = 0.10f
    ) else Color.Black,
    background1 = if (darkness == Darkness.Normal) Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.3f),
        lightness = 0.15f
    ) else Color.Black,
    background2 = if (darkness == Darkness.Normal) Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.4f),
        lightness = 0.2f
    ) else Color.Black,
    iconColor = Color.White,
    accent = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(if (darkness == Darkness.AMOLED) 0.4f else 0.5f),
        lightness = 0.5f
    ),
    onAccent = Color.White,
    text = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.02f),
        lightness = 0.88f
    ),
    textSecondary = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.1f),
        lightness = 0.65f
    ),
    textDisabled = Color.hsl(
        hue = accent.hue,
        saturation = accent.saturation.coerceAtMost(0.2f),
        lightness = 0.40f
    ),
    isDefault = false,
    isDark = true
)

fun accentColorOf(
    source: ColorSource,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
): Hsl = when (source) {
    ColorSource.Default -> defaultAccentColor
    ColorSource.Dynamic -> sampleBitmap?.let { dynamicAccentColorOf(it, isDark) } ?: defaultAccentColor
    ColorSource.MaterialYou -> materialAccentColor?.hsl ?: defaultAccentColor
}

fun dynamicAccentColorOf(
    bitmap: Bitmap,
    isDark: Boolean
): Hsl? {
    val builder = Palette.from(bitmap).maximumColorCount(8)
    if (isDark) {
        builder.addFilter { _, hsl -> hsl[0] !in 36f..100f }
    }

    var palette = builder.generate()
    var swatch = palette.dominantSwatch

    if (isDark && swatch == null) {
        palette = Palette.from(bitmap).maximumColorCount(8).generate()
        swatch = palette.dominantSwatch
    }

    val hslArray = swatch?.hsl ?: return null

    if (hslArray[1] < 0.08f) {
        val colorfulSwatch = palette.swatches.maxByOrNull { it.hsl[1] }

        if (colorfulSwatch != null && colorfulSwatch.hsl[1] > 0f) {
            return colorfulSwatch.hsl.hsl
        }
    }
    return hslArray.hsl
}

fun colorPaletteOf(
    source: ColorSource,
    darkness: Darkness,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
): ColorPalette {
    val accentColor = accentColorOf(source, isDark, materialAccentColor, sampleBitmap)
    val isDefaultAccent = accentColor == defaultAccentColor

    if (isDefaultAccent) {
        if (!isDark) return defaultLightPalette
        if (darkness == Darkness.Normal) return defaultDarkPalette
    }

    return if (isDark) {
        darkColorPalette(accentColor, darkness).copy(isDefault = isDefaultAccent)
    } else {
        lightColorPalette(accentColor).copy(isDefault = false)
    }
}

inline val ColorPalette.isPureBlack get() = background0 == Color.Black

inline val ColorPalette.favoritesIcon: ParcelableColor
    get() = if (isDefault) red else accent

inline val ColorPalette.surface: ParcelableColor
    get() = if (isPureBlack) Color(0xff272727) else background2

object ColorParceler : Parceler<Color> {
    override fun Color.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this.value.toLong())
    }

    override fun create(parcel: Parcel): Color {
        val colorValue = parcel.readLong()
        return Color(colorValue.toULong())
    }
}