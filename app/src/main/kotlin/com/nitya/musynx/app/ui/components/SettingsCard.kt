package com.nitya.musynx.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nitya.musynx.core.ui.LocalAppearance

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = LocalAppearance.current.colorPalette.mainBackground,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = shape,
        color = color,
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
