@file:Suppress("SpellCheckingInspection")

package com.nitya.musynx.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.requests.playlistPage
import com.nitya.musynx.innertube.requests.song
import com.nitya.musynx.app.enums.AppThemeColor
import com.nitya.musynx.app.models.LocalMenuState
import com.nitya.musynx.app.service.PlayerService
import com.nitya.musynx.app.ui.components.YouTubeWebView
import com.nitya.musynx.app.ui.navigation.MainNavigation
import com.nitya.musynx.app.ui.navigation.Routes
import com.nitya.musynx.app.ui.navigation.SettingsDestinations
import com.nitya.musynx.app.ui.screens.player.SharedPlayer
import com.nitya.musynx.app.ui.styling.AppTheme
import com.nitya.musynx.app.utils.appTheme
import com.nitya.musynx.app.utils.asMediaItem
import com.nitya.musynx.app.utils.forcePlay
import com.nitya.musynx.app.utils.intent
import com.nitya.musynx.app.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) this@MainActivity.binder = service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){}
    private var binder by mutableStateOf<PlayerService.Binder?>(null)
    private var data by mutableStateOf<Uri?>(null)

    override fun onStart() {
        super.onStart()
        bindService(intent<PlayerService>(), serviceConnection, BIND_AUTO_CREATE)
    }

    @UnstableApi
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        } else {

            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.BLACK
        }
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val launchedFromNotification = intent?.extras?.getBoolean("expandPlayerBottomSheet") == true
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()

        setContent {
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val updateFile = File(externalCacheDir, "update.apk")
                    if (updateFile.exists()) {
                        updateFile.delete()
                    }
                    externalCacheDir?.listFiles()?.forEach {
                        if (it.name.startsWith("update_") && it.name.endsWith(".apk")) it.delete()
                    }
                }
                reportFullyDrawn()
            }

            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            var isPlayerVisible by remember { mutableStateOf(true) }
            var showSplashScreen by remember { mutableStateOf(!launchedFromNotification) }

            val playerState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                confirmValueChange = { value ->
                    value != SheetValue.Hidden
                },
                skipHiddenState = true
            )

            val appTheme by rememberPreference(appTheme, AppThemeColor.System)

            val darkTheme = when (appTheme) {
                AppThemeColor.System -> isSystemInDarkTheme()
                AppThemeColor.Dark -> true
                AppThemeColor.Light -> false
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }

            AppTheme(
                darkTheme = darkTheme,
                usePureBlack = false,
                useMaterialNeutral = false,
            ) {
                YouTubeWebView()
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CompositionLocalProvider(value = LocalPlayerServiceBinder provides binder) {
                        val menuState = LocalMenuState.current

                        SharedPlayer(
                            navController = navController,
                            sheetState = playerState,
                            scaffoldPadding = WindowInsets.navigationBars.asPaddingValues(),
                            showPlayer = isPlayerVisible,
                            onNavigateToSettings = {
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            },
                            onNavigateToSleepTimer = {
                                val intent = Intent(context, SettingsActivity::class.java).apply {
                                    putExtra("SCREEN_ID", SettingsDestinations.SLEEP_TIMER)
                                }
                                context.startActivity(intent)
                            },
                            onNavigateToPlaybackSourceSettings = {
                                val intent = Intent(context, SettingsActivity::class.java).apply {
                                    putExtra("SCREEN_ID", SettingsDestinations.PLAYER)
                                    putExtra("HIGHLIGHT_SETTING", "playbackSource")
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            MainNavigation(
                                navController = navController,
                                sheetState = playerState,
                                onNavigateToSettings = {
                                    val intent = Intent(context, SettingsActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                        }

                        if (menuState.isDisplayed) {
                            ModalBottomSheet(
                                onDismissRequest = menuState::hide,
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                dragHandle = {
                                    Surface(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        shape = MaterialTheme.shapes.extraLarge
                                    ) {
                                        Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
                                    }
                                }
                            ) {
                                menuState.content()
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSplashScreen,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) +
                                androidx.compose.animation.scaleOut(targetScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(500))
                    ) {
                        com.nitya.musynx.app.ui.screens.splash.AnimatedSplashScreen(
                            onAnimationFinished = {
                                showSplashScreen = false
                            }
                        )
                    }
                }
            }

            DisposableEffect(binder?.player) {
                val player = binder?.player ?: return@DisposableEffect onDispose { }

                if (player.currentMediaItem != null) {
                    if (launchedFromNotification) {
                        intent.replaceExtras(Bundle())
                        scope.launch { playerState.expand() }
                    }
                }

                var lastMediaItem: MediaItem? = player.currentMediaItem

                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null) {
                            if (lastMediaItem == null) {
                                if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                    scope.launch { playerState.expand() }
                                }
                            }
                        }
                        lastMediaItem = mediaItem
                    }
                }

                player.addListener(listener)
                onDispose { player.removeListener(listener) }
            }

            LaunchedEffect(data) {
                val uri = data ?: return@LaunchedEffect

                lifecycleScope.launch(Dispatchers.Main) {
                    when (val path = uri.pathSegments.firstOrNull()) {
                        "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                            val browseId = "VL$playlistId"

                            if (playlistId.startsWith("OLAK5uy_")) {
                                Innertube.playlistPage(browseId = browseId)?.getOrNull()
                                    ?.let {
                                        it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                            navController.navigate(
                                                route = Routes.Album(id = browseId)
                                            )
                                        }
                                    }
                            } else navController.navigate(
                                route = Routes.Playlist(id = browseId)
                            )
                        }

                        "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                            navController.navigate(
                                route = Routes.Artist(id = channelId)
                            )
                        }

                        else -> when {
                            path == "watch" -> uri.getQueryParameter("v")
                            uri.host == "youtu.be" -> path
                            else -> null
                        }?.let { videoId ->
                            Innertube.song(videoId)?.getOrNull()?.let { song ->
                                val binder = snapshotFlow { binder }.filterNotNull().first()
                                withContext(Dispatchers.Main) {
                                    binder.player.forcePlay(song.asMediaItem)
                                }
                            }
                        }
                    }
                }

                data = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()


        if (intent.getBooleanExtra("NAVIGATE_TO_ABOUT", false)) {
            val settingsIntent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("SCREEN_ID", SettingsDestinations.ABOUT)
            }
            startActivity(settingsIntent)
        }
        setIntent(intent)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }
}

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }
val LocalPlayerPadding = compositionLocalOf { 0.dp }