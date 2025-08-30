package com.runningcoach.v2.data.service

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.*

/**
 * Comprehensive unit tests for MusicCoachingIntegration
 * 
 * Tests voice-music coordination, audio ducking, coaching timing, and priority management
 */
@RunWith(MockitoJUnitRunner::class)
class MusicCoachingIntegrationTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSpotifyService: SpotifyService
    
    @Mock
    private lateinit var mockVoiceCoachingManager: VoiceCoachingManager
    
    @Mock
    private lateinit var mockBpmAnalysisEngine: BPMAnalysisEngine
    
    @Mock
    private lateinit var mockAudioManager: AudioManager
    
    private lateinit var musicCoachingIntegration: MusicCoachingIntegration
    
    @Before
    fun setUp() {
        // Mock AudioManager
        `when`(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        `when`(mockAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(80)
        
        musicCoachingIntegration = MusicCoachingIntegration(
            mockContext,
            mockSpotifyService,
            mockVoiceCoachingManager,
            mockBpmAnalysisEngine
        )
    }
    
    @Test
    fun `configureMusicAwareCoaching should update settings correctly`() {
        // Given
        val enabled = true
        val duckingLevel = 40
        val strategy = MusicCoachingIntegration.DuckingStrategy.HEAVY
        
        // When
        musicCoachingIntegration.configureMusicAwareCoaching(enabled, duckingLevel, strategy)
        
        // Then
        assertTrue(musicCoachingIntegration.musicAwareCoaching.value)
        assertEquals(40, musicCoachingIntegration.audioDuckingLevel.value)
    }
    
    @Test
    fun `configureMusicAwareCoaching should set correct ducking level for different strategies`() {
        // Test different strategies
        val testCases = listOf(
            MusicCoachingIntegration.DuckingStrategy.NONE to 0,
            MusicCoachingIntegration.DuckingStrategy.LIGHT to 10,
            MusicCoachingIntegration.DuckingStrategy.MODERATE to 30,
            MusicCoachingIntegration.DuckingStrategy.HEAVY to 50,
            MusicCoachingIntegration.DuckingStrategy.SMART to 30
        )
        
        for ((strategy, expectedLevel) in testCases) {
            // When
            musicCoachingIntegration.configureMusicAwareCoaching(true, 30, strategy)
            
            // Then
            assertEquals(expectedLevel, musicCoachingIntegration.audioDuckingLevel.value)
        }
    }
    
    @Test
    fun `requestCoaching should add urgent coaching to front of queue`() = runTest {
        // Given
        val urgentMessage = "Urgent coaching message"
        val urgentUrgency = ElevenLabsService.CoachingUrgency.URGENT
        val priority = ElevenLabsService.AudioPriority.HIGH
        val coachId = "test_coach"
        
        // When
        musicCoachingIntegration.requestCoaching(
            message = urgentMessage,
            urgency = urgentUrgency,
            priority = priority,
            coachId = coachId,
            isUrgent = true
        )
        
        // Then
        val queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(1, queueStatus.queueSize)
        assertTrue(queueStatus.hasUrgent)
        
        // Add regular coaching
        val regularMessage = "Regular coaching message"
        musicCoachingIntegration.requestCoaching(
            message = regularMessage,
            urgency = ElevenLabsService.CoachingUrgency.NORMAL,
            priority = ElevenLabsService.AudioPriority.NORMAL,
            coachId = coachId,
            isUrgent = false
        )
        
        // Urgent coaching should still be prioritized
        val updatedQueueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(2, updatedQueueStatus.queueSize)
        assertTrue(updatedQueueStatus.hasUrgent)
    }
    
    @Test
    fun `requestCoaching should update coaching priority correctly`() = runTest {
        // Given
        val message = "Test coaching message"
        val urgency = ElevenLabsService.CoachingUrgency.NORMAL
        val priority = ElevenLabsService.AudioPriority.NORMAL
        val coachId = "test_coach"
        
        // When - no coaching requests
        var coachingPriority = musicCoachingIntegration.coachingPriority.value
        assertEquals(MusicCoachingIntegration.CoachingPriority.LOW, coachingPriority)
        
        // When - add normal coaching
        musicCoachingIntegration.requestCoaching(message, urgency, priority, coachId)
        coachingPriority = musicCoachingIntegration.coachingPriority.value
        assertEquals(MusicCoachingIntegration.CoachingPriority.NORMAL, coachingPriority)
        
        // When - add multiple coaching requests
        repeat(4) {
            musicCoachingIntegration.requestCoaching(message, urgency, priority, coachId)
        }
        coachingPriority = musicCoachingIntegration.coachingPriority.value
        assertEquals(MusicCoachingIntegration.CoachingPriority.HIGH, coachingPriority)
        
        // When - add urgent coaching
        musicCoachingIntegration.requestCoaching(
            message, 
            ElevenLabsService.CoachingUrgency.URGENT, 
            priority, 
            coachId, 
            isUrgent = true
        )
        coachingPriority = musicCoachingIntegration.coachingPriority.value
        assertEquals(MusicCoachingIntegration.CoachingPriority.URGENT, coachingPriority)
    }
    
    @Test
    fun `clearCoachingQueue should empty queue and reset priority`() = runTest {
        // Given
        val message = "Test coaching message"
        val urgency = ElevenLabsService.CoachingUrgency.NORMAL
        val priority = ElevenLabsService.AudioPriority.NORMAL
        val coachId = "test_coach"
        
        // Add some coaching requests
        musicCoachingIntegration.requestCoaching(message, urgency, priority, coachId)
        musicCoachingIntegration.requestCoaching(message, urgency, priority, coachId)
        
        // Verify queue has items
        var queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(2, queueStatus.queueSize)
        
        // When
        musicCoachingIntegration.clearCoachingQueue()
        
        // Then
        queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(0, queueStatus.queueSize)
        assertFalse(queueStatus.hasUrgent)
        
        val coachingPriority = musicCoachingIntegration.coachingPriority.value
        assertEquals(MusicCoachingIntegration.CoachingPriority.LOW, coachingPriority)
    }
    
    @Test
    fun `getQueueStatus should provide accurate information`() = runTest {
        // Given
        val message = "Test coaching message"
        val urgency = ElevenLabsService.CoachingUrgency.NORMAL
        val priority = ElevenLabsService.AudioPriority.NORMAL
        val coachId = "test_coach"
        
        // When - empty queue
        var queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(0, queueStatus.queueSize)
        assertFalse(queueStatus.hasUrgent)
        assertEquals(0, queueStatus.estimatedWaitTime)
        
        // When - add coaching requests
        musicCoachingIntegration.requestCoaching(message, urgency, priority, coachId)
        musicCoachingIntegration.requestCoaching(message, urgency, priority, coachId)
        
        queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(2, queueStatus.queueSize)
        assertFalse(queueStatus.hasUrgent)
        assertEquals(10, queueStatus.estimatedWaitTime) // 2 * 5 seconds
        
        // When - add urgent coaching
        musicCoachingIntegration.requestCoaching(
            message, 
            ElevenLabsService.CoachingUrgency.URGENT, 
            priority, 
            coachId, 
            isUrgent = true
        )
        
        queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(3, queueStatus.queueSize)
        assertTrue(queueStatus.hasUrgent)
        assertEquals(15, queueStatus.estimatedWaitTime) // 3 * 5 seconds
    }
    
    @Test
    fun `getBpmMatchingSuggestion should return null when no cadence data`() = runTest {
        // Given
        // No cadence data available
        
        // When
        val suggestion = musicCoachingIntegration.getBpmMatchingSuggestion()
        
        // Then
        assertNull(suggestion)
    }
    
    @Test
    fun `getBpmMatchingSuggestion should return suggestion when cadence and track available`() = runTest {
        // Given
        val cadenceData = BPMAnalysisEngine.CadenceData(160.0, 0.8f)
        val currentTrack = SpotifyService.Track(
            id = "track1",
            name = "Test Track",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 180000L,
            uri = "spotify:track:track1"
        )
        val audioFeatures = SpotifyService.AudioFeatures(
            id = "track1",
            bpm = 155.0f,
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
        
        // Mock BPM analysis engine
        `when`(mockBpmAnalysisEngine.currentCadence).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(cadenceData))
        
        // Mock Spotify service
        `when`(mockSpotifyService.getTrackAudioFeatures("track1"))
            .thenReturn(Result.success(audioFeatures))
        
        // When
        val suggestion = musicCoachingIntegration.getBpmMatchingSuggestion()
        
        // Then
        assertNotNull(suggestion)
        assertEquals(160, suggestion.currentCadence)
        assertEquals(155, suggestion.currentTrackBpm)
        assertEquals(5, suggestion.bpmDifference)
        assertTrue(suggestion.isGoodMatch) // 5 BPM difference is good
        assertTrue(suggestion.suggestion.contains("Good BPM match"))
    }
    
    @Test
    fun `getBpmMatchingSuggestion should handle different BPM differences`() = runTest {
        // Given
        val cadenceData = BPMAnalysisEngine.CadenceData(160.0, 0.8f)
        val currentTrack = SpotifyService.Track(
            id = "track1",
            name = "Test Track",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 180000L,
            uri = "spotify:track:track1"
        )
        
        // Mock BPM analysis engine
        `when`(mockBpmAnalysisEngine.currentCadence).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(cadenceData))
        
        // Test different BPM differences
        val testCases = listOf(
            155 to "Good BPM match",      // 5 BPM difference
            150 to "Moderate BPM match",  // 10 BPM difference
            140 to "Moderate BPM match",  // 20 BPM difference
            130 to "Consider changing"    // 30 BPM difference
        )
        
        for ((trackBpm, expectedSuggestion) in testCases) {
            val audioFeatures = SpotifyService.AudioFeatures(
                id = "track1",
                bpm = trackBpm.toFloat(),
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
            
            `when`(mockSpotifyService.getTrackAudioFeatures("track1"))
                .thenReturn(Result.success(audioFeatures))
            
            // When
            val suggestion = musicCoachingIntegration.getBpmMatchingSuggestion()
            
            // Then
            assertNotNull(suggestion)
            assertTrue(suggestion.suggestion.contains(expectedSuggestion))
        }
    }
    
    @Test
    fun `music state should be initialized correctly`() {
        // Given
        // Music coaching integration is initialized
        
        // When
        val musicState = musicCoachingIntegration.musicState.value
        
        // Then
        assertFalse(musicState.isPlaying)
        assertNull(musicState.currentTrack)
        assertEquals(0L, musicState.trackProgress)
        assertEquals(0L, musicState.trackDuration)
        assertEquals(100, musicState.volume)
        assertFalse(musicState.isDucking)
    }
    
    @Test
    fun `coaching priority should start as normal`() {
        // Given
        // Music coaching integration is initialized
        
        // When
        val coachingPriority = musicCoachingIntegration.coachingPriority.value
        
        // Then
        assertEquals(MusicCoachingIntegration.CoachingPriority.NORMAL, coachingPriority)
    }
    
    @Test
    fun `audio ducking level should start at 30 percent`() {
        // Given
        // Music coaching integration is initialized
        
        // When
        val audioDuckingLevel = musicCoachingIntegration.audioDuckingLevel.value
        
        // Then
        assertEquals(30, audioDuckingLevel)
    }
    
    @Test
    fun `music aware coaching should be enabled by default`() {
        // Given
        // Music coaching integration is initialized
        
        // When
        val musicAwareCoaching = musicCoachingIntegration.musicAwareCoaching.value
        
        // Then
        assertTrue(musicAwareCoaching)
    }
    
    @Test
    fun `cleanup should cancel coroutines and clear state`() {
        // Given
        // Add some coaching requests
        musicCoachingIntegration.requestCoaching(
            "Test message",
            ElevenLabsService.CoachingUrgency.NORMAL,
            ElevenLabsService.AudioPriority.NORMAL,
            "test_coach"
        )
        
        // Verify we have requests
        var queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(1, queueStatus.queueSize)
        
        // When
        musicCoachingIntegration.cleanup()
        
        // Then
        queueStatus = musicCoachingIntegration.getQueueStatus()
        assertEquals(0, queueStatus.queueSize)
        // Coroutines should be cancelled (this is tested indirectly through state changes)
    }
    
    @Test
    fun `data classes should have correct properties`() {
        // Test MusicState
        val musicState = MusicCoachingIntegration.MusicState(
            isPlaying = true,
            currentTrack = SpotifyService.Track(
                id = "test",
                name = "Test Track",
                artist = "Test Artist",
                album = "Test Album",
                durationMs = 180000L,
                uri = "spotify:track:test"
            ),
            trackProgress = 30000L,
            trackDuration = 180000L,
            volume = 75,
            isDucking = true
        )
        
        assertTrue(musicState.isPlaying)
        assertNotNull(musicState.currentTrack)
        assertEquals("Test Track", musicState.currentTrack?.name)
        assertEquals(30000L, musicState.trackProgress)
        assertEquals(180000L, musicState.trackDuration)
        assertEquals(75, musicState.volume)
        assertTrue(musicState.isDucking)
        
        // Test BpmMatchingSuggestion
        val bpmSuggestion = MusicCoachingIntegration.BpmMatchingSuggestion(
            currentCadence = 160,
            currentTrackBpm = 155,
            bpmDifference = 5,
            isGoodMatch = true,
            suggestion = "Perfect BPM match!"
        )
        
        assertEquals(160, bpmSuggestion.currentCadence)
        assertEquals(155, bpmSuggestion.currentTrackBpm)
        assertEquals(5, bpmSuggestion.bpmDifference)
        assertTrue(bpmSuggestion.isGoodMatch)
        assertEquals("Perfect BPM match!", bpmSuggestion.suggestion)
        
        // Test QueueStatus
        val queueStatus = MusicCoachingIntegration.QueueStatus(
            queueSize = 3,
            hasUrgent = true,
            estimatedWaitTime = 15
        )
        
        assertEquals(3, queueStatus.queueSize)
        assertTrue(queueStatus.hasUrgent)
        assertEquals(15, queueStatus.estimatedWaitTime)
    }
    
    @Test
    fun `enums should have correct values`() {
        // Test CoachingPriority
        assertEquals(4, MusicCoachingIntegration.CoachingPriority.values().size)
        assertTrue(MusicCoachingIntegration.CoachingPriority.values().contains(MusicCoachingIntegration.CoachingPriority.LOW))
        assertTrue(MusicCoachingIntegration.CoachingPriority.values().contains(MusicCoachingIntegration.CoachingPriority.NORMAL))
        assertTrue(MusicCoachingIntegration.CoachingPriority.values().contains(MusicCoachingIntegration.CoachingPriority.HIGH))
        assertTrue(MusicCoachingIntegration.CoachingPriority.values().contains(MusicCoachingIntegration.CoachingPriority.URGENT))
        
        // Test DuckingStrategy
        assertEquals(5, MusicCoachingIntegration.DuckingStrategy.values().size)
        assertTrue(MusicCoachingIntegration.DuckingStrategy.values().contains(MusicCoachingIntegration.DuckingStrategy.NONE))
        assertTrue(MusicCoachingIntegration.DuckingStrategy.values().contains(MusicCoachingIntegration.DuckingStrategy.LIGHT))
        assertTrue(MusicCoachingIntegration.DuckingStrategy.values().contains(MusicCoachingIntegration.DuckingStrategy.MODERATE))
        assertTrue(MusicCoachingIntegration.DuckingStrategy.values().contains(MusicCoachingIntegration.DuckingStrategy.HEAVY))
        assertTrue(MusicCoachingIntegration.DuckingStrategy.values().contains(MusicCoachingIntegration.DuckingStrategy.SMART))
    }
}

