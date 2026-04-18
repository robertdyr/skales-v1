package com.example.skales.storage.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ScaleEntity::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SkalesDatabase : RoomDatabase() {
    abstract fun scaleDao(): ScaleDao

    companion object {
        fun create(context: Context): SkalesDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SkalesDatabase::class.java,
                "skales.db",
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
        }
    }
}
