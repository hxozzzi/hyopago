package com.hxozzi.hyopago

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SaveData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun saveDataDao(): SaveDataDao

    companion object MyDb {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this){
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,"room.db"
                )
                    .build()
                    .also { instance = it }
            }
        }

    }
}