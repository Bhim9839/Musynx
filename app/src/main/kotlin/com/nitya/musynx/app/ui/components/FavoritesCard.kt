package com.nitya.musynx.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nitya.musynx.core.ui.LocalAppearance
import com.nitya.musynx.app.ui.common.IconSource
import com.nitya.musynx.app.utils.thumbnail

@Composable
fun FavoritesCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: IconSource? = null,
    onClick: () -> Unit,
    thumbnailUrls: List<String> = emptyList(),
    label: String? = null,
    subtitle: String? = null,
    onPlayClick: (() -> Unit)? = null
) {
    val (colorPalette) = LocalAppearance.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colorPalette.cardColor)
            .border(
                1.dp,
                if (colorPalette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                MaterialTheme.shapes.extraLarge
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrls.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (thumbnailUrls.size == 1) {
                    AsyncImage(
                        model = thumbnailUrls.first().thumbnail(512),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd
                    ).forEachIndexed { index, alignment ->
                        AsyncImage(
                            model = thumbnailUrls.getOrNull(index)?.thumbnail(256),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(alignment)
                                .fillMaxSize(0.51f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        } else if (icon != null) {
            when (icon) {
                is IconSource.Vector -> Icon(
                    imageVector = icon.imageVector,
                    contentDescription = title,
                    tint = colorPalette.accent.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                )
                is IconSource.Icon -> Icon(
                    painter = icon.painter,
                    contentDescription = title,
                    tint = colorPalette.accent.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                )
            }
        }

        if (onPlayClick != null) {
            IconButton(
                onClick = { onPlayClick() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colorPalette.accent, Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            if (title !=null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
