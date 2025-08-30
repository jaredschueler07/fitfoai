package com.runningcoach.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches audio features for Spotify tracks to minimize API calls.
 *
 * @property uri The unique Spotify URI for the track (e.g., "spotify:track:12345").
 * @property bpm The track's tempo in beats per minute.
 * @property durationMs The duration of the track in milliseconds.
 * @property lastAccessed Timestamp of when this track was last accessed, for cache invalidation.
 */
@Entity(tableName = "spotify_track_cache")
data class SpotifyTrackCacheEntity(
    @PrimaryKey val uri: String,
    val bpm: Int,
    val durationMs: Int,
    val lastAccessed: Long = System.currentTimeMillis()
)