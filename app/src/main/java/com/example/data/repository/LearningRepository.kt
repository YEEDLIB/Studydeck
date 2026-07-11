package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.data.dao.CategoryDao
import com.example.data.dao.LessonDao
import com.example.data.dao.PlaylistDao
import com.example.data.dao.ProgressDao
import com.example.data.model.Category
import com.example.data.model.Lesson
import com.example.data.model.Playlist
import com.example.data.model.UserProgress
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class LearningRepository(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val playlistDao: PlaylistDao,
    private val lessonDao: LessonDao,
    private val progressDao: ProgressDao
) {
    private val TAG = "LearningRepository"

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val allLessons: Flow<List<Lesson>> = lessonDao.getAllLessons()
    val favoriteLessons: Flow<List<Lesson>> = lessonDao.getFavoriteLessons()
    val allProgress: Flow<List<UserProgress>> = progressDao.getAllProgress()

    fun getPlaylistsByCategory(categoryId: Int): Flow<List<Playlist>> =
        playlistDao.getPlaylistsByCategory(categoryId)

    fun getLessonsByPlaylist(playlistId: Int): Flow<List<Lesson>> =
        lessonDao.getLessonsByPlaylist(playlistId)

    fun getLessonFlowById(lessonId: Int): Flow<Lesson?> =
        lessonDao.getLessonFlowById(lessonId)

    fun getProgressFlowByLesson(lessonId: Int): Flow<UserProgress?> =
        progressDao.getProgressFlowByLesson(lessonId)

    suspend fun getLessonById(lessonId: Int): Lesson? =
        lessonDao.getLessonById(lessonId)

    suspend fun toggleFavorite(lessonId: Int) {
        val lesson = lessonDao.getLessonById(lessonId)
        if (lesson != null) {
            val updated = lesson.copy(isFavorite = !lesson.isFavorite)
            lessonDao.updateLesson(updated)
        }
    }

    suspend fun updateLessonProgress(lessonId: Int, lastPosition: Long, duration: Long) {
        if (duration <= 0) return
        val percentage = (lastPosition.toFloat() / duration.toFloat() * 100f).coerceIn(0f, 100f)
        val completed = percentage >= 90f // Mark completed if watched 90%+
        val progress = UserProgress(
            lessonId = lessonId,
            lastPosition = lastPosition,
            watchPercentage = percentage,
            completedStatus = completed,
            lastOpened = System.currentTimeMillis()
        )
        progressDao.insertProgress(progress)
    }

    fun searchLessons(query: String): Flow<List<Lesson>> {
        val formatQuery = "%$query%"
        return lessonDao.searchLessons(formatQuery)
    }

    /**
     * Initializes a rich demo / simulated library directly in SQLite.
     * This provides a prominent simulation environment so that the platform can be fully
     * explored on first launch, without needing physical video files on the device/emulator.
     */
    suspend fun initializeSimulatedLibrary() = withContext(Dispatchers.IO) {
        // Clear existing scanned items
        categoryDao.deleteAllCategories()
        playlistDao.deleteAllPlaylists()
        lessonDao.deleteAllLessons()

        // Create Categories
        val programmingId = categoryDao.insertCategory(Category(name = "Programming", path = "simulated://Programming")).toInt()
        val mathId = categoryDao.insertCategory(Category(name = "Mathematics", path = "simulated://Mathematics")).toInt()
        val englishId = categoryDao.insertCategory(Category(name = "English Language", path = "simulated://English")).toInt()

        // Create Playlists
        val pythonId = playlistDao.insertPlaylist(Playlist(categoryId = programmingId, name = "Python Basics", path = "simulated://Programming/Python Basics")).toInt()
        val flutterId = playlistDao.insertPlaylist(Playlist(categoryId = programmingId, name = "Flutter Mastering", path = "simulated://Programming/Flutter Mastering")).toInt()

        val algebraId = playlistDao.insertPlaylist(Playlist(categoryId = mathId, name = "Algebra I", path = "simulated://Mathematics/Algebra I")).toInt()
        val geometryId = playlistDao.insertPlaylist(Playlist(categoryId = mathId, name = "Geometry", path = "simulated://Mathematics/Geometry")).toInt()

        val speakingId = playlistDao.insertPlaylist(Playlist(categoryId = englishId, name = "Speaking Skills", path = "simulated://English/Speaking Skills")).toInt()
        val grammarId = playlistDao.insertPlaylist(Playlist(categoryId = englishId, name = "Grammar Mastery", path = "simulated://English/Grammar Mastery")).toInt()

        // Create Lessons with stable test streams
        val pythonVideos = listOf(
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "Lesson 1: Introduction to Python", "https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=500"),
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "Lesson 2: Variables and Core Operators", "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=500"),
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "Lesson 3: Conditionals and Flow Control", "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500")
        )
        pythonVideos.forEachIndexed { index, (url, title, thumb) ->
            lessonDao.insertLesson(
                Lesson(
                    playlistId = pythonId,
                    title = title,
                    videoPath = url,
                    thumbnailPath = thumb,
                    description = "Comprehensive study material for $title. This offline-ready lesson covers standard syntax and practical concepts using real-world compiler logic.",
                    duration = (index + 2) * 60000L,
                    fileSize = (index + 4) * 1024 * 1024L
                )
            )
        }

        val flutterVideos = listOf(
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", "Lesson 1: Hello Flutter Setup", "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=500"),
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", "Lesson 2: Design and Stateful Widgets", "https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=500")
        )
        flutterVideos.forEachIndexed { index, (url, title, thumb) ->
            lessonDao.insertLesson(
                Lesson(
                    playlistId = flutterId,
                    title = title,
                    videoPath = url,
                    thumbnailPath = thumb,
                    description = "Interactive mobile guide for $title. Build production-grade cross-platform application layouts beautifully using Kotlin, Dart and Jetpack Compose design paradigms.",
                    duration = (index + 3) * 60000L,
                    fileSize = (index + 5) * 1024 * 1024L
                )
            )
        }

        val algebraVideos = listOf(
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", "Lesson 1: Solving Multi-Step Linear Equations", "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=500"),
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", "Lesson 2: Quadratic Equations & Roots", "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=500")
        )
        algebraVideos.forEachIndexed { index, (url, title, thumb) ->
            lessonDao.insertLesson(
                Lesson(
                    playlistId = algebraId,
                    title = title,
                    videoPath = url,
                    thumbnailPath = thumb,
                    description = "Unlock the mathematical foundations of algebra in $title. Learn quadratic root isolation methods and equation solving techniques clearly.",
                    duration = (index + 4) * 60000L,
                    fileSize = (index + 6) * 1024 * 1024L
                )
            )
        }

        val speakingVideos = listOf(
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4", "Lesson 1: Mastering Phonetics and Daily Rhythm", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=500"),
            Triple("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "Lesson 2: English Idioms for Professional Dialogue", "https://images.unsplash.com/photo-1543269865-cbf427effbad?w=500")
        )
        speakingVideos.forEachIndexed { index, (url, title, thumb) ->
            lessonDao.insertLesson(
                Lesson(
                    playlistId = speakingId,
                    title = title,
                    videoPath = url,
                    thumbnailPath = thumb,
                    description = "Linguistic study guides for $title. Master professional pronunciations and fluent context pairings offline.",
                    duration = (index + 5) * 60000L,
                    fileSize = (index + 7) * 1024 * 1024L
                )
            )
        }
    }

    /**
     * Performs a scan of the selected Storage Access Framework folder (SAF Tree Uri).
     * It scans categories, playlists, and supported video files, as well as text (.description)
     * and image files (.jpg, .png) representing lesson metadata.
     */
    suspend fun scanStorageAccessFolder(rootTreeUri: Uri) = withContext(Dispatchers.IO) {
        try {
            val rootDocumentId = DocumentsContract.getTreeDocumentId(rootTreeUri) ?: return@withContext
            val contentResolver = context.contentResolver

            // Let's retrieve all children of root directory
            val rootChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootTreeUri, rootDocumentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            // Step 1: Scan Categories (First-level sub-directories)
            val categoriesMap = mutableMapOf<String, Int>() // folderName to categoryId
            contentResolver.query(rootChildrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    val mimeType = cursor.getString(mimeIndex)

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        // This is a Category
                        var dbCategory = categoryDao.getCategoryByPath(docId)
                        if (dbCategory == null) {
                            val catId = categoryDao.insertCategory(Category(name = name, path = docId)).toInt()
                            categoriesMap[docId] = catId
                        } else {
                            categoriesMap[docId] = dbCategory.id
                        }
                    }
                }
            }

            // Step 2: Scan Playlists & Lessons (Second & Third-level sub-directories)
            for ((categoryDocId, categoryId) in categoriesMap) {
                val playlistsChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootTreeUri, categoryDocId)
                val playlistsMap = mutableMapOf<String, Int>() // playlistDocId to playlistId

                contentResolver.query(playlistsChildrenUri, projection, null, null, null)?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idIndex)
                        val name = cursor.getString(nameIndex)
                        val mimeType = cursor.getString(mimeIndex)

                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            var dbPlaylist = playlistDao.getPlaylistByPath(docId)
                            if (dbPlaylist == null) {
                                val playId = playlistDao.insertPlaylist(Playlist(categoryId = categoryId, name = name, path = docId)).toInt()
                                playlistsMap[docId] = playId
                            } else {
                                playlistsMap[docId] = dbPlaylist.id
                            }
                        }
                    }
                }

                // Step 3: Scan Lessons in each Playlist (Third-level files)
                for ((playlistDocId, playlistId) in playlistsMap) {
                    val lessonsChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootTreeUri, playlistDocId)
                    val fileList = mutableListOf<ScannedFile>()

                    contentResolver.query(lessonsChildrenUri, projection, null, null, null)?.use { cursor ->
                        val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                        while (cursor.moveToNext()) {
                            val docId = cursor.getString(idIndex)
                            val name = cursor.getString(nameIndex)
                            val mimeType = cursor.getString(mimeIndex)
                            val size = cursor.getLong(sizeIndex)

                            fileList.add(ScannedFile(docId, name, mimeType, size))
                        }
                    }

                    // Process videos, thumbnails, and description text files
                    val videoFiles = fileList.filter { isSupportedVideo(it.name) }
                    for (video in videoFiles) {
                        val baseName = getBaseName(video.name)

                        // Look for matched description and image thumbnail
                        val descriptionFile = fileList.firstOrNull { getBaseName(it.name) == baseName && it.name.endsWith(".description") }
                        val imageFile = fileList.firstOrNull { getBaseName(it.name) == baseName && (it.mimeType.startsWith("image/") || it.name.endsWith(".jpg") || it.name.endsWith(".png") || it.name.endsWith(".jpeg")) }

                        // Read description if it exists
                        var descriptionContent = "No description available."
                        if (descriptionFile != null) {
                            try {
                                val descUri = DocumentsContract.buildDocumentUriUsingTree(rootTreeUri, descriptionFile.docId)
                                context.contentResolver.openInputStream(descUri)?.use { stream ->
                                    descriptionContent = stream.bufferedReader().use { it.readText() }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error reading description for ${video.name}", e)
                            }
                        }

                        // Set up thumbnail path
                        var thumbnailPath: String? = null
                        if (imageFile != null) {
                            thumbnailPath = DocumentsContract.buildDocumentUriUsingTree(rootTreeUri, imageFile.docId).toString()
                        }

                        val videoUri = DocumentsContract.buildDocumentUriUsingTree(rootTreeUri, video.docId).toString()

                        // Extract actual duration and thumbnail dynamically from video file
                        var durationMs = 180000L // Default fallback (3 mins)
                        val retriever = MediaMetadataRetriever()
                        try {
                            val parsedUri = Uri.parse(videoUri)
                            context.contentResolver.openFileDescriptor(parsedUri, "r")?.use { pfd ->
                                retriever.setDataSource(pfd.fileDescriptor)
                                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                if (durationStr != null) {
                                    durationMs = durationStr.toLong()
                                }

                                if (thumbnailPath == null) {
                                    val cacheFile = File(context.cacheDir, "thumb_${video.docId.hashCode()}.jpg")
                                    if (!cacheFile.exists()) {
                                        val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                        if (bitmap != null) {
                                            FileOutputStream(cacheFile).use { out ->
                                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                            }
                                        }
                                    }
                                    if (cacheFile.exists()) {
                                        thumbnailPath = Uri.fromFile(cacheFile).toString()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting metadata/thumbnail for ${video.name}", e)
                        } finally {
                            try {
                                retriever.release()
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to release MediaMetadataRetriever", e)
                            }
                        }

                        val existingLesson = lessonDao.getLessonByVideoPath(videoUri)

                        if (existingLesson == null) {
                            lessonDao.insertLesson(
                                Lesson(
                                    playlistId = playlistId,
                                    title = baseName,
                                    videoPath = videoUri,
                                    thumbnailPath = thumbnailPath,
                                    description = descriptionContent,
                                    duration = durationMs,
                                    fileSize = video.size
                                )
                            )
                        } else {
                            // Update existing lesson description, duration, or thumbnail
                            val updated = existingLesson.copy(
                                description = descriptionContent,
                                duration = durationMs,
                                thumbnailPath = thumbnailPath ?: existingLesson.thumbnailPath
                            )
                            lessonDao.updateLesson(updated)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during storage scan", e)
        }
    }

    private fun isSupportedVideo(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".avi")
    }

    private fun getBaseName(filename: String): String {
        val index = filename.lastIndexOf('.')
        return if (index == -1) filename else filename.substring(0, index)
    }

    private data class ScannedFile(
        val docId: String,
        val name: String,
        val mimeType: String,
        val size: Long
    )
}
