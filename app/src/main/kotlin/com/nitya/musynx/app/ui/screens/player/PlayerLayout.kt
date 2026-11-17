package com.nitya.musynx.app.ui.screens.player

import androidx.compose.runtime.Composable
import com.nitya.musynx.app.enums.PlayerLayout
import com.nitya.musynx.app.utils.playerlayout
import com.nitya.musynx.app.utils.rememberPreference

@Composable
fun PlayerLayout(
    expandProgress: Float,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToTrackDetails: () -> Unit,
    onBack: () -> Unit,
    showPlaylist: Boolean,
    onLyricsClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onSleepTimerClick: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    showLyrics: Boolean,
    onTogglePlaylist: (Boolean) -> Unit
) {
    val playerLayoutState = rememberPreference(playerlayout, PlayerLayout.Default)
    val currentLayout = playerLayoutState.value

    MainPlayerContent(
        expandProgress = expandProgress,
        layoutMode = currentLayout,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        onTrackDetailsClick = onGoToTrackDetails,
        onAddToPlaylist = onAddToPlaylist,
        onBack = onBack,
        showPlaylist = showPlaylist,
        onLyricsClick = onLyricsClick,
        onSettingsClick = onSettingsClick,
        onSleepTimerClick = onSleepTimerClick,
        showLyrics = showLyrics,
        onTogglePlaylist = onTogglePlaylist

    )
}
