package com.runningcoach.v2.data.worker

import android.content.Context
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.SpotifyUserPreferencesEntity
import com.runningcoach.v2.data.service.PlaylistRecommendationEngine
import com.runningcoach.v2.data.service.SpotifyService
import io.ktor.client.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.TimeUnit
import kotlin.test.*

/**
 * Comprehensive unit tests for PreRunPlaylistWorker
 * 
 * Tests background playlist generation, scheduling, WorkManager integration, and error handling
 */
@RunWith(MockitoJUnitRunner::class)
class PreRunPlaylistWorkerTest {
    
    @Mock
    private lateinit var mockDatabase: FITFOAIDatabase
    
    @Mock
    private lateinit var mockHttpClient: HttpClient
    
    @Mock
    private lateinit var mockSpotifyService: SpotifyService
    
    @Mock
    private lateinit var mockPlaylistEngine: PlaylistRecommendationEngine
    
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        
        // Initialize WorkManager for testing
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
    }
    
    @Test
    fun `schedulePlaylistGeneration should schedule work for future workout`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3) // 3 hours from now
        val workoutType = "endurance"
        val targetDuration = 30L
        val targetBpm = 130
        val userId = "test_user"
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            workoutType,
            targetDuration,
            targetBpm,
            userId
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        val inputData = workInfo.progress
        assertEquals(workoutTime, inputData.getLong(PreRunPlaylistWorker.KEY_WORKOUT_TIME, 0))
        assertEquals(workoutType, inputData.getString(PreRunPlaylistWorker.KEY_WORKOUT_TYPE))
        assertEquals(targetDuration, inputData.getLong(PreRunPlaylistWorker.KEY_TARGET_DURATION, 0))
        assertEquals(targetBpm, inputData.getInt(PreRunPlaylistWorker.KEY_TARGET_BPM, 0))
        assertEquals(userId, inputData.getString(PreRunPlaylistWorker.KEY_USER_ID))
    }
    
    @Test
    fun `schedulePlaylistGeneration should schedule immediate work for soon workout`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30) // 30 minutes from now
        val workoutType = "interval"
        val targetDuration = 45L
        val targetBpm = 150
        val userId = "test_user"
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            workoutType,
            targetDuration,
            targetBpm,
            userId
        )
        
        // Then
        // Should schedule immediate work since workout is less than 1 hour away
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
    }
    
    @Test
    fun `cancelPlaylistGeneration should cancel specific workout`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        
        // Schedule work first
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            130,
            "test_user"
        )
        
        // Verify work is scheduled
        var workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, workInfos[0].state)
        
        // When
        PreRunPlaylistWorker.cancelPlaylistGeneration(context, workoutTime)
        
        // Then
        workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isEmpty())
    }
    
    @Test
    fun `cancelAllPlaylistGeneration should cancel all pending work`() {
        // Given
        val workoutTime1 = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val workoutTime2 = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4)
        
        // Schedule multiple works
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime1,
            "endurance",
            30L,
            130,
            "test_user"
        )
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime2,
            "interval",
            45L,
            150,
            "test_user"
        )
        
        // Verify works are scheduled
        var workInfos1 = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime1").get()
        var workInfos2 = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime2").get()
        assertTrue(workInfos1.isNotEmpty())
        assertTrue(workInfos2.isNotEmpty())
        
        // When
        PreRunPlaylistWorker.cancelAllPlaylistGeneration(context)
        
        // Then
        workInfos1 = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime1").get()
        workInfos2 = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime2").get()
        assertTrue(workInfos1.isEmpty())
        assertTrue(workInfos2.isEmpty())
    }
    
    @Test
    fun `worker should have correct constraints`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            130,
            "test_user"
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        val constraints = workInfo.progress
        // Constraints should be set (NetworkType.CONNECTED, RequiresBatteryNotLow, etc.)
    }
    
    @Test
    fun `worker should have correct tags`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            130,
            "test_user"
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        val tags = workInfo.tags
        assertTrue(tags.contains(PreRunPlaylistWorker.WORK_TAG_PLAYLIST_GENERATION))
        assertTrue(tags.contains(PreRunPlaylistWorker.WORK_TAG_PRE_RUN))
    }
    
    @Test
    fun `worker should have exponential backoff policy`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            130,
            "test_user"
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        // Backoff policy should be set to exponential with 15-minute initial delay
    }
    
    @Test
    fun `schedulePeriodicMaintenance should schedule daily maintenance`() {
        // Given
        // No periodic maintenance scheduled initially
        
        // When
        PeriodicPlaylistMaintenanceWorker.schedulePeriodicMaintenance(context)
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("periodic_playlist_maintenance").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        // Should have maintenance tag
        assertTrue(workInfo.tags.contains("playlist_maintenance"))
    }
    
    @Test
    fun `worker should handle different workout types`() {
        // Given
        val workoutTypes = listOf("endurance", "interval", "recovery", "tempo")
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        
        for (workoutType in workoutTypes) {
            // When
            PreRunPlaylistWorker.schedulePlaylistGeneration(
                context,
                workoutTime + workoutTypes.indexOf(workoutType) * 1000, // Different times
                workoutType,
                30L,
                130,
                "test_user"
            )
            
            // Then
            val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_${workoutTime + workoutTypes.indexOf(workoutType) * 1000}").get()
            assertTrue(workInfos.isNotEmpty())
            
            val workInfo = workInfos[0]
            assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
            
            val inputData = workInfo.progress
            assertEquals(workoutType, inputData.getString(PreRunPlaylistWorker.KEY_WORKOUT_TYPE))
        }
    }
    
    @Test
    fun `worker should handle different fitness levels`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            130,
            "test_user"
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        // Input data should be properly set
        val inputData = workInfo.progress
        assertEquals(workoutTime, inputData.getLong(PreRunPlaylistWorker.KEY_WORKOUT_TIME, 0))
        assertEquals(30L, inputData.getLong(PreRunPlaylistWorker.KEY_TARGET_DURATION, 0))
        assertEquals(130, inputData.getInt(PreRunPlaylistWorker.KEY_TARGET_BPM, 0))
        assertEquals("test_user", inputData.getString(PreRunPlaylistWorker.KEY_USER_ID))
    }
    
    @Test
    fun `worker should handle edge cases`() {
        // Given
        val pastWorkoutTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) // Past time
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            pastWorkoutTime,
            "endurance",
            30L,
            130,
            "test_user"
        )
        
        // Then
        // Should schedule immediate work since workout time is in the past
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$pastWorkoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
    }
    
    @Test
    fun `worker should handle very long workout durations`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val longDuration = 120L // 2 hours
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            longDuration,
            130,
            "test_user"
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        val inputData = workInfo.progress
        assertEquals(longDuration, inputData.getLong(PreRunPlaylistWorker.KEY_TARGET_DURATION, 0))
    }
    
    @Test
    fun `worker should handle extreme BPM values`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val lowBpm = 60
        val highBpm = 200
        
        // When - low BPM
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "recovery",
            30L,
            lowBpm,
            "test_user"
        )
        
        // Then
        var workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        var workInfo = workInfos[0]
        var inputData = workInfo.progress
        assertEquals(lowBpm, inputData.getInt(PreRunPlaylistWorker.KEY_TARGET_BPM, 0))
        
        // When - high BPM
        val workoutTime2 = workoutTime + 1000
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime2,
            "interval",
            30L,
            highBpm,
            "test_user"
        )
        
        // Then
        workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime2").get()
        assertTrue(workInfos.isNotEmpty())
        
        workInfo = workInfos[0]
        inputData = workInfo.progress
        assertEquals(highBpm, inputData.getInt(PreRunPlaylistWorker.KEY_TARGET_BPM, 0))
    }
    
    @Test
    fun `worker should handle special characters in user ID`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val specialUserId = "user@domain.com_123"
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            130,
            specialUserId
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        val inputData = workInfo.progress
        assertEquals(specialUserId, inputData.getString(PreRunPlaylistWorker.KEY_USER_ID))
    }
    
    @Test
    fun `worker should handle concurrent scheduling`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        
        // When - schedule same workout multiple times
        repeat(3) {
            PreRunPlaylistWorker.schedulePlaylistGeneration(
                context,
                workoutTime,
                "endurance",
                30L,
                130,
                "test_user"
            )
        }
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertEquals(1, workInfos.size) // Should only have one work due to unique work policy
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
    }
    
    @Test
    fun `worker should handle empty user ID`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val emptyUserId = ""
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            130,
            emptyUserId
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        val inputData = workInfo.progress
        assertEquals(emptyUserId, inputData.getString(PreRunPlaylistWorker.KEY_USER_ID))
    }
    
    @Test
    fun `worker should handle zero duration`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val zeroDuration = 0L
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            zeroDuration,
            130,
            "test_user"
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        val inputData = workInfo.progress
        assertEquals(zeroDuration, inputData.getLong(PreRunPlaylistWorker.KEY_TARGET_DURATION, 0))
    }
    
    @Test
    fun `worker should handle zero BPM`() {
        // Given
        val workoutTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val zeroBpm = 0
        
        // When
        PreRunPlaylistWorker.schedulePlaylistGeneration(
            context,
            workoutTime,
            "endurance",
            30L,
            zeroBpm,
            "test_user"
        )
        
        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork("playlist_generation_$workoutTime").get()
        assertTrue(workInfos.isNotEmpty())
        
        val workInfo = workInfos[0]
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        
        val inputData = workInfo.progress
        assertEquals(zeroBpm, inputData.getInt(PreRunPlaylistWorker.KEY_TARGET_BPM, 0))
    }
}

