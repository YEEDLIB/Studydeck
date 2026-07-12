package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Category
import com.example.data.model.Lesson
import com.example.data.model.Playlist
import com.example.data.model.UserProgress
import com.example.data.repository.LearningRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LearningViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("offline_learn_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    
    val repository = LearningRepository(
        context = application,
        categoryDao = database.categoryDao(),
        playlistDao = database.playlistDao(),
        lessonDao = database.lessonDao(),
        progressDao = database.progressDao()
    )

    // Folder & Configuration Preferences
    private val _folderUriString = MutableStateFlow<String?>(sharedPrefs.getString("pref_learning_folder_uri", null))
    val folderUriString: StateFlow<String?> = _folderUriString.asStateFlow()

    private val _isSimulatedMode = MutableStateFlow(sharedPrefs.getBoolean("pref_is_simulated_mode", false))
    val isSimulatedMode: StateFlow<Boolean> = _isSimulatedMode.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("pref_theme_mode", "Dark") ?: "Dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("pref_is_dark_mode", true)) // Default to a gorgeous Dark Mode!
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(sharedPrefs.getString("pref_language", "English") ?: "English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // Library scan status
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Categories, Playlists, and Lessons Flows
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lessons: StateFlow<List<Lesson>> = repository.allLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteLessons: StateFlow<List<Lesson>> = repository.favoriteLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProgress: StateFlow<List<UserProgress>> = repository.allProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen navigation data context
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedLesson = MutableStateFlow<Lesson?>(null)
    val selectedLesson: StateFlow<Lesson?> = _selectedLesson.asStateFlow()

    // Custom playback speed settings
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    init {
        // Automatically check if we should populate simulated folders initially
        if (_folderUriString.value == null && !_isSimulatedMode.value) {
            // Wait for user to select simulated vs real on first setup
        } else if (_isSimulatedMode.value) {
            triggerLibraryScan()
        } else {
            triggerLibraryScan()
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        sharedPrefs.edit().putBoolean("pref_is_dark_mode", enabled).apply()
        setThemeMode(if (enabled) "Dark" else "Light")
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("pref_theme_mode", mode).apply()
        _isDarkMode.value = (mode == "Dark")
        sharedPrefs.edit().putBoolean("pref_is_dark_mode", mode == "Dark").apply()
    }

    fun setLanguage(language: String) {
        _selectedLanguage.value = language
        sharedPrefs.edit().putString("pref_language", language).apply()
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun selectLesson(lesson: Lesson?) {
        _selectedLesson.value = lesson
    }

    fun updatePlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun toggleFavorite(lessonId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(lessonId)
            // Refresh currently selected lesson if needed
            val current = _selectedLesson.value
            if (current != null && current.id == lessonId) {
                _selectedLesson.value = current.copy(isFavorite = !current.isFavorite)
            }
        }
    }

    fun updateLessonProgress(lessonId: Int, position: Long, duration: Long) {
        viewModelScope.launch {
            repository.updateLessonProgress(lessonId, position, duration)
        }
    }

    fun getProgressFlowByLesson(lessonId: Int): Flow<UserProgress?> {
        return repository.getProgressFlowByLesson(lessonId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Get search results reactively
    val searchResults: StateFlow<List<Lesson>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchLessons(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Set the selected Storage Access Framework URI
     */
    fun selectLearningFolder(uri: Uri) {
        viewModelScope.launch {
            _folderUriString.value = uri.toString()
            _isSimulatedMode.value = false
            sharedPrefs.edit().putString("pref_learning_folder_uri", uri.toString()).apply()
            sharedPrefs.edit().putBoolean("pref_is_simulated_mode", false).apply()
            triggerLibraryScan()
        }
    }

    /**
     * Run the application in Simulated Mode with rich pre-loaded classes
     */
    fun setupSimulatedMode() {
        viewModelScope.launch {
            _isSimulatedMode.value = true
            _folderUriString.value = null
            sharedPrefs.edit().putBoolean("pref_is_simulated_mode", true).apply()
            sharedPrefs.edit().remove("pref_learning_folder_uri").apply()
            triggerLibraryScan()
        }
    }

    /**
     * Resets settings and wipes database
     */
    fun clearFolderAndReset() {
        viewModelScope.launch {
            _folderUriString.value = null
            _isSimulatedMode.value = false
            sharedPrefs.edit().remove("pref_learning_folder_uri").apply()
            sharedPrefs.edit().putBoolean("pref_is_simulated_mode", false).apply()
            
            // Wipe Room
            database.categoryDao().deleteAllCategories()
            database.playlistDao().deleteAllPlaylists()
            database.lessonDao().deleteAllLessons()
            database.progressDao().deleteAllProgress()
            
            _selectedCategory.value = null
            _selectedPlaylist.value = null
            _selectedLesson.value = null
        }
    }

    /**
     * Triggers active category scanning in background
     */
    fun triggerLibraryScan() {
        if (_isScanning.value) return
        _isScanning.value = true

        viewModelScope.launch {
            try {
                if (_isSimulatedMode.value) {
                    repository.initializeSimulatedLibrary()
                } else {
                    val uriStr = _folderUriString.value
                    if (uriStr != null) {
                        repository.scanStorageAccessFolder(Uri.parse(uriStr))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }
}
