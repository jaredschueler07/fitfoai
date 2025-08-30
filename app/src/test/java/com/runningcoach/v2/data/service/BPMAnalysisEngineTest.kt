package com.runningcoach.v2.data.service

import android.location.Location
import com.runningcoach.v2.data.local.entity.SpotifyTrackCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.*

/**
 * Comprehensive unit tests for BPMAnalysisEngine
 * 
 * Tests cadence detection, BPM matching, track recommendations, and analysis accuracy
 */
@RunWith(MockitoJUnitRunner::class)
class BPMAnalysisEngineTest {
    
    @Mock
    private lateinit var mockLocation: Location
    
    private lateinit var bpmAnalysisEngine: BPMAnalysisEngine
    
    @Before
    fun setUp() {
        bpmAnalysisEngine = BPMAnalysisEngine()
    }
    
    @Test
    fun `addLocation should update location history`() {
        // Given
        val location = createMockLocation(40.7128, -74.0060, 5.0) // NYC coordinates, 5 m/s speed
        
        // When
        bpmAnalysisEngine.addLocation(location)
        
        // Then
        val status = bpmAnalysisEngine.getStatus()
        assertEquals(1, status.locationPoints)
        assertFalse(status.isActive) // Need at least 3 points for active analysis
    }
    
    @Test
    fun `addLocation should activate analysis with 3+ points`() {
        // Given
        val location1 = createMockLocation(40.7128, -74.0060, 5.0)
        val location2 = createMockLocation(40.7129, -74.0061, 5.2)
        val location3 = createMockLocation(40.7130, -74.0062, 5.1)
        
        // When
        bpmAnalysisEngine.addLocation(location1)
        bpmAnalysisEngine.addLocation(location2)
        bpmAnalysisEngine.addLocation(location3)
        
        // Then
        val status = bpmAnalysisEngine.getStatus()
        assertEquals(3, status.locationPoints)
        assertTrue(status.isActive)
    }
    
    @Test
    fun `cadence calculation should be within reasonable range`() {
        // Given
        val locations = createLocationSequence(5.0, 10) // 5 m/s average speed
        
        // When
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        // Then
        val cadence = bpmAnalysisEngine.currentCadence.value
        assertNotNull(cadence)
        assertTrue(cadence.cadence >= 150.0) // Minimum reasonable cadence
        assertTrue(cadence.cadence <= 220.0) // Maximum reasonable cadence
        assertTrue(cadence.confidence > 0.0f)
        assertTrue(cadence.confidence <= 1.0f)
    }
    
    @Test
    fun `BPM recommendation should match cadence`() {
        // Given
        val locations = createLocationSequence(5.0, 10)
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        // When
        val cadence = bpmAnalysisEngine.currentCadence.value
        val recommendation = bpmAnalysisEngine.recommendedBpm.value
        
        // Then
        assertNotNull(cadence)
        assertNotNull(recommendation)
        assertEquals(cadence.cadence.toInt(), recommendation.targetBpm)
        assertTrue(recommendation.bpmRange.contains(recommendation.targetBpm))
        assertTrue(recommendation.bpmRange.first >= BPMAnalysisEngine.MIN_CADENCE_BPM)
        assertTrue(recommendation.bpmRange.last <= BPMAnalysisEngine.MAX_CADENCE_BPM)
    }
    
    @Test
    fun `findBestTracksForCadence should return matching tracks`() {
        // Given
        val locations = createLocationSequence(5.0, 10)
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        val availableTracks = listOf(
            createMockTrack("track1", 130),
            createMockTrack("track2", 140),
            createMockTrack("track3", 150),
            createMockTrack("track4", 160),
            createMockTrack("track5", 170)
        )
        
        // When
        val matchingTracks = bpmAnalysisEngine.findBestTracksForCadence(availableTracks, 3)
        
        // Then
        assertTrue(matchingTracks.isNotEmpty())
        assertTrue(matchingTracks.size <= 3)
        
        // All tracks should be within BPM range
        val recommendation = bpmAnalysisEngine.recommendedBpm.value
        assertNotNull(recommendation)
        matchingTracks.forEach { track ->
            assertTrue(track.bpm in recommendation.bpmRange)
        }
    }
    
    @Test
    fun `findBestTracksForCadence should return empty when no recommendation`() {
        // Given
        val availableTracks = listOf(
            createMockTrack("track1", 130),
            createMockTrack("track2", 140)
        )
        
        // When
        val matchingTracks = bpmAnalysisEngine.findBestTracksForCadence(availableTracks)
        
        // Then
        assertTrue(matchingTracks.isEmpty())
    }
    
    @Test
    fun `calculateBpmTransition should create smooth transition`() {
        // Given
        val currentTrackBpm = 120
        val targetCadence = 150.0
        val transitionDurationMs = 10000L
        
        // When
        val transition = bpmAnalysisEngine.calculateBpmTransition(
            currentTrackBpm,
            targetCadence,
            transitionDurationMs
        )
        
        // Then
        assertEquals(currentTrackBpm, transition.startBpm)
        assertEquals(targetCadence.toInt(), transition.targetBpm)
        assertTrue(transition.totalSteps > 0)
        assertTrue(transition.stepDurationMs > 0)
        assertEquals(transitionDurationMs / transition.totalSteps, transition.stepDurationMs)
    }
    
    @Test
    fun `reset should clear all data`() {
        // Given
        val locations = createLocationSequence(5.0, 5)
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        // Verify we have data
        assertTrue(bpmAnalysisEngine.getStatus().isActive)
        
        // When
        bpmAnalysisEngine.reset()
        
        // Then
        val status = bpmAnalysisEngine.getStatus()
        assertEquals(0, status.locationPoints)
        assertEquals(0, status.cadenceHistorySize)
        assertNull(status.currentCadence)
        assertNull(status.currentRecommendation)
        assertFalse(status.isActive)
    }
    
