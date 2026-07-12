package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoUrl: String,
    initialPosition: Long,
    playbackSpeed: Float,
    onProgressUpdate: (position: Long, duration: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize ExoPlayer (using application context to avoid memory leaks)
    val exoPlayer = remember {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // States for custom overlay controls
    var isPlaying by remember { mutableStateOf(true) }
    var currentPos by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // Smooth dragging states for the seek bar
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    // Synchronize play/pause state and release player on disposal
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Gesture control States (Volume & Brightness simulation)
    var volumeLevel by remember { mutableStateOf(0.7f) } // 0.0 to 1.0
    var brightnessLevel by remember { mutableStateOf(0.6f) } // 0.0 to 1.0
    var activeGestureText by remember { mutableStateOf("") }
    var showGestureIndicator by remember { mutableStateOf(false) }

    // Sync media source and playback speed
    LaunchedEffect(videoUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
        exoPlayer.seekTo(initialPosition)
        exoPlayer.playWhenReady = true
        isPlaying = true
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Auto-save and progress updating loop
    LaunchedEffect(exoPlayer, isPlaying, isDragging) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                if (!isDragging) {
                    currentPos = pos
                    totalDuration = dur
                }
                onProgressUpdate(pos, dur)
            }
            delay(1000)
        }
    }

    // Fade out controls timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        // Underlying ExoPlayer
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Custom overlay instead
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Volume / Brightness vertical drag gesture layer (Left side brightness, Right side volume)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                showGestureIndicator = true
                                activeGestureText = if (offset.x < size.width / 2) "Brightness" else "Volume"
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (activeGestureText == "Brightness") {
                                    brightnessLevel = (brightnessLevel - dragAmount.y / 500f).coerceIn(0f, 1.0f)
                                } else {
                                    volumeLevel = (volumeLevel - dragAmount.y / 500f).coerceIn(0f, 1.0f)
                                    exoPlayer.volume = volumeLevel
                                }
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    delay(1000)
                                    showGestureIndicator = false
                                }
                            }
                        )
                    }
            )
        }

        // Gesture Overlay Sliders
        if (showGestureIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (activeGestureText == "Volume") Icons.Default.VolumeUp else Icons.Default.BrightnessMedium,
                        contentDescription = activeGestureText,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    val englishText = if (activeGestureText == "Volume") "Volume" else "Brightness"
                    val percentageValue = (if (activeGestureText == "Volume") volumeLevel else brightnessLevel) * 100
                    
                    Text(
                        text = "$englishText: ${percentageValue.toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Small visual gauge indicator
                    LinearProgressIndicator(
                        progress = { if (activeGestureText == "Volume") volumeLevel else brightnessLevel },
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Custom Overlay Controls (Play, Seek, Lock, Fullscreen, Custom Speed)
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Floating translucent Lock button at center-left (avoids overlap with Back button at top-left)
                IconButton(
                    onClick = { isLocked = !isLocked },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Controls",
                        tint = if (isLocked) MaterialTheme.colorScheme.secondary else Color.White
                    )
                }

                // Middle Controls (Play, Rewind, Fast Forward with high-fidelity rings and outlines)
                if (!isLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPos = target
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(34.dp)),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(target)
                                currentPos = target
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Bottom Controls (Seek Bar, Progress numbers)
                if (!isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPos),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatTime(totalDuration),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Slider(
                            value = if (isDragging) dragPosition else (if (totalDuration > 0) currentPos.toFloat() / totalDuration.toFloat() else 0f),
                            onValueChange = { percent ->
                                isDragging = true
                                dragPosition = percent
                                currentPos = (percent * totalDuration).toLong()
                            },
                            onValueChangeFinished = {
                                val target = (dragPosition * totalDuration).toLong()
                                exoPlayer.seekTo(target)
                                currentPos = target
                                isDragging = false
                                onProgressUpdate(target, totalDuration)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0) return "00:00"
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
