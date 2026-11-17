package com.nitya.musynx.app.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitya.musynx.app.R
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(
    onAnimationFinished: () -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val waveProgress = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splashInfinite")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rotationDegree by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationDegree"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
        waveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(800)
        )
        delay(1600)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAnimationFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow orbs
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(pulseScale)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.35f),
                            Color(0xFFEC4899).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(260.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF06B6D4).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Concentric audio ripple canvas
        Canvas(modifier = Modifier.size(340.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = 75.dp.toPx()
            val maxExtra = 85.dp.toPx()

            for (i in 0..2) {
                val phase = (waveOffset + i * 0.33f) % 1f
                val radius = baseRadius + phase * maxExtra
                val ringAlpha = (1f - phase).coerceIn(0f, 1f) * 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = ringAlpha),
                            Color(0xFFEC4899).copy(alpha = ringAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Rotating gradient halo around app logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value * pulseScale)
                    .alpha(alpha.value)
            ) {
                // Gradient Ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationDegree)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6),
                                    Color(0xFFEC4899),
                                    Color(0xFF06B6D4),
                                    Color(0xFF8B5CF6)
                                )
                            )
                        )
                )

                // Inner core
                Box(
                    modifier = Modifier
                        .size(122.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E121B)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.latest_icon),
                        contentDescription = "Musynx Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name with modern gradient typography
            Text(
                text = "Musynx",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Modern Subtitle
            Text(
                text = "FEEL THE RHYTHM • REDEFINED",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = Color(0xFFA1A1AA),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Equalizer Visualizer Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .height(26.dp)
                    .alpha(textAlpha.value)
            ) {
                val heights = listOf(0.4f, 0.9f, 0.6f, 1f, 0.7f, 0.35f, 0.8f)
                val speeds = listOf(600, 750, 500, 900, 650, 800, 700)

                heights.forEachIndexed { index, baseHeight ->
                    val barAnim by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = baseHeight,
                        animationSpec = infiniteRepeatable(
                            animation = tween(speeds[index], easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "splashBar$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height((barAnim * 26).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFEC4899),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
