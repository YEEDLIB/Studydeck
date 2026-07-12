package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lesson
import com.example.data.model.Playlist
import com.example.data.model.UserProgress
import com.example.ui.LearningViewModel
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.formatTime
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    viewModel: LearningViewModel,
    lesson: Lesson,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    BackHandler {
        onBack()
    }

    // State bindings
    val currentProgress by viewModel.getProgressFlowByLesson(lesson.id).collectAsState(initial = null)
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    var isFavorite by remember(lesson) { mutableStateOf(lesson.isFavorite) }

    var isProgressLoaded by remember { mutableStateOf(false) }
    var resumePosition by remember { mutableStateOf(0L) }
    var initialPosition by remember { mutableStateOf(0L) }
    var showResumePrompt by remember { mutableStateOf(false) }

    LaunchedEffect(lesson.id) {
        viewModel.getProgressFlowByLesson(lesson.id).collect { progress ->
            if (!isProgressLoaded) {
                if (progress != null && progress.lastPosition > 3000L && progress.lastPosition < (lesson.duration - 5000L)) {
                    resumePosition = progress.lastPosition
                    showResumePrompt = true
                } else {
                    initialPosition = 0L
                    showResumePrompt = false
                }
                isProgressLoaded = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Video player card (Inset & sized with beautifully curved edges to feel premium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                )
                .background(Color.Black)
                .aspectRatio(16f / 9f)
                .testTag("video_player_box")
        ) {
            if (isProgressLoaded) {
                if (showResumePrompt) {
                    // Modern, translucent high-fidelity Resume overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Resume Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = "RESUME PLAYBACK?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Would you like to resume from where you left off?",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = formatTime(resumePosition),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.5.sp
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Start over" button
                            OutlinedButton(
                                onClick = {
                                    initialPosition = 0L
                                    showResumePrompt = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Restart",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start Over",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // "Resume" button
                            Button(
                                onClick = {
                                    initialPosition = resumePosition
                                    showResumePrompt = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Resume",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Start Video player
                    VideoPlayerView(
                        videoUrl = lesson.videoPath,
                        initialPosition = initialPosition,
                        playbackSpeed = playbackSpeed,
                        onProgressUpdate = { pos, duration ->
                            viewModel.updateLessonProgress(lesson.id, pos, duration)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Beautiful translucent loading indicator placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Floating translucent Back Action button over the video canvas
            FilledIconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Floating translucent Favorite Toggle button over the video canvas
            FilledIconButton(
                onClick = {
                    isFavorite = !isFavorite
                    viewModel.toggleFavorite(lesson.id)
                    Toast.makeText(
                        context,
                        if (isFavorite) "Added to Favorites" else "Removed from Favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .testTag("lesson_favorite_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = if (isFavorite) Color.Red else Color.White
                )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Details Container
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. YouTube-style Title & Meta Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = lesson.title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 24.sp,
                    modifier = Modifier.testTag("lesson_title")
                )
            }

            // 2. Playlist / Channel Header with Favorite Pill (YouTube Subscription Row Style)
            val allPlaylists by viewModel.allPlaylists.collectAsState()
            val allLessons by viewModel.lessons.collectAsState()
            val allProgress by viewModel.allProgress.collectAsState()

            val playlist = remember(allPlaylists, lesson.playlistId) {
                allPlaylists.find { it.id == lesson.playlistId }
            }
            val playlistLessons = remember(allLessons, lesson.playlistId) {
                allLessons.filter { it.playlistId == lesson.playlistId }.sortedBy { it.title }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(
                            text = playlist?.name ?: "Course Playlist",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlistLessons.size} lessons",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // YouTube-like favorite action button
                Button(
                    onClick = {
                        isFavorite = !isFavorite
                        viewModel.toggleFavorite(lesson.id)
                        Toast.makeText(
                            context,
                            if (isFavorite) "Added to Favorites" else "Removed from Favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFavorite) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        contentColor = if (isFavorite) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.background
                        }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).testTag("lesson_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(15.dp),
                        tint = if (isFavorite) Color.Red else Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFavorite) "Liked" else "Like",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Playback Speed Selector (YouTube action pill row style)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYBACK SPEED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${playbackSpeed}x",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("speed_selector_card"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updatePlaybackSpeed(speed) },
                            label = { Text("${speed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // 4. Study Progress Log Card
            if (currentProgress != null) {
                val percentage = currentProgress!!.watchPercentage.toInt()
                val isCompleted = percentage >= 95

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Status",
                                    tint = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isCompleted) "Completed" else "In Progress",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "$percentage%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { currentProgress!!.watchPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            trackColor = (if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // 5. Expandable YouTube-style Description Box
            var isDescExpanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable { isDescExpanded = !isDescExpanded }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Description",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Description",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (isDescExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isDescExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = lesson.description ?: "No description available for this lesson.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines = if (isDescExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("lesson_description_text")
                )
            }

            // 6. Playlist section under Description
            if (playlistLessons.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    
                    // Playlist Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playlist Lessons",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        // Current playing index position
                        val currentIndex = playlistLessons.indexOfFirst { it.id == lesson.id }
                        if (currentIndex != -1) {
                            Text(
                                text = "${currentIndex + 1} / ${playlistLessons.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Lessons List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        playlistLessons.forEach { otherLesson ->
                            val isActive = otherLesson.id == lesson.id
                            val progress = allProgress.find { it.lessonId == otherLesson.id }
                            
                            CompactLessonCard(
                                lesson = otherLesson,
                                isActive = isActive,
                                progress = progress,
                                onClick = {
                                    if (!isActive) {
                                        viewModel.selectLesson(otherLesson)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CompactLessonCard(
    lesson: Lesson,
    isActive: Boolean,
    progress: UserProgress?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("compact_lesson_card_${lesson.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Block
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!lesson.thumbnailPath.isNullOrBlank()) {
                    AsyncImage(
                        model = lesson.thumbnailPath,
                        contentDescription = "Lesson thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getCategoryGradient(lesson.title))
                    )
                    Icon(
                        imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.PlayCircle,
                        contentDescription = "Play Icon",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Lesson Details Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Watch Status label
                    val progressText = when {
                        isActive -> "NOW PLAYING"
                        progress?.completedStatus == true -> "COMPLETED"
                        progress != null && progress.watchPercentage > 0f -> "Watched ${progress.watchPercentage.toInt()}%"
                        else -> "Up Next"
                    }
                    
                    Text(
                        text = progressText,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else if (progress?.completedStatus == true) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        fontSize = 10.sp,
                        fontWeight = if (isActive || progress?.completedStatus == true) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

