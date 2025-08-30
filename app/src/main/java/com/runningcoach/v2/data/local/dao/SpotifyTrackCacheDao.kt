package com.runningcoach.v2.data.local.dao

import androidx.room.*
import com.runningcoach.v2.data.local.entity.SpotifyTrackCacheEntity

@Dao
interface SpotifyTrackCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<SpotifyTrackCacheEntity>)

    @Query("SELECT * FROM spotify_track_cache WHERE uri IN (:uris)")
    suspend fun getTracksByUris(uris: List<String>): List<SpotifyTrackCacheEntity>
}