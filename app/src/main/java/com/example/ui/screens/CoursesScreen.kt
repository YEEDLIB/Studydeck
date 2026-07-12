package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Lesson
import com.example.data.model.Playlist
import com.example.data.model.UserProgress
import com.example.ui.LearningViewModel
import com.example.ui.components.formatTime

// Custom dynamic gradient builder to make the offline app look colorful and inviting
fun getCategoryGradient(categoryName: String): Brush {
    val hash = categoryName.hashCode()
    val colors = when (kotlin.math.abs(hash) % 5) {
        0 -> listOf(Color(0xFF6366F1), Color(0xFFA855F7)) // Indigo to Purple
        1 -> listOf(Color(0xFFEC4899), Color(0xFFF43F5E)) // Pink to Rose
        2 -> listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)) // Blue to Cyan
        3 -> listOf(Color(0xFF10B981), Color(0xFF3B82F6)) // Emerald to Blue
        else -> listOf(Color(0xFFF59E0B), Color(0xFFEF4444)) // Amber to Red
    }
    return Brush.linearGradient(colors)
}

fun formatFriendlyDuration(ms: Long): String {
    if (ms <= 0) return "0m"
    val totalSeconds = ms / 1000
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / (60 * 60)
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatLastStudied(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 30 -> "${days} days ago"
        else -> "Recently"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: LearningViewModel,
    onNavigateToLesson: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val allProgress by viewModel.allProgress.collectAsState()

    // Drill-down navigation states
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()

    // Filter playlists and lessons based on selection (sorted by name/title)
    val filteredPlaylists = remember(selectedCategory, allPlaylists) {
        val cat = selectedCategory
        if (cat != null) {
            allPlaylists.filter { it.categoryId == cat.id }.sortedBy { it.name }
        } else {
            emptyList()
        }
    }

    val filteredLessons = remember(selectedPlaylist, lessons) {
        val play = selectedPlaylist
        if (play != null) {
            lessons.filter { it.playlistId == play.id }.sortedBy { it.title }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                selectedPlaylist != null -> selectedPlaylist!!.name
                                selectedCategory != null -> selectedCategory!!.name
                                else -> "🎓 Academic Syllabus"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = when {
                                selectedPlaylist != null -> "Browse structured playlist lessons"
                                selectedCategory != null -> "Structured study tracks"
                                else -> "Transforming raw folders to offline courses"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (selectedCategory != null || selectedPlaylist != null) {
                        IconButton(
                            onClick = {
                                if (selectedPlaylist != null) {
                                    viewModel.selectPlaylist(null)
                                } else {
                                    viewModel.selectCategory(null)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Step 3: Playlist Selected -> Display lessons list with robust statistics header
                selectedPlaylist != null -> {
                    if (filteredLessons.isEmpty()) {
                        EmptyListState(message = "No lessons found in this playlist.")
                    } else {
                        val playlistProgress = remember(filteredLessons, allProgress) {
                            val progressMap = allProgress.associateBy { it.lessonId }
                            filteredLessons.mapNotNull { progressMap[it.id] }
                        }
                        val completedCount = remember(playlistProgress) {
                            playlistProgress.count { it.completedStatus }
                        }
                        val playlistDuration = remember(filteredLessons) {
                            filteredLessons.sumOf { it.duration }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("lessons_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Playlist Status Header
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayLesson,
                                                contentDescription = "Playlist Details",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Syllabus Track Metrics",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$completedCount of ${filteredLessons.size} completed • Duration: ${formatFriendlyDuration(playlistDuration)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Lessons items
                            items(filteredLessons) { lesson ->
                                val progress = allProgress.find { it.lessonId == lesson.id }
                                EnhancedLessonCard(
                                    lesson = lesson,
                                    progress = progress,
                                    onToggleFavorite = { viewModel.toggleFavorite(lesson.id) },
                                    onClick = { onNavigateToLesson(lesson) }
                                )
                            }
                        }
                    }
                }

                // Step 2: Category Selected -> Display playlists list styled similar to YouTube courses
                selectedCategory != null -> {
                    if (filteredPlaylists.isEmpty()) {
                        EmptyListState(message = "No playlists found under this category.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("playlists_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredPlaylists) { playlist ->
                                val playlistLessons = remember(playlist, lessons) {
                                    lessons.filter { it.playlistId == playlist.id }
                                }
                                val progressMap = remember(allProgress) { allProgress.associateBy { it.lessonId } }
                                val completedCount = remember(playlistLessons, progressMap) {
                                    playlistLessons.count { progressMap[it.id]?.completedStatus == true }
                                }
                                val playlistDuration = remember(playlistLessons) {
                                    playlistLessons.sumOf { it.duration }
                                }
                                val lastOpenedTime = remember(playlistLessons, progressMap) {
                                    playlistLessons.mapNotNull { progressMap[it.id]?.lastOpened }.maxOrNull() ?: 0L
                                }

                                YouTubePlaylistCard(
                                    playlist = playlist,
                                    lessonCount = playlistLessons.size,
                                    completedCount = completedCount,
                                    duration = playlistDuration,
                                    lastOpened = lastOpenedTime,
                                    onClick = { viewModel.selectPlaylist(playlist) }
                                )
                            }
                        }
                    }
                }

                // Step 1: Default -> Display main categories list as modern Material 3 cards
                else -> {
                    if (categories.isEmpty()) {
                        EmptyListState(message = "Please configure a learning folder in Settings to start learning.")
                    } else {
                        val categoriesSorted = remember(categories) { categories.sortedBy { it.name } }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("categories_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(categoriesSorted) { category ->
                                val categoryPlaylists = remember(category, allPlaylists) {
                                    allPlaylists.filter { it.categoryId == category.id }.sortedBy { it.name }
                                }
                                val categoryPlaylistIds = remember(categoryPlaylists) {
                                    categoryPlaylists.map { it.id }.toSet()
                                }
                                val categoryLessons = remember(categoryPlaylistIds, lessons) {
                                    lessons.filter { it.playlistId in categoryPlaylistIds }
                                }
                                val progressMap = remember(allProgress) { allProgress.associateBy { it.lessonId } }
                                val hasProgress = remember(categoryLessons, progressMap) {
                                    categoryLessons.any { progressMap[it.id]?.completedStatus == false && (progressMap[it.id]?.watchPercentage ?: 0f) > 0f }
                                }

                                ModernCategoryCard(
                                    category = category,
                                    playlistCount = categoryPlaylists.size,
                                    lessonCount = categoryLessons.size,
                                    hasProgress = hasProgress,
                                    onClick = { viewModel.selectCategory(category) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernCategoryCard(
    category: Category,
    playlistCount: Int,
    lessonCount: Int,
    hasProgress: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("category_card_${category.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            // Category Cover Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(getCategoryGradient(category.name))
            ) {
                // Category Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Category Name display
                Text(
                    text = category.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )

                if (hasProgress) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Active Track",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "In Progress",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Stats Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Playlists",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$playlistCount ${if (playlistCount == 1) "Playlist" else "Playlists"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Lessons",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$lessonCount ${if (lessonCount == 1) "Lesson" else "Lessons"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Explore",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun YouTubePlaylistCard(
    playlist: Playlist,
    lessonCount: Int,
    completedCount: Int,
    duration: Long,
    lastOpened: Long,
    onClick: () -> Unit
) {
    val completionPercentage = if (lessonCount == 0) 0 else ((completedCount.toFloat() / lessonCount.toFloat()) * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("playlist_card_${playlist.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            // Large 16:9 Aspect Ratio cover banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(getCategoryGradient(playlist.name))
            ) {
                // Stack graphics effect on side
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = "Playlist stack",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$lessonCount",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "LESSONS",
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                // Beautiful playlist badge on top left
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatFriendlyDuration(duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Completion status banner at bottom
                if (completionPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                            .align(Alignment.BottomCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(completionPercentage / 100f)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Playlist Details
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sub-details
                    Column {
                        Text(
                            text = "$completedCount completed • $lessonCount Lessons",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lastOpened > 0) {
                            Text(
                                text = "Last studied: ${formatLastStudied(lastOpened)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Circular Progress indicator
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { completionPercentage / 100f },
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "$completionPercentage%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedLessonCard(
    lesson: Lesson,
    progress: UserProgress?,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var isFav by remember(lesson) { mutableStateOf(lesson.isFavorite) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("lesson_card_${lesson.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lesson Video Thumbnail block
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 62.dp)
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
                        // Play placeholder with neat gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(getCategoryGradient(lesson.title))
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play icon",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                        )
                    }

                    // Duration Badge Overlay
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = formatTime(lesson.duration),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Lesson details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (progress?.completedStatus == true) "Completed" else if (progress != null && progress.watchPercentage > 0f) "Watched ${progress.watchPercentage.toInt()}%" else "Not Started",
                        fontSize = 11.sp,
                        color = if (progress?.completedStatus == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (progress?.completedStatus == true) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Interactive favorite button directly on list row
                IconButton(
                    onClick = {
                        isFav = !isFav
                        onToggleFavorite()
                        Toast.makeText(
                            context,
                            if (isFav) "Added to Favorites" else "Removed from Favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("lesson_fav_${lesson.id}")
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Compact Watch progress indicator line
            if (progress != null && progress.watchPercentage > 0f) {
                LinearProgressIndicator(
                    progress = { progress.watchPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp),
                    color = if (progress.completedStatus) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun EmptyListState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

