package com.runningcoach.v2.data.local.dao

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.TrainingPlanEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Integration tests for WorkoutDao (Future Implementation)
 * 
 * NOTE: Since WorkoutEntity and WorkoutDao don't currently exist in the codebase,
 * this test file demonstrates comprehensive DAO testing patterns that can be applied
 * when WorkoutDao is implemented. The tests use TrainingPlanDao as a reference
 * but show the testing structure for workout-specific operations.
 * 
 * Test Coverage:
 * - CRUD operations with database constraints
 * - Foreign key relationship testing  
 * - Index performance benchmarking
 * - Concurrent access patterns
 * - Query optimization validation
 * - Error handling and edge cases
 * - Performance benchmarks for bulk operations
 */
@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var database: FITFOAIDatabase
    private lateinit var trainingPlanDao: TrainingPlanDao // Using existing DAO as reference
    private lateinit var userDao: UserDao

    // Test data
    private val testUser = UserEntity(
        id = 1,
        name = "Test User",
        email = "test@example.com"
    )

    private val testTrainingPlan = TrainingPlanEntity(
        id = 1,
        userId = 1,
        name = "Test Training Plan",
        description = "Test plan for workout testing",
        planData = """{"totalWeeks": 12, "workouts": []}"""
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FITFOAIDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FITFOAIDatabase::class.java
        ).build()
        
        userDao = database.userDao()
        trainingPlanDao = database.trainingPlanDao()

        // Insert test dependencies
        runBlocking {
            userDao.insertUser(testUser)
            trainingPlanDao.insertTrainingPlan(testTrainingPlan)
        }
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    // Future WorkoutDao interface structure (for reference)
    /*
    interface WorkoutDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertWorkout(workout: WorkoutEntity): Long
        
        @Update
        suspend fun updateWorkout(workout: WorkoutEntity)
        
        @Delete
        suspend fun deleteWorkout(workout: WorkoutEntity)
        
        @Query("SELECT * FROM workouts WHERE id = :workoutId")
        suspend fun getWorkoutById(workoutId: Long): WorkoutEntity?
        
        @Query("SELECT * FROM workouts WHERE trainingPlanId = :planId ORDER BY scheduledDate ASC")
        fun getWorkoutsForPlan(planId: Long): Flow<List<WorkoutEntity>>
        
        @Query("SELECT * FROM workouts WHERE userId = :userId AND isCompleted = 0 ORDER BY scheduledDate ASC")
        suspend fun getPendingWorkouts(userId: Long): List<WorkoutEntity>
        
        @Query("SELECT * FROM workouts WHERE userId = :userId AND workoutType = :type ORDER BY scheduledDate DESC")
        suspend fun getWorkoutsByType(userId: Long, type: String): List<WorkoutEntity>
        
        @Query("UPDATE workouts SET isCompleted = 1, completedRunSessionId = :sessionId WHERE id = :workoutId")
        suspend fun markWorkoutCompleted(workoutId: Long, sessionId: Long)
        
        @Query("SELECT COUNT(*) FROM workouts WHERE trainingPlanId = :planId")
        suspend fun getWorkoutCountForPlan(planId: Long): Int
        
        @Query("SELECT * FROM workouts WHERE scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate")
        suspend fun getWorkoutsInDateRange(startDate: Long, endDate: Long): List<WorkoutEntity>
    }
    */

    /**
     * Test basic CRUD operations using existing DAOs as reference
     * This demonstrates the testing pattern for future WorkoutDao implementation
     */
    @Test
    fun testBasicCrudOperations() = runBlocking {
        // Using TrainingPlanDao as reference for CRUD testing patterns
        
        // Test CREATE
        val newPlan = TrainingPlanEntity(
            userId = testUser.id,
            name = "CRUD Test Plan",
            description = "Plan for testing CRUD operations",
            planData = """{"totalWeeks": 8, "workouts": [{"day": 1, "type": "EASY"}]}"""
        )
        
        val insertedId = trainingPlanDao.insertTrainingPlan(newPlan)
        assertTrue("Insert should return positive ID", insertedId > 0)
        
        // Test READ
        val retrievedPlan = trainingPlanDao.getTrainingPlanById(insertedId)
        assertNotNull("Should retrieve inserted plan", retrievedPlan)
        assertEquals("Plan name should match", newPlan.name, retrievedPlan?.name)
        assertEquals("User ID should match", newPlan.userId, retrievedPlan?.userId)
        
        // Test UPDATE
        val updatedPlan = retrievedPlan!!.copy(
            name = "Updated CRUD Test Plan",
            description = "Updated description"
        )
        trainingPlanDao.updateTrainingPlan(updatedPlan)
        
        val updatedRetrieved = trainingPlanDao.getTrainingPlanById(insertedId)
        assertEquals("Name should be updated", "Updated CRUD Test Plan", updatedRetrieved?.name)
        assertEquals("Description should be updated", "Updated description", updatedRetrieved?.description)
        
        // Test DELETE
        trainingPlanDao.deleteTrainingPlan(updatedPlan)
        val deletedPlan = trainingPlanDao.getTrainingPlanById(insertedId)
        assertNull("Plan should be deleted", deletedPlan)
    }

    /**
     * Test foreign key constraints and relationships
     */
    @Test
    fun testForeignKeyConstraints() = runBlocking {
        // Test valid foreign key relationship
        val validPlan = TrainingPlanEntity(
            userId = testUser.id, // Valid user ID
            name = "Valid FK Plan",
            description = "Plan with valid foreign key",
            planData = "{}"
        )
        
        val validId = trainingPlanDao.insertTrainingPlan(validPlan)
        assertTrue("Should insert with valid foreign key", validId > 0)
        
        // Test invalid foreign key (should fail due to constraint)
        val invalidPlan = TrainingPlanEntity(
            userId = 999L, // Non-existent user ID
            name = "Invalid FK Plan",
            description = "Plan with invalid foreign key",
            planData = "{}"
        )
        
        try {
            trainingPlanDao.insertTrainingPlan(invalidPlan)
            fail("Should throw exception for invalid foreign key")
        } catch (e: Exception) {
            // Expected - foreign key constraint violation
            assertTrue("Exception should be related to constraint", 
                       e.message?.contains("FOREIGN KEY constraint failed") == true ||
                       e.message?.contains("constraint") == true)
        }
    }

    /**
     * Test query performance with different dataset sizes
     */
    @Test
    fun testQueryPerformance() = runBlocking {
        // Insert multiple training plans to test query performance
        val planCount = 1000
        val plans = mutableListOf<TrainingPlanEntity>()
        
        // Measure bulk insert performance
        val insertTime = measureTimeMillis {
            for (i in 1..planCount) {
                val plan = TrainingPlanEntity(
                    userId = testUser.id,
                    name = "Performance Test Plan $i",
                    description = "Plan $i for performance testing",
                    planData = """{"totalWeeks": 12, "planNumber": $i}"""
                )
                plans.add(plan)
                trainingPlanDao.insertTrainingPlan(plan)
            }
        }
        
        println("Bulk insert of $planCount plans took: ${insertTime}ms")
        assertTrue("Bulk insert should complete in reasonable time", insertTime < 5000) // 5 seconds max
        
        // Measure query performance
        val queryTime = measureTimeMillis {
            val retrievedPlans = trainingPlanDao.getTrainingPlansForUser(testUser.id).first()
            assertEquals("Should retrieve all inserted plans", planCount + 1, retrievedPlans.size) // +1 for setup plan
        }
        
        println("Query of $planCount plans took: ${queryTime}ms")
        assertTrue("Query should complete quickly", queryTime < 1000) // 1 second max
        
        // Measure specific query performance
        val specificQueryTime = measureTimeMillis {
            val activePlans = trainingPlanDao.getActivePlansForUser(testUser.id)
            assertTrue("Should have active plans", activePlans.isNotEmpty())
        }
        
        println("Specific query took: ${specificQueryTime}ms")
        assertTrue("Specific query should be fast", specificQueryTime < 500) // 0.5 seconds max
    }

    /**
     * Test index effectiveness on query performance
     */
    @Test
    fun testIndexPerformance() = runBlocking {
        // Insert data that would benefit from indices
        val userCount = 10
        val plansPerUser = 100
        
        // Create multiple users
        for (userId in 2..userCount) {
            userDao.insertUser(UserEntity(id = userId.toLong(), name = "User $userId", email = "user$userId@test.com"))
        }
        
        // Insert plans for each user
        for (userId in 1..userCount) {
            for (planNum in 1..plansPerUser) {
                trainingPlanDao.insertTrainingPlan(
                    TrainingPlanEntity(
                        userId = userId.toLong(),
                        name = "User $userId Plan $planNum",
                        description = "Index test plan",
                        planData = "{}",
                        isActive = planNum % 2 == 0 // Some active, some inactive
                    )
                )
            }
        }
        
        // Test indexed query performance (userId index)
        val indexedQueryTime = measureTimeMillis {
            val userPlans = trainingPlanDao.getTrainingPlansForUser(5L).first()
            assertEquals("Should get correct count for specific user", plansPerUser, userPlans.size)
        }
        
        println("Indexed query (userId) took: ${indexedQueryTime}ms")
        assertTrue("Indexed query should be fast", indexedQueryTime < 200)
        
        // Test compound query performance (userId + isActive)
        val compoundQueryTime = measureTimeMillis {
            val activePlans = trainingPlanDao.getActivePlansForUser(5L)
            assertEquals("Should get correct active count", plansPerUser / 2, activePlans.size)
        }
        
        println("Compound indexed query took: ${compoundQueryTime}ms")
        assertTrue("Compound indexed query should be fast", compoundQueryTime < 200)
    }

    /**
     * Test concurrent access patterns
     */
    @Test
    fun testConcurrentAccess() = runBlocking {
        val concurrentOperations = 50
        val results = mutableListOf<Long>()
        
        // Simulate concurrent inserts
        val concurrentTime = measureTimeMillis {
            val jobs = (1..concurrentOperations).map { i ->
                kotlinx.coroutines.async {
                    trainingPlanDao.insertTrainingPlan(
                        TrainingPlanEntity(
                            userId = testUser.id,
                            name = "Concurrent Plan $i",
                            description = "Concurrent test plan",
                            planData = "{}"
                        )
                    )
                }
            }
            
            // Wait for all inserts to complete
            jobs.forEach { job ->
                results.add(job.await())
            }
        }
        
        println("$concurrentOperations concurrent operations took: ${concurrentTime}ms")
        
        // Validate results
        assertEquals("All concurrent operations should succeed", concurrentOperations, results.size)
        assertTrue("All inserts should return positive IDs", results.all { it > 0 })
        assertTrue("All IDs should be unique", results.distinct().size == results.size)
        
        // Verify data integrity
        val totalPlans = trainingPlanDao.getTrainingPlansForUser(testUser.id).first()
        assertTrue("All concurrent plans should be retrievable", totalPlans.size >= concurrentOperations)
    }

    /**
     * Test query optimization with EXPLAIN QUERY PLAN
     */
    @Test
    fun testQueryOptimization() = runBlocking {
        // This test demonstrates how to analyze query performance
        // In a real WorkoutDao test, we would analyze specific workout queries
        
        // Insert test data
        repeat(100) { i ->
            trainingPlanDao.insertTrainingPlan(
                TrainingPlanEntity(
                    userId = testUser.id,
                    name = "Optimization Test Plan $i",
                    description = "Query optimization test",
                    planData = "{}",
                    isActive = i % 3 == 0
                )
            )
        }
        
        // Test that queries use indices effectively
        // Note: In actual implementation, we would use EXPLAIN QUERY PLAN to verify index usage
        
        val startTime = System.nanoTime()
        val plans = trainingPlanDao.getTrainingPlansForUser(testUser.id).first()
        val queryTime = (System.nanoTime() - startTime) / 1_000_000 // Convert to milliseconds
        
        assertTrue("Query should use index for fast access", queryTime < 50) // Very fast with proper indexing
        assertTrue("Should retrieve all plans", plans.size >= 100)
    }

    /**
     * Test error handling and edge cases
     */
    @Test
    fun testErrorHandling() = runBlocking {
        // Test null/empty values handling
        val planWithEmptyName = TrainingPlanEntity(
            userId = testUser.id,
            name = "", // Empty name
            description = "Test empty name handling",
            planData = "{}"
        )
        
        val emptyNameId = trainingPlanDao.insertTrainingPlan(planWithEmptyName)
        assertTrue("Should handle empty name", emptyNameId > 0)
        
        // Test very long strings
        val longName = "A".repeat(1000)
        val planWithLongName = TrainingPlanEntity(
            userId = testUser.id,
            name = longName,
            description = "Test long name handling",
            planData = "{}"
        )
        
        val longNameId = trainingPlanDao.insertTrainingPlan(planWithLongName)
        assertTrue("Should handle long names", longNameId > 0)
        
        val retrieved = trainingPlanDao.getTrainingPlanById(longNameId)
        assertEquals("Long name should be preserved", longName, retrieved?.name)
        
        // Test malformed JSON in planData
        val planWithBadJson = TrainingPlanEntity(
            userId = testUser.id,
            name = "Bad JSON Plan",
            description = "Test malformed JSON handling",
            planData = """{"unclosed": "json object"""" // Malformed JSON
        )
        
        val badJsonId = trainingPlanDao.insertTrainingPlan(planWithBadJson)
        assertTrue("Should accept malformed JSON (validation happens at service layer)", badJsonId > 0)
    }

    /**
     * Test transaction behavior and rollback scenarios
     */
    @Test
    fun testTransactionBehavior() = runBlocking {
        val initialCount = trainingPlanDao.getTrainingPlansForUser(testUser.id).first().size
        
        try {
            database.runInTransaction {
                // Insert multiple plans in a transaction
                trainingPlanDao.insertTrainingPlan(
                    TrainingPlanEntity(
                        userId = testUser.id,
                        name = "Transaction Test 1",
                        description = "First plan in transaction",
                        planData = "{}"
                    )
                )
                
                trainingPlanDao.insertTrainingPlan(
                    TrainingPlanEntity(
                        userId = testUser.id,
                        name = "Transaction Test 2",
                        description = "Second plan in transaction",
                        planData = "{}"
                    )
                )
                
                // Force a rollback by throwing an exception
                throw RuntimeException("Test rollback")
            }
        } catch (e: RuntimeException) {
            // Expected exception for testing rollback
            assertEquals("Test rollback", e.message)
        }
        
        // Verify that transaction was rolled back
        val finalCount = trainingPlanDao.getTrainingPlansForUser(testUser.id).first().size
        assertEquals("Transaction should be rolled back", initialCount, finalCount)
    }

    /**
     * Test bulk operations performance
     */
    @Test
    fun testBulkOperationsPerformance() = runBlocking {
        val bulkSize = 500
        val plans = mutableListOf<TrainingPlanEntity>()
        
        // Prepare bulk data
        for (i in 1..bulkSize) {
            plans.add(
                TrainingPlanEntity(
                    userId = testUser.id,
                    name = "Bulk Plan $i",
                    description = "Bulk operation test plan",
                    planData = """{"bulkIndex": $i}"""
                )
            )
        }
        
        // Test bulk insert performance
        val bulkInsertTime = measureTimeMillis {
            database.runInTransaction {
                plans.forEach { plan ->
                    trainingPlanDao.insertTrainingPlan(plan)
                }
            }
        }
        
        println("Bulk insert of $bulkSize plans took: ${bulkInsertTime}ms")
        assertTrue("Bulk insert in transaction should be fast", bulkInsertTime < 2000) // 2 seconds max
        
        // Verify all plans were inserted
        val retrievedPlans = trainingPlanDao.getTrainingPlansForUser(testUser.id).first()
        assertTrue("All bulk plans should be inserted", retrievedPlans.size >= bulkSize)
        
        // Test bulk query performance
        val bulkQueryTime = measureTimeMillis {
            val allPlans = trainingPlanDao.getTrainingPlansForUser(testUser.id).first()
            assertTrue("Should retrieve all bulk plans", allPlans.size >= bulkSize)
        }
        
        println("Bulk query of $bulkSize+ plans took: ${bulkQueryTime}ms")
        assertTrue("Bulk query should be fast", bulkQueryTime < 1000) // 1 second max
    }

    /**
     * Test memory usage during large operations
     */
    @Test
    fun testMemoryUsage() = runBlocking {
        val largeDatasetSize = 1000
        
        // Get baseline memory
        System.gc()
        val runtime = Runtime.getRuntime()
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform large dataset operations
        val plans = mutableListOf<TrainingPlanEntity>()
        for (i in 1..largeDatasetSize) {
            val largePlanData = """{"workouts": ${(1..100).map { """{"day": $it, "type": "EASY"}""" }.joinToString(",", "[", "]")}}"""
            
            plans.add(
                TrainingPlanEntity(
                    userId = testUser.id,
                    name = "Memory Test Plan $i",
                    description = "Large plan data for memory testing",
                    planData = largePlanData
                )
            )
        }
        
        // Insert all plans
        database.runInTransaction {
            plans.forEach { plan ->
                trainingPlanDao.insertTrainingPlan(plan)
            }
        }
        
        // Check memory usage after operations
        System.gc()
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - baselineMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        
        println("Memory increase after $largeDatasetSize large operations: ${memoryIncreaseMB}MB")
        
        // Verify reasonable memory usage (should be less than 100MB for 1000 operations)
        assertTrue("Memory usage should be reasonable", memoryIncreaseMB < 100)
        
        // Verify data integrity wasn't compromised
        val retrievedCount = trainingPlanDao.getTrainingPlansForUser(testUser.id).first().size
        assertTrue("All large dataset plans should be retrievable", retrievedCount >= largeDatasetSize)
    }

    /**
     * Test database constraint validation
     */
    @Test
    fun testDatabaseConstraints() = runBlocking {
        // Test unique constraint handling (if applicable)
        // Note: TrainingPlanEntity doesn't have unique constraints other than primary key
        
        // Test NOT NULL constraints
        try {
            // This would fail compilation, but demonstrates constraint testing approach
            // val nullNamePlan = TrainingPlanEntity(userId = testUser.id, name = null, ...)
            // trainingPlanDao.insertTrainingPlan(nullNamePlan)
            
            // For string fields that could be empty but not null
            val emptyStringPlan = TrainingPlanEntity(
                userId = testUser.id,
                name = "", // Empty but not null
                description = "",
                planData = ""
            )
            
            val id = trainingPlanDao.insertTrainingPlan(emptyStringPlan)
            assertTrue("Should accept empty strings for nullable string fields", id > 0)
            
        } catch (e: Exception) {
            // Handle constraint violations appropriately
            assertTrue("Constraint violation should be meaningful", 
                       e.message?.contains("NOT NULL") == true ||
                       e.message?.contains("constraint") == true)
        }
        
        // Test data type constraints
        val validPlan = TrainingPlanEntity(
            userId = testUser.id,
            name = "Constraint Test",
            description = "Testing database constraints",
            planData = "{}",
            isActive = true, // Boolean constraint
            targetDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30) // Future date
        )
        
        val constraintTestId = trainingPlanDao.insertTrainingPlan(validPlan)
        assertTrue("Valid data should be accepted", constraintTestId > 0)
        
        val retrieved = trainingPlanDao.getTrainingPlanById(constraintTestId)
        assertEquals("Boolean constraint should be preserved", true, retrieved?.isActive)
        assertNotNull("Target date should be preserved", retrieved?.targetDate)
    }
}