package com.runningcoach.v2.data.repository

import android.content.Context
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.dao.GoogleFitDailySummaryDao
import com.runningcoach.v2.data.local.dao.UserDao
import com.runningcoach.v2.data.local.dao.ConnectedAppDao
import com.runningcoach.v2.data.local.entity.GoogleFitDailySummaryEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import com.runningcoach.v2.data.service.GoogleFitService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for GoogleFitRepository focusing on data mapping and sync operations.
 * Tests the critical property name mappings that were causing compilation errors.
 */
class GoogleFitRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: FITFOAIDatabase
    private lateinit var userDao: UserDao
    private lateinit var googleFitDao: GoogleFitDailySummaryDao
    private lateinit var connectedAppDao: ConnectedAppDao
    private lateinit var googleFitService: GoogleFitService
    private lateinit var repository: GoogleFitRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        database = mockk(relaxed = true)
        userDao = mockk(relaxed = true)
        googleFitDao = mockk(relaxed = true)
        connectedAppDao = mockk(relaxed = true)
        googleFitService = mockk(relaxed = true)

        every { database.userDao() } returns userDao
        every { database.googleFitDailySummaryDao() } returns googleFitDao
        every { database.connectedAppDao() } returns connectedAppDao

        repository = GoogleFitRepository(context, database)
    }

    @Test
    fun testHeartRateMappingFromGoogleFitServiceToEntity() = runTest {
        // Arrange
        val mockUser = UserEntity(
            id = 1L,
            name = "Test User",
            age = 30,
            height = 175,
            weight = 70f,
            experienceLevel = "INTERMEDIATE",
            runningGoals = listOf("GENERAL_FITNESS"),
            selectedCoach = "Bennett"
        )
        val mockFitnessData = GoogleFitService.FitnessData(
            steps = 10000,
            distance = 5000f,
            calories = 350,
            heartRate = 75,
            activeMinutes = 45,
            weight = 70f,
            height = 1.75f
        )

        coEvery { userDao.getCurrentUser() } returns mockUser
        coEvery { googleFitDao.getDailySummaryForDate(any(), any()) } returns null
        coEvery { googleFitDao.insertDailySummary(any()) } returns 1L
        every { googleFitService.getFitnessDataForDate(any()) } returns mockFitnessData

        // Act
        val result = repository.syncDailyFitnessData()

        // Assert
        assertTrue("Sync should succeed", result.isSuccess)
        
        // Verify the entity was created with correct heart rate mapping
        val capturedEntity = slot<GoogleFitDailySummaryEntity>()
        coVerify { googleFitDao.insertDailySummary(capture(capturedEntity)) }
        
        // Critical test: Verify heart rate is mapped to avgHeartRate as Float
        assertEquals("Heart rate should be mapped correctly", 75f, capturedEntity.captured.avgHeartRate)
        assertNull("Resting heart rate should remain null", capturedEntity.captured.restingHeartRate)
        assertEquals("Steps should match", 10000, capturedEntity.captured.steps)
        assertEquals("Distance should match", 5000f, capturedEntity.captured.distance, 0.001f)
        assertEquals("Calories should match", 350, capturedEntity.captured.calories)
    }

    @Test
    fun testFitnessDataTransformationHandlesNullHeartRate() = runTest {
        // Arrange
        val mockUser = UserEntity(
            id = 1L,
            name = "Test User",
            age = 25,
            height = 170,
            weight = 65f,
            experienceLevel = "BEGINNER",
            runningGoals = listOf("WEIGHT_LOSS"),
            selectedCoach = "Mariana"
        )
        val mockFitnessData = GoogleFitService.FitnessData(
            steps = 8000,
            distance = 3000f,
            calories = 200,
            heartRate = null, // Null heart rate
            activeMinutes = 30,
            weight = 65f,
            height = 1.70f
        )

        coEvery { userDao.getCurrentUser() } returns mockUser
        coEvery { googleFitDao.getDailySummaryForDate(any(), any()) } returns null
        coEvery { googleFitDao.insertDailySummary(any()) } returns 1L
        every { googleFitService.getFitnessDataForDate(any()) } returns mockFitnessData

        // Act
        val result = repository.syncDailyFitnessData()

        // Assert
        assertTrue("Sync should succeed with null heart rate", result.isSuccess)
        
        val capturedEntity = slot<GoogleFitDailySummaryEntity>()
        coVerify { googleFitDao.insertDailySummary(capture(capturedEntity)) }
        
        // Verify null heart rate is handled correctly
        assertNull("Heart rate should be null", capturedEntity.captured.avgHeartRate)
        assertEquals("Steps should match", 8000, capturedEntity.captured.steps)
        assertEquals("Sync status should be SYNCED", "SYNCED", capturedEntity.captured.syncStatus)
    }

    @Test
    fun testDailySummaryUpdateWithExistingData() = runTest {
        // Arrange
        val mockUser = UserEntity(
            id = 1L,
            name = "Test User",
            age = 35,
            height = 180,
            weight = 80f,
            experienceLevel = "ADVANCED",
            runningGoals = listOf("PERFORMANCE"),
            selectedCoach = "Becs"
        )
        val existingEntity = GoogleFitDailySummaryEntity(
            id = 5L,
            userId = 1L,
            date = System.currentTimeMillis(),
            steps = 5000,
            avgHeartRate = 70f,
            syncStatus = "SYNCED"
        )
        val newFitnessData = GoogleFitService.FitnessData(
            steps = 12000,
            distance = 6000f,
            calories = 400,
            heartRate = 80,
            activeMinutes = 50,
            weight = 80f,
            height = 1.80f
        )

        coEvery { userDao.getCurrentUser() } returns mockUser
        coEvery { googleFitDao.getDailySummaryForDate(any(), any()) } returns existingEntity
        coEvery { googleFitDao.insertDailySummary(any()) } returns 5L
        every { googleFitService.getFitnessDataForDate(any()) } returns newFitnessData

        // Act
        val result = repository.syncDailyFitnessData()

        // Assert
        assertTrue("Update should succeed", result.isSuccess)
        
        val capturedEntity = slot<GoogleFitDailySummaryEntity>()
        coVerify { googleFitDao.insertDailySummary(capture(capturedEntity)) }
        
        // Verify entity is updated, not replaced
        assertEquals("ID should remain the same", 5L, capturedEntity.captured.id)
        assertEquals("Steps should be updated", 12000, capturedEntity.captured.steps)
        assertEquals("Heart rate should be updated", 80f, capturedEntity.captured.avgHeartRate)
        assertEquals("Distance should be set", 6000f, capturedEntity.captured.distance, 0.001f)
        assertEquals("Status should remain SYNCED", "SYNCED", capturedEntity.captured.syncStatus)
    }

    @Test
    fun testErrorHandlingForFailedGoogleFitAPICalls() = runTest {
        // Arrange
        val mockUser = UserEntity(
            id = 1L,
            name = "Test User",
            age = 28,
            height = 175,
            weight = 70f,
            experienceLevel = "INTERMEDIATE",
            runningGoals = listOf("GENERAL_FITNESS"),
            selectedCoach = "Goggins"
        )
        
        coEvery { userDao.getCurrentUser() } returns mockUser
        every { googleFitService.getFitnessDataForDate(any()) } throws Exception("API Rate Limit Exceeded")

        // Act
        val result = repository.syncDailyFitnessData()

        // Assert
        assertTrue("Sync should fail", result.isFailure)
        assertEquals("Error message should match", "API Rate Limit Exceeded", result.exceptionOrNull()?.message)
        
        // Verify no database write occurred during error
        coVerify(exactly = 0) { googleFitDao.insertDailySummary(any()) }
    }

    @Test
    fun testDailySummaryEntityCreationWithAllFields() = runTest {
        // Arrange
        val mockUser = UserEntity(
            id = 1L,
            name = "Complete Test User",
            age = 32,
            height = 178,
            weight = 72f,
            experienceLevel = "ADVANCED",
            runningGoals = listOf("MARATHON", "PERFORMANCE"),
            selectedCoach = "Bennett"
        )
        val completeFitnessData = GoogleFitService.FitnessData(
            steps = 15000,
            distance = 8500f,
            calories = 450,
            heartRate = 85,
            activeMinutes = 60,
            weight = 72f,
            height = 1.78f
        )

        coEvery { userDao.getCurrentUser() } returns mockUser
        coEvery { googleFitDao.getDailySummaryForDate(any(), any()) } returns null
        coEvery { googleFitDao.insertDailySummary(any()) } returns 1L
        every { googleFitService.getFitnessDataForDate(any()) } returns completeFitnessData

        // Act
        val result = repository.syncDailyFitnessData()

        // Assert
        assertTrue("Complete sync should succeed", result.isSuccess)
        
        val capturedEntity = slot<GoogleFitDailySummaryEntity>()
        coVerify { googleFitDao.insertDailySummary(capture(capturedEntity)) }
        
        // Verify all critical field mappings
        with(capturedEntity.captured) {
            assertEquals("User ID should match", 1L, userId)
            assertEquals("Steps should match", 15000, steps)
            assertEquals("Distance should match", 8500f, distance, 0.001f)
            assertEquals("Calories should match", 450, calories)
            assertEquals("Heart rate mapping should work", 85f, avgHeartRate) 
            assertEquals("Weight should match", 72f, weight, 0.001f)
            assertEquals("Height should match", 1.78f, height, 0.001f)
            assertEquals("Sync status should be SYNCED", "SYNCED", syncStatus)
            assertTrue("Last synced should be set", lastSynced != null)
            assertTrue("Created at should be positive", createdAt > 0)
            assertTrue("Updated at should be positive", updatedAt > 0)
        }
    }
}