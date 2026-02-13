package com.hermanns.hermannsgerenciador.salesman.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("UPDATE locations SET isExported = 1")
    suspend fun markAllAsExported()

    @Insert
    suspend fun insert(location: LocationEntity)

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isExported = 0 ORDER BY timestamp DESC")
    fun getUnexportedLocations(): Flow<List<LocationEntity>>

    @Query("UPDATE locations SET isExported = 1 WHERE id = :id")
    suspend fun markAsExported(id: Long)

    @Query("SELECT * FROM locations WHERE local != 'Auto-coletado' ORDER BY timestamp DESC")
    fun getManualLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE local = 'Auto-coletado' ORDER BY timestamp DESC")
    fun getAutoLocations(): Flow<List<LocationEntity>>

    @Query("DELETE FROM locations WHERE isExported = 0")
    suspend fun deleteUnexportedLocations()
}
