package com.nitya.musynx.app.ui.styling

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nitya.musynx.core.ui.BuiltInFontFamily.Poppins
import com.nitya.musynx.core.ui.ColorMode
import com.nitya.musynx.core.ui.ColorSource
import com.nitya.musynx.core.ui.Darkness
import com.nitya.musynx.core.ui.LocalAppearance
import com.nitya.musynx.core.ui.appearance

private val PureBlackColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF06B6D4),
    tertiary = Color(0xFFEC4899),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF10121A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E2232),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val OffsetWhiteColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF0284C7),
    tertiary = Color(0xFFDB2777),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B)
)

private val MaterialDarkScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF06B6D4),
    tertiary = Color(0xFFEC4899),
    background = Color(0xFF0B0D14),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131722),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1D2436),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val MaterialLightScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun AppTheme(
    usePureBlack: Boolean = false,
    useMaterialNeutral: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useMaterialNeutral && darkTheme -> MaterialDarkScheme
        useMaterialNeutral && !darkTheme -> MaterialLightScheme

        usePureBlack && darkTheme -> PureBlackColorScheme

        darkTheme -> MaterialDarkScheme
        else -> OffsetWhiteColorScheme
    }

    val materialAccentColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context).primary
    } else {
        null
    }

    val appearance = appearance(
        source = if (materialAccentColor != null) ColorSource.MaterialYou else ColorSource.Default,
        mode = if (darkTheme) ColorMode.Dark else ColorMode.Light,
        darkness = if (usePureBlack && darkTheme) Darkness.AMOLED else Darkness.Normal,
        materialAccentColor = materialAccentColor,
        sampleBitmap = null,
        fontFamily = Poppins,
        applyFontPadding = true,
        thumbnailRoundness = 16.dp
    )

    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(
            colorScheme = baseColorScheme,
            typography = Typography,
            content = content
        )
    }

}
