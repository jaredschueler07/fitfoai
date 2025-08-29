package com.runningcoach.v2.data.service

import android.content.Context
import com.runningcoach.v2.BuildConfig
import com.runningcoach.v2.data.local.dao.SpotifyTrackCacheDao
import com.runningcoach.v2.data.local.entity.SpotifyTrackCacheEntity
import com.runningcoach.v2.data.repository.SpotifyAuthRepository
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

class SpotifyService(
    private val context: Context,
    private val authRepository: SpotifyAuthRepository,
    private val trackCacheDao: SpotifyTrackCacheDao,
    private val httpClient: HttpClient
) {

    // ... (existing code)

    suspend fun getAudioFeatures(trackUris: List<String>): List<SpotifyTrackCacheEntity> {
        val cachedTracks = trackCacheDao.getTracksByUris(trackUris)
        val urisToFetch = trackUris - cachedTracks.map { it.uri }.toSet()

        if (urisToFetch.isEmpty()) {
            return cachedTracks
        }

        val fetchedTracks = mutableListOf<SpotifyTrackCacheEntity>()
        val accessToken = authRepository.getAccessToken()

        urisToFetch.chunked(100).forEach { chunk ->
            val chunkTrackIds = chunk.joinToString(",") { it.substringAfter("spotify:track:") }
            
            val featuresResponse: AudioFeaturesResponse = httpClient.get("https://api.spotify.com/v1/audio-features") {
                header("Authorization", "Bearer $accessToken")
                parameter("ids", chunkTrackIds)
            }.body()

            val tracksResponse: TracksResponse = httpClient.get("https://api.spotify.com/v1/tracks") {
                header("Authorization", "Bearer $accessToken")
                parameter("ids", chunkTrackIds)
            }.body()

            val trackDurations = tracksResponse.tracks.associateBy { it.id }

            val tracks = featuresResponse.audio_features.mapNotNull { feature ->
                trackDurations[feature.id]?.let { track ->
                    SpotifyTrackCacheEntity(
                        uri = "spotify:track:${feature.id}",
                        bpm = feature.tempo.toInt(),
                        durationMs = track.duration_ms
                    )
                }
            }
            fetchedTracks.addAll(tracks)
        }

        if (fetchedTracks.isNotEmpty()) {
            trackCacheDao.insertTracks(fetchedTracks)
        }

        return cachedTracks + fetchedTracks
    }

    // ... (rest of the existing SpotifyService code)
}

@Serializable
data class LibraryTracksResponse(
    val href: String,
    val items: List<LibraryTrackItem>,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int
)

@Serializable
data class LibraryTrackItem(
    val added_at: String,
    val track: SimplifiedTrack
)

@Serializable
data class SimplifiedTrack(
    val id: String,
    val name: String,
    val uri: String
)

@Serializable
data class TracksResponse(val tracks: List<TrackResponse>)

@Serializable
data class TrackResponse(val id: String, val duration_ms: Int)

@Serializable
data class AudioFeaturesResponse(val audio_features: List<AudioFeature>)

@Serializable
data class AudioFeature(val id: String, val tempo: Float)

// ... (rest of the data classes)