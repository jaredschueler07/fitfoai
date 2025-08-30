package com.runningcoach.v2.data.local.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.HealthConnectDailySummaryEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Integration tests for HealthConnectDailySummaryDao
 * Tests database operations with actual Room database
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectDailySummaryDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: FITFOAIDatabase
    private lateinit var healthConnectDao: HealthConnectDailySummaryDao
    private lateinit var userDao: UserDao

    private val testUser = UserEntity(
        id = 1L,
        name = "Test User",
        email = "test@example.com",
        height = 70,
        weight = 150.0f
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FITFOAIDatabase::class.java
        ).allowMainThreadQueries().build()

        healthConnectDao = database.healthConnectDailySummaryDao()
        userDao = database.userDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveDailySummary() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val today = LocalDate.now()
        val dateMillis = today.atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        
        val summary = HealthConnectDailySummaryEntity(
            userId = userId,
            date = dateMillis,
            steps = 10000,
            distance = 8000f,
            calories = 400,
            activeMinutes = 45,
            avgHeartRate = 75f,
            maxHeartRate = 150f,
            minHeartRate = 60f,
            lastSynced = System.currentTimeMillis()
        )

        // Act
        val summaryId = healthConnectDao.insertOrUpdateDailySummary(summary)
        val retrieved = healthConnectDao.getDailySummaryForDate(userId, dateMillis)

        // Assert
        assertNotNull(retrieved)
        assertEquals(summary.steps, retrieved?.steps)
        assertEquals(summary.distance, retrieved?.distance, 0.1f)
        assertEquals(summary.calories, retrieved?.calories)
        assertEquals(summary.activeMinutes, retrieved?.activeMinutes)
        assertEquals(summary.avgHeartRate, retrieved?.avgHeartRate)
        assertTrue(summaryId > 0)
    }

    @Test
    fun updateExistingSummary() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val dateMillis = System.currentTimeMillis()
        
        val originalSummary = HealthConnectDailySummaryEntity(
            userId = userId,
            date = dateMillis,
            steps = 5000,
            distance = 4000f,
            calories = 200,
            lastSynced = System.currentTimeMillis()
        )

        // Act
        healthConnectDao.insertOrUpdateDailySummary(originalSummary)
        
        val updatedSummary = originalSummary.copy(
            steps = 8000,
            distance = 6500f,
            calories = 350
        )
        healthConnectDao.insertOrUpdateDailySummary(updatedSummary)
        
        val retrieved = healthConnectDao.getDailySummaryForDate(userId, dateMillis)

        // Assert
        assertNotNull(retrieved)
        assertEquals(8000, retrieved?.steps)
        assertEquals(6500f, retrieved?.distance, 0.1f)
        assertEquals(350, retrieved?.calories)
    }

    @Test
    fun getUserDailySummariesFlow() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val today = LocalDate.now()
        
        val summaries = listOf(
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(2).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 8000,
                distance = 6000f,
                calories = 300,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 12000,
                distance = 9000f,
                calories = 500,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 6000,
                distance = 4500f,
                calories = 250,
                lastSynced = System.currentTimeMillis()
            )
        )

        // Act
        summaries.forEach { healthConnectDao.insertOrUpdateDailySummary(it) }
        val retrievedFlow = healthConnectDao.getUserDailySummaries(userId)
        val retrievedList = retrievedFlow.first()

        // Assert
        assertEquals(3, retrievedList.size)
        // Should be ordered by date DESC (most recent first)
        assertEquals(6000, retrievedList[0].steps) // Today
        assertEquals(12000, retrievedList[1].steps) // Yesterday
        assertEquals(8000, retrievedList[2].steps) // Day before yesterday
    }

    @Test
    fun getDailySummariesForDateRange() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val today = LocalDate.now()
        
        val summaries = listOf(
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(5).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 5000,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(3).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 8000,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 12000,
                lastSynced = System.currentTimeMillis()
            )
        )

        summaries.forEach { healthConnectDao.insertOrUpdateDailySummary(it) }

        // Act - Get last 4 days
        val startDate = today.minusDays(4).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        val endDate = today.atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        val rangeResults = healthConnectDao.getDailySummariesForDateRange(userId, startDate, endDate)

        // Assert
        assertEquals(2, rangeResults.size) // Should exclude the 5-day-old entry
        assertEquals(8000, rangeResults[0].steps) // 3 days ago (ordered ASC)
        assertEquals(12000, rangeResults[1].steps) // 1 day ago
    }

    @Test
    fun getTotalStepsForPeriod() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val today = LocalDate.now()
        val startDate = today.minusDays(7).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        
        val summaries = listOf(
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(6).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 8000,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(3).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 12000,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 6000,
                lastSynced = System.currentTimeMillis()
            )
        )

        summaries.forEach { healthConnectDao.insertOrUpdateDailySummary(it) }

        // Act
        val totalSteps = healthConnectDao.getTotalStepsForPeriod(userId, startDate)

        // Assert
        assertEquals(26000, totalSteps) // 8000 + 12000 + 6000
    }

    @Test
    fun getWeeklyActivitySummary() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val today = LocalDate.now()
        val startDate = today.minusDays(6).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        val endDate = today.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        
        val summaries = listOf(
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(5).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 8000,
                distance = 6000f,
                calories = 400,
                activeMinutes = 45,
                avgHeartRate = 75f,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(3).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 12000,
                distance = 9000f,
                calories = 600,
                activeMinutes = 60,
                avgHeartRate = 80f,
                lastSynced = System.currentTimeMillis()
            ),
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 6000,
                distance = 4500f,
                calories = 300,
                activeMinutes = 30,
                avgHeartRate = 70f,
                lastSynced = System.currentTimeMillis()
            )
        )

        summaries.forEach { healthConnectDao.insertOrUpdateDailySummary(it) }

        // Act
        val weeklySummary = healthConnectDao.getWeeklyActivitySummary(userId, startDate, endDate)

        // Assert
        assertEquals(26000, weeklySummary.totalSteps) // 8000 + 12000 + 6000
        assertEquals(19500f, weeklySummary.totalDistance, 0.1f) // 6000 + 9000 + 4500
        assertEquals(1300, weeklySummary.totalCalories) // 400 + 600 + 300
        assertEquals(135, weeklySummary.totalActiveMinutes) // 45 + 60 + 30
        assertEquals(75f, weeklySummary.avgHeartRate, 1f) // (75 + 80 + 70) / 3
        assertEquals(3, weeklySummary.daysWithData)
    }

    @Test
    fun updateSyncStatus() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val summary = HealthConnectDailySummaryEntity(
            userId = userId,
            date = System.currentTimeMillis(),
            steps = 8000,
            syncStatus = HealthConnectDailySummaryEntity.SyncStatus.PENDING,
            lastSynced = System.currentTimeMillis()
        )

        val summaryId = healthConnectDao.insertOrUpdateDailySummary(summary)

        // Act
        val newTimestamp = System.currentTimeMillis() + 10000
        healthConnectDao.updateSyncStatus(
            summaryId,
            HealthConnectDailySummaryEntity.SyncStatus.SYNCED,
            null,
            newTimestamp
        )

        val retrieved = healthConnectDao.getDailySummaryForDate(userId, summary.date)

        // Assert
        assertNotNull(retrieved)
        assertEquals(HealthConnectDailySummaryEntity.SyncStatus.SYNCED, retrieved?.syncStatus)
        assertEquals(newTimestamp, retrieved?.lastSynced)
        assertNull(retrieved?.syncError)
    }

    @Test
    fun hasDataForDate() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val dateWithData = System.currentTimeMillis()
        val dateWithoutData = dateWithData - 86400000 // Previous day
        
        val summary = HealthConnectDailySummaryEntity(
            userId = userId,
            date = dateWithData,
            steps = 8000,
            lastSynced = System.currentTimeMillis()
        )

        healthConnectDao.insertOrUpdateDailySummary(summary)

        // Act & Assert
        assertTrue(healthConnectDao.hasDataForDate(userId, dateWithData))
        assertFalse(healthConnectDao.hasDataForDate(userId, dateWithoutData))
    }

    @Test
    fun getActiveDays() = runTest {
        // Arrange
        val userId = userDao.insertUser(testUser)
        val today = LocalDate.now()
        val startDate = today.minusDays(7).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        
        val summaries = listOf(
            // Active day (meets steps criteria)
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(5).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 8000,
                activeMinutes = 10,
                lastSynced = System.currentTimeMillis()
            ),
            // Active day (meets active minutes criteria)
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(3).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 500,
                activeMinutes = 20,
                lastSynced = System.currentTimeMillis()
            ),
            // Inactive day (meets neither criteria)
            HealthConnectDailySummaryEntity(
                userId = userId,
                date = today.minusDays(2).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000,
                steps = 500,
                activeMinutes = 5,
                lastSynced = System.currentTimeMillis()
            )
        )

        summaries.forEach { healthConnectDao.insertOrUpdateDailySummary(it) }

        // Act
        val activeDays = healthConnectDao.getActiveDays(
            userId = userId,
            minSteps = 1000,
            minActiveMinutes = 15,
            startDate = startDate
        )

        // Assert
        assertEquals(2, activeDays.size) // Only the first two should qualify as active
    }
}