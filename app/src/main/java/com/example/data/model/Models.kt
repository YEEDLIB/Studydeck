package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val name: String,
    val path: String
)

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val title: String,
    val videoPath: String,
    val thumbnailPath: String? = null,
    val description: String? = null,
    val duration: Long = 0, // in milliseconds
    val fileSize: Long = 0, // in bytes
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val lessonId: Int,
    val lastPosition: Long = 0, // in milliseconds
    val watchPercentage: Float = 0f, // 0.0f to 100.0f
    val completedStatus: Boolean = false,
    val lastOpened: Long = System.currentTimeMillis()
)
