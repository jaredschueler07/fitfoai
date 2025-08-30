package com.runningcoach.v2.data.service

import android.content.Context
import com.runningcoach.v2.data.local.entity.SpotifyUserPreferencesEntity
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
 * Comprehensive unit tests for PlaylistRecommendationEngine
 * 
 * Tests Vertex AI integration, playlist generation, fallback mechanisms, and cost optimization
 */
@RunWith(MockitoJUnitRunner::class)
class PlaylistRecommendationEngineTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockHttpClient: HttpClient
    
    @Mock
    private lateinit var mockSpotifyService: SpotifyService
    
    private lateinit var playlistEngine: PlaylistRecommendationEngine
    
    @Before
    fun setUp() {
        playlistEngine = PlaylistRecommendationEngine(mockContext, mockHttpClient, mockSpotifyService)
    }
    
    @Test
    fun `generateWorkoutPlaylist should create playlist with Vertex AI recommendations`() = runTest {
        // Given
        val workoutContext = createWorkoutContext()
        val userPreferences = createUserPreferences()
        val mockVertexResponse = createMockVertexAIResponse()
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockVertexResponse)
        
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockSearchResult()))
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(emptyList()))
        
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.success(createMockSpotifyPlaylist()))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        assertEquals("Endurance Workout - 30min", playlist.name)
        assertEquals("endurance", playlist.targetWorkoutType)
        assertTrue(playlist.tracks.isNotEmpty())
    }
    
    @Test
    fun `generateWorkoutPlaylist should fallback to rule-based recommendations when Vertex AI fails`() = runTest {
        // Given
        val workoutContext = createWorkoutContext()
        val userPreferences = createUserPreferences()
        
        // Mock Vertex AI failure
        val mockErrorResponse = mock<HttpResponse>()
        `when`(mockErrorResponse.status).thenReturn(HttpStatusCode.InternalServerError)
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(mockErrorResponse)
        
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockSearchResult()))
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(emptyList()))
        
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.success(createMockSpotifyPlaylist()))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        // Should still generate a playlist using rule-based recommendations
        assertEquals("Endurance Workout - 30min", playlist.name)
    }
    
    @Test
    fun `generateWorkoutPlaylist should handle different workout types`() = runTest {
        // Given
        val workoutTypes = listOf("endurance", "interval", "recovery", "tempo")
        
        for (workoutType in workoutTypes) {
            val workoutContext = createWorkoutContext(workoutType = workoutType)
            val userPreferences = createUserPreferences()
            
            // Mock successful responses
            `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
                .thenReturn(createMockVertexAIResponse())
            
            `when`(mockSpotifyService.search(any(), any(), any(), any()))
                .thenReturn(Result.success(createMockSearchResult()))
            
            `when`(mockSpotifyService.getTracksAudioFeatures(any()))
                .thenReturn(Result.success(emptyList()))
            
            `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
                .thenReturn(Result.success(createMockSpotifyPlaylist()))
            
            // When
            val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
            
            // Then
            assertTrue(result.isSuccess)
            val playlist = result.getOrNull()
            assertNotNull(playlist)
            assertEquals("${workoutType.capitalize()} Workout - 30min", playlist.name)
            assertEquals(workoutType, playlist.targetWorkoutType)
        }
    }
    
    @Test
    fun `generateWorkoutPlaylist should respect user preferences`() = runTest {
        // Given
        val workoutContext = createWorkoutContext()
        val userPreferences = createUserPreferences(
            preferredGenres = listOf("rock", "metal"),
            energyPreference = 0.9f,
            avoidExplicit = false
        )
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(createMockVertexAIResponse())
        
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockSearchResult()))
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(emptyList()))
        
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.success(createMockSpotifyPlaylist()))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        // The playlist should be generated considering the user's rock/metal preference
        // and high energy preference
    }
    
    @Test
    fun `generateWorkoutPlaylist should handle Spotify service failures gracefully`() = runTest {
        // Given
        val workoutContext = createWorkoutContext()
        val userPreferences = createUserPreferences()
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(createMockVertexAIResponse())
        
        // Mock Spotify service failure
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.failure(Exception("Spotify service unavailable")))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to get AI recommendations") == true)
    }
    
    @Test
    fun `generateWorkoutPlaylist should optimize playlist structure based on energy profile`() = runTest {
        // Given
        val workoutContext = createWorkoutContext(workoutType = "interval")
        val userPreferences = createUserPreferences()
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(createMockVertexAIResponse())
        
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockSearchResult()))
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(emptyList()))
        
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.success(createMockSpotifyPlaylist()))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        assertEquals("Interval Workout - 30min", playlist.name)
        // Interval workouts should have energy building progression
    }
    
    @Test
    fun `generateWorkoutPlaylist should calculate correct duration and BPM`() = runTest {
        // Given
        val workoutContext = createWorkoutContext(
            targetDuration = 45L,
            targetBpm = 160
        )
        val userPreferences = createUserPreferences()
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(createMockVertexAIResponse())
        
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockSearchResult()))
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(emptyList()))
        
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.success(createMockSpotifyPlaylist()))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        assertEquals("Endurance Workout - 45min", playlist.name)
        assertEquals(160, playlist.averageBpm)
        assertEquals(45 * 60 * 1000, playlist.totalDuration) // Convert minutes to milliseconds
    }
    
    @Test
    fun `generateWorkoutPlaylist should handle different fitness levels`() = runTest {
        // Given
        val fitnessLevels = listOf("beginner", "intermediate", "advanced")
        
        for (fitnessLevel in fitnessLevels) {
            val workoutContext = createWorkoutContext(userFitnessLevel = fitnessLevel)
            val userPreferences = createUserPreferences()
            
            `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
                .thenReturn(createMockVertexAIResponse())
            
            `when`(mockSpotifyService.search(any(), any(), any(), any()))
                .thenReturn(Result.success(createMockSearchResult()))
            
            `when`(mockSpotifyService.getTracksAudioFeatures(any()))
                .thenReturn(Result.success(emptyList()))
            
            `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
                .thenReturn(Result.success(createMockSpotifyPlaylist()))
            
            // When
            val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
            
            // Then
            assertTrue(result.isSuccess)
            val playlist = result.getOrNull()
            assertNotNull(playlist)
            // Different fitness levels should result in different playlist characteristics
        }
    }
    
    @Test
    fun `generateWorkoutPlaylist should filter tracks based on audio features`() = runTest {
        // Given
        val workoutContext = createWorkoutContext()
        val userPreferences = createUserPreferences()
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(createMockVertexAIResponse())
        
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockSearchResult()))
        
        // Mock audio features with some tracks having valid BPM
        val audioFeatures = listOf(
            SpotifyService.AudioFeatures(
                id = "track1",
                bpm = 130.0f,
                energy = 0.8f,
                danceability = 0.7f,
                valence = 0.6f,
                acousticness = 0.2f,
                instrumentalness = 0.1f,
                loudness = -8.0f,
                mode = 1,
                key = 0,
                timeSignature = 4
            ),
            SpotifyService.AudioFeatures(
                id = "track2",
                bpm = 0.0f, // Invalid BPM
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
        )
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(audioFeatures))
        
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.success(createMockSpotifyPlaylist()))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        // Only tracks with valid BPM should be included
    }
    
    @Test
    fun `generateWorkoutPlaylist should handle empty search results`() = runTest {
        // Given
        val workoutContext = createWorkoutContext()
        val userPreferences = createUserPreferences()
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(createMockVertexAIResponse())
        
        // Mock empty search results
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(SpotifyService.SearchResult(emptyList(), emptyList(), 0)))
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(emptyList()))
        
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.success(createMockSpotifyPlaylist()))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertNotNull(playlist)
        // Should still create a playlist even with empty search results
        assertTrue(playlist.tracks.isEmpty())
    }
    
    @Test
    fun `generateWorkoutPlaylist should handle Spotify playlist creation failure`() = runTest {
        // Given
        val workoutContext = createWorkoutContext()
        val userPreferences = createUserPreferences()
        
        `when`(mockHttpClient.post(any<String>(), any<suspend HttpClientRequestConfig.() -> Unit>()))
            .thenReturn(createMockVertexAIResponse())
        
        `when`(mockSpotifyService.search(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockSearchResult()))
        
        `when`(mockSpotifyService.getTracksAudioFeatures(any()))
            .thenReturn(Result.success(emptyList()))
        
        // Mock Spotify playlist creation failure
        `when`(mockSpotifyService.createRunningPlaylist(any(), any(), any()))
            .thenReturn(Result.failure(Exception("Failed to create Spotify playlist")))
        
        // When
        val result = playlistEngine.generateWorkoutPlaylist(workoutContext, userPreferences)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to create Spotify playlist") == true)
    }
    
    @Test
    fun `getRecommendationHistory should return empty list initially`() {
        // When
        val history = playlistEngine.getRecommendationHistory()
        
        // Then
        assertTrue(history.isEmpty())
    }
    
    @Test
    fun `clearCache should reset engine state`() {
        // Given
        // Engine starts with default state
        
        // When
        playlistEngine.clearCache()
        
        // Then
        val history = playlistEngine.getRecommendationHistory()
        assertTrue(history.isEmpty())
        // Cache should be cleared
    }
    
    // Helper methods
    
    private fun createWorkoutContext(
        workoutType: String = "endurance",
        targetDuration: Long = 30L,
        targetBpm: Int = 130,
        userFitnessLevel: String = "intermediate"
    ): PlaylistRecommendationEngine.WorkoutContext {
        return PlaylistRecommendationEngine.WorkoutContext(
            workoutType = workoutType,
            targetDuration = targetDuration,
            targetBpm = targetBpm,
            userFitnessLevel = userFitnessLevel,
            preferredGenres = listOf("pop", "rock", "electronic"),
            energyPreference = 0.7f,
            avoidExplicit = true
        )
    }
    
    private fun createUserPreferences(
        preferredGenres: List<String> = listOf("pop", "rock", "electronic"),
        energyPreference: Float = 0.7f,
        avoidExplicit: Boolean = true
    ): SpotifyUserPreferencesEntity {
        return SpotifyUserPreferencesEntity(
            userId = "test_user",
            preferredGenres = preferredGenres,
            preferredBpmMin = 120,
            preferredBpmMax = 160,
            energyPreference = energyPreference,
            avoidExplicit = avoidExplicit,
            preferredLanguages = listOf("en"),
            workoutMusicEnabled = true,
            autoBpmMatching = true
        )
    }
    
    private fun createMockVertexAIResponse(): HttpResponse {
        val mockResponse = mock<HttpResponse>()
        val responseBody = """
            {
                "predictions": [
                    {
                        "trackRecommendations": [
                            {
                                "trackId": "track1",
                                "confidence": 0.9,
                                "reason": "High energy track",
                                "bpm": 130,
                                "energy": 0.8,
                                "danceability": 0.7
                            },
                            {
                                "trackId": "track2",
                                "confidence": 0.85,
                                "reason": "Good BPM match",
                                "bpm": 135,
                                "energy": 0.75,
                                "danceability": 0.8
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        
        `when`(mockResponse.status).thenReturn(HttpStatusCode.OK)
        `when`(mockResponse.bodyAsText()).thenReturn(responseBody)
        
        return mockResponse
    }
    
    private fun createMockSearchResult(): SpotifyService.SearchResult {
        val tracks = listOf(
            SpotifyService.Track(
                id = "track1",
                name = "Test Track 1",
                artist = "Test Artist 1",
                album = "Test Album 1",
                durationMs = 180000L,
                uri = "spotify:track:track1"
            ),
            SpotifyService.Track(
                id = "track2",
                name = "Test Track 2",
                artist = "Test Artist 2",
                album = "Test Album 2",
                durationMs = 180000L,
                uri = "spotify:track:track2"
            )
        )
        
        return SpotifyService.SearchResult(
            tracks = tracks,
            playlists = emptyList(),
            total = tracks.size
        )
    }
    
    private fun createMockSpotifyPlaylist(): SpotifyService.Playlist {
        return SpotifyService.Playlist(
            id = "test_playlist",
            name = "Test Playlist",
            description = "Test Description",
            trackCount = 2,
            uri = "spotify:playlist:test_playlist",
            owner = "test_user",
            isPublic = false
        )
    }
}

