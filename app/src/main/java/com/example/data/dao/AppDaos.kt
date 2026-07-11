package com.example.data.dao

import androidx.room.*
import com.example.data.model.Category
import com.example.data.model.Lesson
import com.example.data.model.Playlist
import com.example.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)

    @Query("SELECT * FROM categories WHERE path = :path LIMIT 1")
    suspend fun getCategoryByPath(path: String): Category?
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getPlaylistsByCategory(categoryId: Int): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()

    @Query("DELETE FROM playlists WHERE categoryId = :categoryId")
    suspend fun deletePlaylistsByCategory(categoryId: Int)

    @Query("SELECT * FROM playlists WHERE path = :path LIMIT 1")
    suspend fun getPlaylistByPath(path: String): Playlist?
    
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Int): Playlist?
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE playlistId = :playlistId ORDER BY title ASC")
    fun getLessonsByPlaylist(playlistId: Int): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons ORDER BY dateAdded DESC")
    fun getAllLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun getLessonFlowById(lessonId: Int): Flow<Lesson?>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: Int): Lesson?

    @Query("SELECT * FROM lessons WHERE isFavorite = 1")
    fun getFavoriteLessons(): Flow<List<Lesson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson): Long

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun deleteLessonById(id: Int)

    @Query("SELECT * FROM lessons WHERE videoPath = :videoPath LIMIT 1")
    suspend fun getLessonByVideoPath(videoPath: String): Lesson?

    @Query("SELECT * FROM lessons WHERE title LIKE :query OR description LIKE :query")
    fun searchLessons(query: String): Flow<List<Lesson>>
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM user_progress WHERE lessonId = :lessonId LIMIT 1")
    fun getProgressFlowByLesson(lessonId: Int): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getProgressByLesson(lessonId: Int): UserProgress?

    @Query("SELECT * FROM user_progress ORDER BY lastOpened DESC")
    fun getAllProgress(): Flow<List<UserProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgress)

    @Update
    suspend fun updateProgress(progress: UserProgress)

    @Query("DELETE FROM user_progress")
    suspend fun deleteAllProgress()
}
