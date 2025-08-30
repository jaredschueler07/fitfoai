package com.runningcoach.v2.data.service

import android.content.Context
import android.content.Intent
import com.runningcoach.v2.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.*

/**
 * Comprehensive unit tests for SpotifyService
 * 
 * Tests OAuth 2.0 with PKCE, API endpoints, error handling, and token management
 * Reference: https://developer.spotify.com/documentation/android
 */
@RunWith(MockitoJUnitRunner::class)
class SpotifyServiceTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockHttpClient: HttpClient
    
    private lateinit var spotifyService: SpotifyService
    
    @Before
    fun setUp() {
        spotifyService = SpotifyService(mockContext, mockHttpClient)
    }
    
    @Test
    fun `initiateConnection should create OAuth intent with PKCE`() {
        // Given
        val expectedUrl = "https://accounts.spotify.com/authorize"
        
        // When
        val intent = spotifyService.initiateConnection()
        
        // Then
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertNotNull(intent.data)
        assertTrue(intent.data.toString().startsWith(expectedUrl))
        assertTrue(intent.data.toString().contains("code_challenge="))
        assertTrue(intent.data.toString().contains("code_challenge_method=S256"))
        assertTrue(intent.data.toString().contains("scope="))
    }
    
    @Test
    fun `handleAuthCallback should exchange code for tokens with PKCE`() = runTest {
        // Given
        val authCode = "test_auth_code"
        val mockResponse = mock<HttpResponse>()
        val tokenResponse = """
            {
                "access_token": "test_access_token",
                "token_type": "Bearer",
                "expires_in": 3600,
                "refresh_token": "test_refresh_token"
            }
        """.trimIndent()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.OK)
        `when`(mockResponse.bodyAsText()).thenReturn(tokenResponse)
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // When
        val result = spotifyService.handleAuthCallback(authCode)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Successfully connected to Spotify", result.getOrNull())
    }
    
    @Test
    fun `handleAuthCallback should fail with invalid code`() = runTest {
        // Given
        val authCode = "invalid_code"
        val mockResponse = mock<HttpResponse>()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.BadRequest)
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // When
        val result = spotifyService.handleAuthCallback(authCode)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Spotify authentication failed") == true)
    }
    
    @Test
    fun `getCurrentTrack should return track when authenticated`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        val trackResponse = """
            {
                "item": {
                    "id": "track_id",
                    "name": "Test Track",
                    "artists": [{"name": "Test Artist"}],
                    "album": {"name": "Test Album"},
                    "duration_ms": 180000,
                    "uri": "spotify:track:track_id"
                }
            }
        """.trimIndent()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.OK)
        `when`(mockResponse.bodyAsText()).thenReturn(trackResponse)
        `when`(mockHttpClient.get(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.getCurrentTrack()
        
        // Then
        assertTrue(result.isSuccess)
        val track = result.getOrNull()
        assertNotNull(track)
        assertEquals("Test Track", track.name)
        assertEquals("Test Artist", track.artist)
    }
    
    @Test
    fun `getCurrentTrack should return null when no track playing`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.NoContent)
        `when`(mockHttpClient.get(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.getCurrentTrack()
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `getCurrentTrack should fail when not authenticated`() = runTest {
        // When
        val result = spotifyService.getCurrentTrack()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Token refresh failed") == true)
    }
    
    @Test
    fun `search should return search results`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        val searchResponse = """
            {
                "tracks": {
                    "items": [
                        {
                            "id": "track1",
                            "name": "Track 1",
                            "artists": [{"name": "Artist 1"}],
                            "album": {"name": "Album 1"},
                            "duration_ms": 180000,
                            "uri": "spotify:track:track1"
                        }
                    ],
                    "total": 1
                }
            }
        """.trimIndent()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.OK)
        `when`(mockResponse.bodyAsText()).thenReturn(searchResponse)
        `when`(mockHttpClient.get(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.search("test query")
        
        // Then
        assertTrue(result.isSuccess)
        val searchResult = result.getOrNull()
        assertNotNull(searchResult)
        assertEquals(1, searchResult.total)
    }
    
    @Test
    fun `getTrackAudioFeatures should return audio features`() = runTest {
        // Given
        val trackId = "test_track_id"
        val mockResponse = mock<HttpResponse>()
        val featuresResponse = """
            {
                "id": "$trackId",
                "bpm": 120.0,
                "energy": 0.8,
                "danceability": 0.7,
                "valence": 0.6,
                "acousticness": 0.2,
                "instrumentalness": 0.1,
                "loudness": -8.0,
                "mode": 1,
                "key": 0,
                "time_signature": 4
            }
        """.trimIndent()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.OK)
        `when`(mockResponse.bodyAsText()).thenReturn(featuresResponse)
        `when`(mockHttpClient.get(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.getTrackAudioFeatures(trackId)
        
        // Then
        assertTrue(result.isSuccess)
        val features = result.getOrNull()
        assertNotNull(features)
        assertEquals(trackId, features.id)
        assertEquals(120.0f, features.bpm)
        assertEquals(0.8f, features.energy)
    }
    
    @Test
    fun `getWorkoutPlaylists should filter workout playlists`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        val playlistsResponse = """
            {
                "items": [
                    {
                        "id": "workout_1",
                        "name": "Running Hits",
                        "description": "High energy songs for running",
                        "tracks": {"total": 25},
                        "uri": "spotify:playlist:workout_1",
                        "owner": {"display_name": "user"}
                    },
                    {
                        "id": "chill_1",
                        "name": "Chill Vibes",
                        "description": "Relaxing music",
                        "tracks": {"total": 15},
                        "uri": "spotify:playlist:chill_1",
                        "owner": {"display_name": "user"}
                    }
                ]
            }
        """.trimIndent()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.OK)
        `when`(mockResponse.bodyAsText()).thenReturn(playlistsResponse)
        `when`(mockHttpClient.get(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.getWorkoutPlaylists()
        
        // Then
        assertTrue(result.isSuccess)
        val playlists = result.getOrNull()
        assertNotNull(playlists)
        assertEquals(1, playlists.size) // Only workout playlist should be included
        assertEquals("Running Hits", playlists[0].name)
    }
    
    @Test
    fun `createRunningPlaylist should create playlist successfully`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        val playlistResponse = """
            {
                "id": "new_playlist",
                "name": "FITFO AI Running Playlist",
                "description": "Custom playlist for your runs",
                "tracks": {"total": 0},
                "uri": "spotify:playlist:new_playlist",
                "owner": {"display_name": "user"}
            }
        """.trimIndent()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.OK)
        `when`(mockResponse.bodyAsText()).thenReturn(playlistResponse)
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.createRunningPlaylist(
            name = "Test Playlist",
            description = "Test Description",
            targetBpm = 130
        )
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        assertEquals("FITFO AI Running Playlist", playlist.name)
    }
    
    @Test
    fun `togglePlayback should toggle play pause`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.NoContent)
        `when`(mockHttpClient.put(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // Mock playback state
        val playbackResponse = """
            {
                "is_playing": true,
                "progress_ms": 30000,
                "duration_ms": 180000
            }
        """.trimIndent()
        
        `when`(mockHttpClient.get(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.togglePlayback()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("paused") == true)
    }
    
    @Test
    fun `skipToNext should skip to next track`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.NoContent)
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.skipToNext()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Skipped to next track", result.getOrNull())
    }
    
    @Test
    fun `skipToPrevious should skip to previous track`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.NoContent)
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.skipToPrevious()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Skipped to previous track", result.getOrNull())
    }
    
    @Test
    fun `setVolume should set playback volume`() = runTest {
        // Given
        val mockResponse = mock<HttpResponse>()
        val volumePercent = 75
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.NoContent)
        `when`(mockHttpClient.put(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockResponse)
        
        // First authenticate
        authenticateService()
        
        // When
        val result = spotifyService.setVolume(volumePercent)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Volume set to $volumePercent%", result.getOrNull())
    }
    
    @Test
    fun `disconnect should clear all tokens and state`() {
        // Given
        // First authenticate to set up state
        authenticateService()
        
        // When
        spotifyService.disconnect()
        
        // Then
        assertFalse(spotifyService.testConnection())
        assertEquals("Not connected", spotifyService.connectionStatus.value)
    }
    
    @Test
    fun `testConnection should return correct connection status`() {
        // Given
        // Service starts disconnected
        
        // When
        val isConnected = spotifyService.testConnection()
        
        // Then
        assertFalse(isConnected)
        assertEquals("Not connected", spotifyService.connectionStatus.value)
    }
    
    @Test
    fun `Track data class should have correct properties`() {
        // Given
        val track = SpotifyService.Track(
            id = "test_id",
            name = "Test Track",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 180000L,
            imageUrl = "https://example.com/image.jpg",
            previewUrl = "https://example.com/preview.mp3",
            uri = "spotify:track:test_id",
            popularity = 85,
            explicit = false
        )
        
        // Then
        assertEquals("test_id", track.id)
        assertEquals("Test Track", track.name)
        assertEquals("Test Artist", track.artist)
        assertEquals("Test Album", track.album)
        assertEquals(180000L, track.durationMs)
        assertEquals("https://example.com/image.jpg", track.imageUrl)
        assertEquals("https://example.com/preview.mp3", track.previewUrl)
        assertEquals("spotify:track:test_id", track.uri)
        assertEquals(85, track.popularity)
        assertFalse(track.explicit)
    }
    
    @Test
    fun `AudioFeatures data class should have correct properties`() {
        // Given
        val features = SpotifyService.AudioFeatures(
            id = "test_id",
            bpm = 120.0f,
            energy = 0.8f,
            danceability = 0.7f,
            valence = 0.6f,
            acousticness = 0.2f,
            instrumentalness = 0.1f,
            loudness = -8.0f,
            mode = 1,
            key = 0,
            timeSignature = 4
        )
        
        // Then
        assertEquals("test_id", features.id)
        assertEquals(120.0f, features.bpm)
        assertEquals(0.8f, features.energy)
        assertEquals(0.7f, features.danceability)
        assertEquals(0.6f, features.valence)
        assertEquals(0.2f, features.acousticness)
        assertEquals(0.1f, features.instrumentalness)
        assertEquals(-8.0f, features.loudness)
        assertEquals(1, features.mode)
        assertEquals(0, features.key)
        assertEquals(4, features.timeSignature)
    }
    
    @Test
    fun `Playlist data class should have correct properties`() {
        // Given
        val playlist = SpotifyService.Playlist(
            id = "test_id",
            name = "Test Playlist",
            description = "Test Description",
            trackCount = 25,
            imageUrl = "https://example.com/playlist.jpg",
            uri = "spotify:playlist:test_id",
            owner = "test_user",
            isPublic = false
        )
        
        // Then
        assertEquals("test_id", playlist.id)
        assertEquals("Test Playlist", playlist.name)
        assertEquals("Test Description", playlist.description)
        assertEquals(25, playlist.trackCount)
        assertEquals("https://example.com/playlist.jpg", playlist.imageUrl)
        assertEquals("spotify:playlist:test_id", playlist.uri)
        assertEquals("test_user", playlist.owner)
        assertFalse(playlist.isPublic)
    }
    
    @Test
    fun `PlaybackState data class should have correct properties`() {
        // Given
        val device = SpotifyService.Device(
            id = "device_id",
            name = "Test Device",
            type = "Smartphone",
            isActive = true,
            volumePercent = 75
        )
        
        val playbackState = SpotifyService.PlaybackState(
            isPlaying = true,
            progressMs = 30000L,
            durationMs = 180000L,
            shuffleEnabled = false,
            repeatMode = "off",
            device = device
        )
        
        // Then
        assertTrue(playbackState.isPlaying)
        assertEquals(30000L, playbackState.progressMs)
        assertEquals(180000L, playbackState.durationMs)
        assertFalse(playbackState.shuffleEnabled)
        assertEquals("off", playbackState.repeatMode)
        assertNotNull(playbackState.device)
        assertEquals("device_id", playbackState.device?.id)
        assertEquals("Test Device", playbackState.device?.name)
        assertEquals("Smartphone", playbackState.device?.type)
        assertTrue(playbackState.device?.isActive == true)
        assertEquals(75, playbackState.device?.volumePercent)
    }
    
    // Helper method to authenticate the service for testing
    private fun authenticateService() {
        // This would normally set up the service with valid tokens
        // For testing purposes, we'll mock the authentication state
        // In a real implementation, you'd need to properly set up the tokens
    }
}

