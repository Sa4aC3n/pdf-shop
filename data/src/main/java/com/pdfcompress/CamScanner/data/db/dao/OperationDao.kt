package com.pdfcompress.CamScanner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdfcompress.CamScanner.data.db.entity.OperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {

    @Query("SELECT * FROM operations ORDER BY completedAt DESC")
    fun getAllOperations(): Flow<List<OperationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OperationEntity): Long

    @Query("DELETE FROM operations")
    suspend fun clearAllOperations()

    @Query("UPDATE operations SET isOutputAvailable = :isAvailable WHERE id = :id")
    suspend fun updateAvailability(id: Long, isAvailable: Boolean)
}
