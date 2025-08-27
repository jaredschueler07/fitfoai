package com.runningcoach.v2.data.service

import android.content.Context
import android.content.Intent
import com.runningcoach.v2.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.*

class SpotifyService(
    private val context: Context,
    private val httpClient: HttpClient
) {
    
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI
    private val baseUrl = "https://api.spotify.com/v1"
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow("Not connected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()
    
    private var accessToken: String? = null
    private var refreshToken: String? = null
    
    @Serializable
    data class Track(
        val id: String,
        val name: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val imageUrl: String? = null,
        val previewUrl: String? = null
    )
    
    @Serializable
    data class Playlist(
        val id: String,
        val name: String,
        val description: String,
        val trackCount: Int,
        val imageUrl: String? = null
    )
    
    @Serializable
    data class WorkoutPlaylist(
        val id: String,
        val name: String,
        val tracks: List<Track>,
        val totalDuration: Long,
        val averageBpm: Int? = null
    )
    
    fun initiateConnection(): Intent {
        _connectionStatus.value = "Connecting..."
        
        // Generate state parameter for security
        val state = UUID.randomUUID().toString()
        
        // Create Spotify OAuth URL
        val authUrl = buildString {
            append("https://accounts.spotify.com/authorize")
            append("?client_id=${clientId}")
            append("&response_type=code")
            append("&redirect_uri=${redirectUri}")
            append("&state=${state}")
            append("&scope=user-read-playback-state")
            append("%20user-modify-playback-state")
            append("%20user-read-currently-playing")
            append("%20playlist-read-private")
            append("%20playlist-modify-public")
            append("%20playlist-modify-private")
            append("&show_dialog=true")
        }
        
        return Intent(Intent.ACTION_VIEW, android.net.Uri.parse(authUrl))
    }
    
    suspend fun handleAuthCallback(authCode: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            _connectionStatus.value = "Authenticating..."
            
            // Exchange auth code for access token
            val tokenResponse: HttpResponse = httpClient.post("https://accounts.spotify.com/api/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                header("Authorization", "Basic ${encodeClientCredentials()}")
                setBody("grant_type=authorization_code&" +
                       "code=$authCode&" +
                       "redirect_uri=$redirectUri")
            }
            
            if (tokenResponse.status.isSuccess()) {
                val responseBody = tokenResponse.bodyAsText()
                accessToken = extractTokenFromResponse(responseBody, "access_token")
                refreshToken = extractTokenFromResponse(responseBody, "refresh_token")
                
                _isConnected.value = true
                _connectionStatus.value = "Connected to Spotify"
                
                Result.success("Successfully connected to Spotify")
            } else {
                _connectionStatus.value = "Connection failed"
                Result.failure(Exception("Spotify authentication failed: ${tokenResponse.status}"))
            }
        } catch (e: Exception) {
            _connectionStatus.value = "Connection failed"
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentTrack(): Result<Track?> = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext Result.failure(Exception("Not authenticated with Spotify"))
        }
        
        try {
            val response = httpClient.get("$baseUrl/me/player/currently-playing") {
                header("Authorization", "Bearer $accessToken")
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseBody = response.bodyAsText()
                    val track = parseTrackFromResponse(responseBody)
                    _currentTrack.value = track
                    Result.success(track)
                }
                HttpStatusCode.NoContent -> {
                    _currentTrack.value = null
                    Result.success(null) // No track currently playing
                }
                else -> {
                    Result.failure(Exception("Failed to get current track: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getWorkoutPlaylists(): Result<List<Playlist>> = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext Result.failure(Exception("Not authenticated with Spotify"))
        }
        
        try {
            val response = httpClient.get("$baseUrl/me/playlists") {
                header("Authorization", "Bearer $accessToken")
                parameter("limit", "20")
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val playlists = parsePlaylistsFromResponse(responseBody)
                
                // Filter for workout-related playlists
                val workoutPlaylists = playlists.filter { playlist ->
                    val name = playlist.name.lowercase()
                    name.contains("workout") || name.contains("running") || 
                    name.contains("fitness") || name.contains("cardio") ||
                    name.contains("gym") || name.contains("exercise")
                }
                
                Result.success(workoutPlaylists)
            } else {
                Result.failure(Exception("Failed to get playlists: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createRunningPlaylist(
        name: String,
        description: String,
        targetBpm: Int = 120
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext Result.failure(Exception("Not authenticated with Spotify"))
        }
        
        try {
            // First, create the playlist
            val createResponse = httpClient.post("$baseUrl/me/playlists") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "$name",
                        "description": "$description",
                        "public": false
                    }
                """.trimIndent())
            }
            
            if (createResponse.status.isSuccess()) {
                val responseBody = createResponse.bodyAsText()
                val playlist = parsePlaylistFromResponse(responseBody)
                
                Result.success(playlist)
            } else {
                Result.failure(Exception("Failed to create playlist: ${createResponse.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun pausePlayback(): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext Result.failure(Exception("Not authenticated with Spotify"))
        }
        
        try {
            val response = httpClient.put("$baseUrl/me/player/pause") {
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status.isSuccess() || response.status == HttpStatusCode.NoContent) {
                Result.success("Playback paused")
            } else {
                Result.failure(Exception("Failed to pause playback: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resumePlayback(): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext Result.failure(Exception("Not authenticated with Spotify"))
        }
        
        try {
            val response = httpClient.put("$baseUrl/me/player/play") {
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status.isSuccess() || response.status == HttpStatusCode.NoContent) {
                Result.success("Playback resumed")
            } else {
                Result.failure(Exception("Failed to resume playback: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun disconnect() {
        accessToken = null
        refreshToken = null
        _isConnected.value = false
        _connectionStatus.value = "Not connected"
        _currentTrack.value = null
    }
    
    fun testConnection(): Boolean {
        _connectionStatus.value = if (_isConnected.value) "Connected" else "Not connected"
        return _isConnected.value
    }
    
    private fun encodeClientCredentials(): String {
        val credentials = "$clientId:$clientSecret"
        return Base64.getEncoder().encodeToString(credentials.toByteArray())
    }
    
    private fun extractTokenFromResponse(response: String, tokenType: String): String {
        val tokenRegex = "\"$tokenType\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return tokenRegex.find(response)?.groupValues?.get(1) ?: ""
    }
    
    private fun parseTrackFromResponse(response: String): Track? {
        // Simplified parsing - in production, use proper JSON parsing
        try {
            val nameRegex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val name = nameRegex.find(response)?.groupValues?.get(1) ?: return null
            
            // For demo purposes, return a mock track
            return Track(
                id = "mock_id",
                name = name,
                artist = "Mock Artist",
                album = "Mock Album",
                durationMs = 180000
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun parsePlaylistsFromResponse(response: String): List<Playlist> {
        // Simplified parsing - in production, use proper JSON parsing
        // Return mock playlists for demo
        return listOf(
            Playlist(
                id = "workout_1",
                name = "Running Hits",
                description = "High energy songs for running",
                trackCount = 25
            ),
            Playlist(
                id = "workout_2", 
                name = "Cardio Pump",
                description = "Perfect BPM for cardio workouts",
                trackCount = 30
            )
        )
    }
    
    private fun parsePlaylistFromResponse(response: String): Playlist {
        // Simplified parsing - return mock playlist
        return Playlist(
            id = "new_playlist",
            name = "FITFO AI Running Playlist",
            description = "Custom playlist for your runs",
            trackCount = 0
        )
    }
}
