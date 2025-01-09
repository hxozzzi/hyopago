package com.hxozzi.hyopago

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveDataDao {
    @Query("SELECT * FROM savedata")
    fun getAll(): Flow<List<SaveData>>

    @Insert
    fun insertAll(vararg saveDatas: SaveData)

    @Delete
    fun delete(saveData: SaveData)
}