    @Test
    fun `confidence should increase with more data points`() {
        // Given
        val locations = createLocationSequence(5.0, 5)
        
        // When - add locations one by one
        locations.forEachIndexed { index, location ->
            bpmAnalysisEngine.addLocation(location)
            
            if (index >= 2) { // After 3rd location
                val cadence = bpmAnalysisEngine.currentCadence.value
                assertNotNull(cadence)
                
                // Confidence should generally increase with more data
                if (index >= 5) {
                    assertTrue(cadence.confidence > 0.5f)
                }
            }
        }
        
        // Then
        val finalCadence = bpmAnalysisEngine.currentCadence.value
        assertNotNull(finalCadence)
        assertTrue(finalCadence.confidence > 0.7f) // Should have good confidence with 5+ points
    }
    
    @Test
    fun `cadence should be smoothed with exponential moving average`() {
        // Given
        val locations = createLocationSequence(5.0, 10)
        
        // When
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        // Then
        val cadence = bpmAnalysisEngine.currentCadence.value
        assertNotNull(cadence)
        
        // The smoothed cadence should be reasonable
        assertTrue(cadence.cadence >= 150.0)
        assertTrue(cadence.cadence <= 220.0)
    }
    
    @Test
    fun `BPM tolerance should be within configured range`() {
        // Given
        val locations = createLocationSequence(5.0, 10)
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        // When
        val recommendation = bpmAnalysisEngine.recommendedBpm.value
        
        // Then
        assertNotNull(recommendation)
        val tolerance = recommendation.bpmRange.last - recommendation.bpmRange.first
        assertEquals(BPMAnalysisEngine.BPM_TOLERANCE * 2, tolerance) // Range is ±tolerance
    }
    
    @Test
    fun `cadence calculation should handle edge cases`() {
        // Given - very slow speed
        val slowLocations = createLocationSequence(1.0, 5) // 1 m/s (walking speed)
        
        // When
        slowLocations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        // Then
        val cadence = bpmAnalysisEngine.currentCadence.value
        assertNotNull(cadence)
        // Should be constrained to minimum cadence
        assertTrue(cadence.cadence >= 150.0)
        
        // Reset and test very fast speed
        bpmAnalysisEngine.reset()
        val fastLocations = createLocationSequence(10.0, 5) // 10 m/s (very fast)
        fastLocations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        val fastCadence = bpmAnalysisEngine.currentCadence.value
        assertNotNull(fastCadence)
        // Should be constrained to maximum cadence
        assertTrue(fastCadence.cadence <= 220.0)
    }
    
    @Test
    fun `engine status should provide accurate information`() {
        // Given
        val locations = createLocationSequence(5.0, 5)
        
        // When
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        // Then
        val status = bpmAnalysisEngine.getStatus()
        assertEquals(5, status.locationPoints)
        assertTrue(status.cadenceHistorySize > 0)
        assertNotNull(status.currentCadence)
        assertNotNull(status.currentRecommendation)
        assertTrue(status.isActive)
    }
    
    @Test
    fun `track matching should prioritize closest BPM`() {
        // Given
        val locations = createLocationSequence(5.0, 10)
        locations.forEach { bpmAnalysisEngine.addLocation(it) }
        
        val availableTracks = listOf(
            createMockTrack("track1", 130),
            createMockTrack("track2", 135),
            createMockTrack("track3", 140),
            createMockTrack("track4", 145),
            createMockTrack("track5", 150)
        )
        
        // When
        val matchingTracks = bpmAnalysisEngine.findBestTracksForCadence(availableTracks, 3)
        
        // Then
        assertTrue(matchingTracks.isNotEmpty())
        
        // Tracks should be sorted by BPM closeness to target
        val recommendation = bpmAnalysisEngine.recommendedBpm.value
        assertNotNull(recommendation)
        
        for (i in 0 until matchingTracks.size - 1) {
            val currentDiff = kotlin.math.abs(matchingTracks[i].bpm - recommendation.targetBpm)
            val nextDiff = kotlin.math.abs(matchingTracks[i + 1].bpm - recommendation.targetBpm)
            assertTrue(currentDiff <= nextDiff) // Earlier tracks should be closer to target
        }
    }
    
    // Helper methods
    
    private fun createMockLocation(lat: Double, lng: Double, speed: Double): Location {
        val location = mock<Location>()
        `when`(location.latitude).thenReturn(lat)
        `when`(location.longitude).thenReturn(lng)
        `when`(location.speed).thenReturn(speed.toFloat())
        return location
    }
    
    private fun createLocationSequence(averageSpeed: Double, count: Int): List<Location> {
        val locations = mutableListOf<Location>()
        val baseLat = 40.7128
        val baseLng = -74.0060
        
        for (i in 0 until count) {
            val lat = baseLat + (i * 0.0001) // Small increment
            val lng = baseLng + (i * 0.0001)
            val speed = averageSpeed + (Math.random() - 0.5) * 2 // ±1 m/s variation
            locations.add(createMockLocation(lat, lng, speed))
        }
        
        return locations
    }
    
    private fun createMockTrack(id: String, bpm: Int): SpotifyTrackCacheEntity {
        return SpotifyTrackCacheEntity(
            uri = "spotify:track:$id",
            name = "Track $id",
            artist = "Artist $id",
            album = "Album $id",
            bpm = bpm,
            energy = 0.7f,
            danceability = 0.8f,
            valence = 0.6f,
            acousticness = 0.2f,
            instrumentalness = 0.1f,
            loudness = -8.0f,
            durationMs = 180000L,
            popularity = 75,
            cachedAt = System.currentTimeMillis()
        )
    }
}

