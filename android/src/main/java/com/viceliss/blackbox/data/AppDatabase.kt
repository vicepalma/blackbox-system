package com.viceliss.blackbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Journey::class], version = 1, exportSchema = false) // ✅ Asegurar que la entidad esté definida
abstract class AppDatabase : RoomDatabase() {
    abstract fun journeyDao(): JourneyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blackbox_database"
                )
                    .fallbackToDestructiveMigration() // ✅ Asegurar que la BD se actualice si cambia la estructura
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
