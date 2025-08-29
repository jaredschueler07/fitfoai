package com.runningcoach.v2.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.local.dao.UserDao
import com.runningcoach.v2.data.local.entity.RunSessionEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for RunSessionEntity and RunSessionDao operations.
 * Tests the database schema and entity relationships that were causing compilation errors.
 * 
 * Critical tests for:
 * - Property name consistency (avgPace vs averagePace, etc.)
 * - Required vs optional fields in constructor
 * - Database constraints and foreign keys
 * - Active session queries
 */
@RunWith(AndroidJUnit4::class)
class RunSessionEntityIntegrationTest {

    private lateinit var database: FITFOAIDatabase
    private lateinit var runSessionDao: RunSessionDao
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FITFOAIDatabase::class.java
        ).allowMainThreadQueries().build()
        
        runSessionDao = database.runSessionDao()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRunSessionEntityCreationWithRequiredFieldsOnly() = runTest {
        // Arrange - Create user first (foreign key requirement)
        val user = UserEntity(
            name = "Test Runner",
            email = "runner@test.com",
            height = 175f,
            weight = 70f,
            fitnessLevel = "INTERMEDIATE"
        )
        val userId = userDao.insertUser(user)

        // Act - Create session with minimal required fields (as fixed in compilation)
        val session = RunSessionEntity(
            userId = userId,
            startTime = System.currentTimeMillis(),
            duration = 0L, // Required field
            distance = 0f  // Required field
        )
        val sessionId = runSessionDao.insertRunSession(session)

        // Assert
        assertTrue("Session ID should be positive", sessionId > 0)
        
        val retrievedSession = runSessionDao.getSessionById(sessionId)
        assertNotNull("Session should be retrievable", retrievedSession)
        with(retrievedSession!!) {
            assertEquals(userId, this.userId)
            assertEquals(0L, duration)
            assertEquals(0f, distance, 0.001f)
            assertNull("End time should be null for new session", endTime)
            assertNull("Average pace should be null initially", avgPace)
            assertNull("Heart rate should be null initially", avgHeartRate)
            assertEquals("Sync status should default to false", false, syncedWithGoogleFit)
        }
    }

    @Test
    fun testRunSessionEntityWithAllFields() = runTest {
        // Arrange
        val user = UserEntity(
            name = "Test Runner",
            email = "runner@test.com", 
            height = 175f,
            weight = 70f,
            fitnessLevel = "ADVANCED"
        )
        val userId = userDao.insertUser(user)
        val currentTime = System.currentTimeMillis()

        // Act - Create session with all fields populated
        val session = RunSessionEntity(
            userId = userId,
            startTime = currentTime - 3600000, // 1 hour ago
            endTime = currentTime,
            duration = 3600000, // 1 hour in milliseconds
            distance = 10000f, // 10km
            avgSpeed = 2.78f, // ~10km/h in m/s
            maxSpeed = 4.17f, // ~15km/h in m/s
            avgPace = 6.0f, // 6 min/km
            bestPace = 5.5f, // 5.5 min/km
            avgHeartRate = 150,
            maxHeartRate = 175,
            minHeartRate = 120,
            calories = 600,
            steps = 12000,
            cadence = 180,
            routeName = "Morning Run",
            notes = "Great run today!",
            rating = 4,
            perceivedEffort = 7,
            workoutType = "EASY",
            syncedWithGoogleFit = true,
            googleFitSessionId = "gf_session_123",
            lastSyncTime = currentTime
        )
        val sessionId = runSessionDao.insertRunSession(session)

        // Assert
        val retrievedSession = runSessionDao.getSessionById(sessionId)
        assertNotNull(retrievedSession)
        with(retrievedSession!!) {
            // Test critical property names that were causing compilation errors
            assertEquals("Average pace mapping", 6.0f, avgPace, 0.001f)
            assertEquals("Average heart rate mapping", 150, avgHeartRate)
            assertEquals("Calories mapping", 600, calories)
            assertEquals("Distance", 10000f, distance, 0.001f)
            assertEquals("Duration", 3600000L, duration)
            
            // Test Google Fit sync fields
            assertTrue("Should be synced with Google Fit", syncedWithGoogleFit)
            assertEquals("Google Fit session ID", "gf_session_123", googleFitSessionId)
            assertEquals("Last sync time", currentTime, lastSyncTime)
            
            // Test optional fields
            assertEquals("Route name", "Morning Run", routeName)
            assertEquals("Notes", "Great run today!", notes)
            assertEquals("Rating", 4, rating)
            assertEquals("Workout type", "EASY", workoutType)
        }
    }

    @Test
    fun testGetActiveSessionQuery() = runTest {
        // Arrange
        val user = UserEntity(name = "Active Runner", email = "active@test.com", height = 180f, weight = 75f, fitnessLevel = "INTERMEDIATE")
        val userId = userDao.insertUser(user)
        val currentTime = System.currentTimeMillis()

        // Create completed session (should not be returned as active)
        val completedSession = RunSessionEntity(
            userId = userId,
            startTime = currentTime - 7200000, // 2 hours ago
            endTime = currentTime - 3600000, // 1 hour ago (completed)
            duration = 3600000,
            distance = 5000f
        )
        runSessionDao.insertRunSession(completedSession)

        // Create active session (no endTime)
        val activeSession = RunSessionEntity(
            userId = userId,
            startTime = currentTime - 1800000, // 30 minutes ago
            endTime = null, // No end time = active
            duration = 0, // Will be updated as session progresses
            distance = 0f
        )
        val activeSessionId = runSessionDao.insertRunSession(activeSession)

        // Act
        val retrievedActiveSession = runSessionDao.getActiveSession(userId)

        // Assert
        assertNotNull("Should find active session", retrievedActiveSession)
        assertEquals("Should return the active session", activeSessionId, retrievedActiveSession!!.id)
        assertNull("Active session should have no end time", retrievedActiveSession.endTime)
    }

    @Test
    fun testGoogleFitSyncStatusUpdates() = runTest {
        // Arrange
        val user = UserEntity(name = "Sync Test User", email = "sync@test.com", height = 175f, weight = 70f, fitnessLevel = "BEGINNER")
        val userId = userDao.insertUser(user)
        
        val session = RunSessionEntity(
            userId = userId,
            startTime = System.currentTimeMillis() - 3600000,
            endTime = System.currentTimeMillis(),
            duration = 3600000,
            distance = 8000f,
            syncedWithGoogleFit = false
        )
        val sessionId = runSessionDao.insertRunSession(session)

        // Act - Update Google Fit sync status
        val syncTime = System.currentTimeMillis()
        runSessionDao.updateGoogleFitSync(
            sessionId = sessionId,
            synced = true,
            googleFitId = "google_fit_abc123",
            syncTime = syncTime
        )

        // Assert
        val updatedSession = runSessionDao.getSessionById(sessionId)
        assertNotNull(updatedSession)
        with(updatedSession!!) {
            assertTrue("Should be synced", syncedWithGoogleFit)
            assertEquals("Google Fit ID should be set", "google_fit_abc123", googleFitSessionId)
            assertEquals("Sync time should be updated", syncTime, lastSyncTime)
        }
    }

    @Test
    fun testUserSessionCascadeDelete() = runTest {
        // Arrange
        val user = UserEntity(name = "Delete Test", email = "delete@test.com", height = 170f, weight = 65f, fitnessLevel = "INTERMEDIATE")
        val userId = userDao.insertUser(user)
        
        // Create multiple sessions for the user
        val session1 = RunSessionEntity(userId = userId, startTime = System.currentTimeMillis() - 7200000, duration = 3600000, distance = 5000f)
        val session2 = RunSessionEntity(userId = userId, startTime = System.currentTimeMillis() - 3600000, duration = 1800000, distance = 2500f)
        
        runSessionDao.insertRunSession(session1)
        runSessionDao.insertRunSession(session2)

        // Verify sessions exist
        val initialSessions = runSessionDao.getRecentSessions(userId, 10)
        assertEquals("Should have 2 sessions", 2, initialSessions.size)

        // Act - Delete user (should cascade to sessions)
        userDao.deleteUser(user.copy(id = userId))

        // Assert - Sessions should be deleted due to foreign key cascade
        val remainingSessions = runSessionDao.getRecentSessions(userId, 10)
        assertEquals("Sessions should be deleted when user is deleted", 0, remainingSessions.size)
    }

    @Test
    fun testRunSessionStatsAggregation() = runTest {
        // Arrange
        val user = UserEntity(name = "Stats User", email = "stats@test.com", height = 175f, weight = 70f, fitnessLevel = "ADVANCED")
        val userId = userDao.insertUser(user)
        val baseTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 1 week ago

        // Create sessions over the past week
        val sessions = listOf(
            RunSessionEntity(userId = userId, startTime = baseTime + 86400000, duration = 3600000, distance = 10000f, avgPace = 6.0f, calories = 600),
            RunSessionEntity(userId = userId, startTime = baseTime + 2*86400000, duration = 2700000, distance = 7500f, avgPace = 6.5f, calories = 450),
            RunSessionEntity(userId = userId, startTime = baseTime + 3*86400000, duration = 4500000, distance = 12000f, avgPace = 5.8f, calories = 720)
        )
        
        sessions.forEach { runSessionDao.insertRunSession(it) }

        // Act
        val stats = runSessionDao.getUserStats(userId, baseTime)

        // Assert
        assertNotNull("Stats should be calculated", stats)
        with(stats!!) {
            assertEquals("Total distance", 29500f, totalDistance, 0.001f)
            assertEquals("Total duration", 10800000L, totalDuration) // 3 hours total
            assertEquals("Total calories", 1770, totalCalories)
            assertEquals("Session count", 3, sessionCount)
            assertTrue("Average pace should be calculated", averagePace != null && averagePace!! > 0)
        }
    }
}