package com.nitya.musynx.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitya.musynx.core.ui.LocalAppearance
import kotlinx.coroutines.launch

@Composable
fun ClassicHorizontalTabs(
    pagerState: PagerState,
    tabs: List<Int>,
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = pagerState.currentPage
    val (colorPalette) = LocalAppearance.current
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTabIndex) {
        listState.animateScrollToItem((selectedTabIndex - 1).coerceAtLeast(0))
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(44.dp)
        ) {
            itemsIndexed(tabs) { index, titleRes ->
                val isSelected = selectedTabIndex == index

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.04f else 0.98f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "tabScale$index"
                )

                val gradient = Brush.linearGradient(
                    colors = listOf(
                        colorPalette.accent,
                        Color(0xFFEC4899)
                    )
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(scale)
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(gradient)
                            } else {
                                Modifier
                                    .background(
                                        if (colorPalette.isDark) Color(0xFF161A26).copy(alpha = 0.6f)
                                        else Color.White.copy(alpha = 0.7f)
                                    )
                                    .border(
                                        1.dp,
                                        if (colorPalette.isDark) Color.White.copy(alpha = 0.08f)
                                        else Color.Black.copy(alpha = 0.06f),
                                        CircleShape
                                    )
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = stringResource(id = titleRes),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else colorPalette.textSecondary
                    )
                }
            }
        }
    }
}