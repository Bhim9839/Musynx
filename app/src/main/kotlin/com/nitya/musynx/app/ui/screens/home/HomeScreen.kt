@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.nitya.musynx.app.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nitya.musynx.core.ui.LocalAppearance
import com.nitya.musynx.app.LocalPlayerPadding
import com.nitya.musynx.app.R
import com.nitya.musynx.app.enums.BuiltInPlaylist
import com.nitya.musynx.app.ui.components.BannerAdView
import com.nitya.musynx.app.ui.components.ClassicHorizontalTabs
import com.nitya.musynx.app.ui.components.HorizontalTabs
import com.nitya.musynx.app.ui.components.SettingsCard
import com.nitya.musynx.app.ui.components.StaticScreenLayout
import com.nitya.musynx.app.ui.navigation.Routes
import com.nitya.musynx.app.ui.screens.favorites.FavoritesScreen
import com.nitya.musynx.app.utils.HomeTab
import com.nitya.musynx.app.utils.TabStyle
import com.nitya.musynx.app.utils.rememberPreference
import com.nitya.musynx.app.utils.tabStyleKey

@Composable
fun HomeScreen(
    navController: NavController,
    onSettingsClick: () -> Unit,
) {
    val tabStyle by rememberPreference(tabStyleKey, TabStyle.Classic)
    val activeTabs = HomeTab.entries.filter { tab ->
        rememberPreference(tab.key, true).value
    }.ifEmpty { listOf(HomeTab.Home) }

    val pagerState = rememberPagerState(initialPage = 0) { activeTabs.size }
    val navigateToAlbum = { browseId: String ->
        navController.navigate(route = Routes.Album(id = browseId))
    }
    val navigateToArtist = { browseId: String ->
        navController.navigate(route = Routes.Artist(id = browseId))
    }

    val (colorPalette) = LocalAppearance.current

    StaticScreenLayout(
        title = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(colorPalette.accent, Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.latest_icon),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Text(
                    text = "Musynx",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = colorPalette.text
                )
            }
        },
        scrollable = false,
        horizontalPadding = 0.dp,
        actions = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                androidx.compose.material3.Surface(
                    onClick = { navController.navigate(route = Routes.Search) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (colorPalette.isDark) Color(0xFF161A26).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, colorPalette.accent.copy(alpha = 0.25f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(38.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = colorPalette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.search),
                            color = colorPalette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                androidx.compose.material3.Surface(
                    onClick = onSettingsClick,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (colorPalette.isDark) Color(0xFF161A26).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, if (colorPalette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colorPalette.text,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) {
        val tabTitles = activeTabs.map { it.title }

        if (tabStyle == TabStyle.Modern) {
            HorizontalTabs(
                pagerState = pagerState,
                tabs = tabTitles
            )
        } else {
            ClassicHorizontalTabs(
                pagerState = pagerState,
                tabs = tabTitles
            )
        }

        SettingsCard(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 4,
                    modifier = Modifier.weight(1f)
                ) { page ->
                when (activeTabs[page]) {
                    HomeTab.Home -> QuickPicks(
                        onAlbumClick = navigateToAlbum,
                        onArtistClick = navigateToArtist,
                        onPlaylistClick = { browseId ->
                            navController.navigate(route = Routes.Playlist(id = browseId))
                        },
                        onHistoryViewAllClick = {
                            navController.navigate(route = Routes.History)
                        },
                        onOfflinePlaylistClick = {
                            navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
                        }
                    )

                    HomeTab.Favorites -> FavoritesScreen(
                        onFavoriteTracksClick = { navController.navigate(route = Routes.FavoriteTracks) },
                        onGoToAlbum = navigateToAlbum,
                        onGoToArtist = navigateToArtist,
                        isEmbedded = true
                    )

                    HomeTab.Following -> FollowingScreen(
                        onArtistClick = navigateToArtist,
                        onAlbumClick = navigateToAlbum
                    )

                    HomeTab.Songs -> HomeSongs(
                        onGoToAlbum = navigateToAlbum,
                        onGoToArtist = navigateToArtist
                    )

                    HomeTab.Artists -> HomeArtistList(
                        onArtistClick = { artist -> navigateToArtist(artist.id) }
                    )

                    HomeTab.Albums -> HomeAlbums(
                        onAlbumClick = { album -> navigateToAlbum(album.id) }
                    )

                    HomeTab.Playlists -> HomePlaylists(
                        onBuiltInPlaylist = { playlistIndex ->
                            if (playlistIndex == BuiltInPlaylist.Favorites.ordinal) {
                                navController.navigate(route = Routes.Favorites)
                            } else {
                                navController.navigate(route = Routes.BuiltInPlaylist(index = playlistIndex))
                            }
                        },
                        onPlaylistClick = { playlist ->
                            navController.navigate(route = Routes.LocalPlaylist(id = playlist.id))
                        }
                    )
                }
            }
        }
    }
}
}
