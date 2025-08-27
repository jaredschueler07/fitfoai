package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "connected_apps",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "appType"]),
        Index(value = ["isConnected"])
    ]
)
data class ConnectedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val appType: String, // GOOGLE_FIT, SPOTIFY, FITBIT
    val appName: String,
    val isConnected: Boolean = false,
    val connectionToken: String? = null, // encrypted token if needed
    val lastSyncTime: Long? = null,
    val syncStatus: String = "NOT_SYNCED",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
