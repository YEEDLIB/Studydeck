package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.LessonDao
import com.example.data.dao.PlaylistDao
import com.example.data.dao.ProgressDao
import com.example.data.model.Category
import com.example.data.model.Lesson
import com.example.data.model.Playlist
import com.example.data.model.UserProgress

@Database(
    entities = [
        Category::class,
        Playlist::class,
        Lesson::class,
        UserProgress::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_learning_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
