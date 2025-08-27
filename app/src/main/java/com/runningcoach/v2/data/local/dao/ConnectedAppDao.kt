package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.ConnectedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectedAppDao {
    
    @Query("SELECT * FROM connected_apps WHERE userId = :userId")
    fun getConnectedAppsForUser(userId: Long): Flow<List<ConnectedAppEntity>>
    
    @Query("SELECT * FROM connected_apps WHERE userId = :userId AND appType = :appType LIMIT 1")
    suspend fun getConnectedAppByType(userId: Long, appType: String): ConnectedAppEntity?
    
    @Query("SELECT * FROM connected_apps WHERE userId = :userId AND isConnected = 1")
    suspend fun getActiveConnectedApps(userId: Long): List<ConnectedAppEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnectedApp(app: ConnectedAppEntity): Long
    
    @Update
    suspend fun updateConnectedApp(app: ConnectedAppEntity)
    
    @Query("UPDATE connected_apps SET isConnected = :isConnected, lastSyncTime = :lastSyncTime, updatedAt = :updatedAt WHERE userId = :userId AND appType = :appType")
    suspend fun updateConnectionStatus(userId: Long, appType: String, isConnected: Boolean, lastSyncTime: Long?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE connected_apps SET syncStatus = :syncStatus, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE userId = :userId AND appType = :appType")
    suspend fun updateSyncStatus(userId: Long, appType: String, syncStatus: String, errorMessage: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteConnectedApp(app: ConnectedAppEntity)
    
    @Query("DELETE FROM connected_apps WHERE userId = :userId AND appType = :appType")
    suspend fun deleteConnectedAppByType(userId: Long, appType: String)
}
