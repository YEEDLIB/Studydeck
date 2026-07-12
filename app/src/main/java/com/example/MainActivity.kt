package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Lesson
import com.example.ui.LearningViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: LearningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                // Main Entry Control State
                var showSplash by remember { mutableStateOf(true) }
                val folderUri by viewModel.folderUriString.collectAsState()
                val isSimulated by viewModel.isSimulatedMode.collectAsState()

                // Bottom Tab state
                var selectedTab by remember { mutableStateOf(AppTab.HOME) }
                // Active Lesson viewing state
                val selectedLesson by viewModel.selectedLesson.collectAsState()

                // Request Permissions on launch
                LaunchedEffect(Unit) {
                    checkAndRequestPermissions()
                    delay(2200) // Beautiful splash dwell
                    showSplash = false
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when {
                        showSplash -> {
                            SplashScreen()
                        }

                        folderUri == null && !isSimulated -> {
                            SetupScreen(
                                viewModel = viewModel,
                                onSetupComplete = {
                                    selectedTab = AppTab.HOME
                                }
                            )
                        }

                        selectedLesson != null -> {
                            LessonScreen(
                                viewModel = viewModel,
                                lesson = selectedLesson!!,
                                onBack = {
                                    viewModel.selectLesson(null)
                                }
                            )
                        }

                        else -> {
                            val currentCategory by viewModel.selectedCategory.collectAsState()
                            val currentPlaylist by viewModel.selectedPlaylist.collectAsState()

                            if (selectedTab != AppTab.HOME || currentCategory != null || currentPlaylist != null) {
                                BackHandler {
                                    if (selectedTab == AppTab.COURSES) {
                                        when {
                                            currentPlaylist != null -> viewModel.selectPlaylist(null)
                                            currentCategory != null -> viewModel.selectCategory(null)
                                            else -> selectedTab = AppTab.HOME
                                        }
                                    } else {
                                        selectedTab = AppTab.HOME
                                    }
                                }
                            }

                            // Main Application Shell (with bottom tabs)
                            Scaffold(
                                bottomBar = {
                                    NavigationBar(
                                        modifier = Modifier
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                            .testTag("app_bottom_bar")
                                    ) {
                                        NavigationBarItem(
                                            selected = selectedTab == AppTab.HOME,
                                            onClick = { selectedTab = AppTab.HOME },
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                            label = { Text("Home") },
                                            modifier = Modifier.testTag("tab_home")
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == AppTab.COURSES,
                                            onClick = { selectedTab = AppTab.COURSES },
                                            icon = { Icon(Icons.Default.School, contentDescription = "Courses") },
                                            label = { Text("Courses") },
                                            modifier = Modifier.testTag("tab_courses")
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == AppTab.SEARCH,
                                            onClick = { selectedTab = AppTab.SEARCH },
                                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                            label = { Text("Search") },
                                            modifier = Modifier.testTag("tab_search")
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == AppTab.PROGRESS,
                                            onClick = { selectedTab = AppTab.PROGRESS },
                                            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Progress") },
                                            label = { Text("Progress") },
                                            modifier = Modifier.testTag("tab_progress")
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == AppTab.SETTINGS,
                                            onClick = { selectedTab = AppTab.SETTINGS },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                            label = { Text("Settings") },
                                            modifier = Modifier.testTag("tab_settings")
                                        )
                                    }
                                },
                                contentWindowInsets = WindowInsets.safeDrawing,
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (selectedTab) {
                                        AppTab.HOME -> HomeScreen(
                                            viewModel = viewModel,
                                            onNavigateToCourses = { selectedTab = AppTab.COURSES },
                                            onNavigateToLesson = { viewModel.selectLesson(it) }
                                        )
                                        AppTab.COURSES -> CoursesScreen(
                                            viewModel = viewModel,
                                            onNavigateToLesson = { viewModel.selectLesson(it) }
                                        )
                                        AppTab.SEARCH -> SearchScreen(
                                            viewModel = viewModel,
                                            onNavigateToLesson = { viewModel.selectLesson(it) }
                                        )
                                        AppTab.PROGRESS -> ProgressScreen(
                                            viewModel = viewModel
                                        )
                                        AppTab.SETTINGS -> SettingsScreen(
                                            viewModel = viewModel,
                                            onResetFolder = {
                                                selectedTab = AppTab.HOME
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissions(permissionsNeeded.toTypedArray(), 100)
        }
    }
}

enum class AppTab {
    HOME, COURSES, SEARCH, PROGRESS, SETTINGS
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Splash Logo",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Offline Academy",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Autonomous Offline Studying",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }
}
