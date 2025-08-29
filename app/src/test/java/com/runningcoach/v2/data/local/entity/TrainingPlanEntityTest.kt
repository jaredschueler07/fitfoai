package com.runningcoach.v2.data.local.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Comprehensive unit tests for TrainingPlanEntity
 * 
 * Test Coverage:
 * - Entity creation and validation
 * - JSON field handling for planData
 * - Date calculations and race targets
 * - Progress tracking calculations
 * - Edge cases and null value handling
 * - Performance validation
 * - Training plan state management
 */
@DisplayName("TrainingPlanEntity Tests")
class TrainingPlanEntityTest {

    private lateinit var baseEntity: TrainingPlanEntity
    private val testUserId = 123L
    private val testName = "Marathon Training Plan"
    private val testDescription = "16-week marathon preparation program"
    private val testTargetRace = "Boston Marathon 2024"
    private val testTargetDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(112) // 16 weeks
    private val testPlanDataJson = """{
        "totalWeeks": 16,
        "weeklyMileage": [25, 28, 32, 35, 38, 42, 45, 48, 52, 55, 58, 60, 45, 35, 25, 15],
        "workouts": [
            {
                "week": 1,
                "day": 1,
                "type": "EASY",
                "distance": 5,
                "description": "Easy pace run"
            },
            {
                "week": 1,
                "day": 3,
                "type": "TEMPO",
                "distance": 8,
                "description": "5 mile tempo run"
            }
        ],
        "raceGoal": {
            "time": "3:30:00",
            "pace": "8:00"
        }
    }"""

    @BeforeEach
    fun setup() {
        baseEntity = TrainingPlanEntity(
            userId = testUserId,
            name = testName,
            description = testDescription,
            targetRace = testTargetRace,
            targetDate = testTargetDate,
            planData = testPlanDataJson
        )
    }

    @Nested
    @DisplayName("Entity Creation Tests")
    inner class EntityCreationTests {

        @Test
        @DisplayName("Should create entity with required fields")
        fun `should create entity with required fields`() {
            // Arrange & Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = testName,
                description = testDescription,
                planData = testPlanDataJson
            )

            // Assert
            assertEquals(0, entity.id) // Auto-generated primary key
            assertEquals(testUserId, entity.userId)
            assertEquals(testName, entity.name)
            assertEquals(testDescription, entity.description)
            assertEquals(testPlanDataJson, entity.planData)
            assertTrue(entity.isActive)
            assertTrue(entity.createdAt > 0)
            assertTrue(entity.updatedAt > 0)
        }

        @Test
        @DisplayName("Should create entity with all optional fields")
        fun `should create entity with all optional fields`() {
            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = testName,
                description = testDescription,
                targetRace = testTargetRace,
                targetDate = testTargetDate,
                planData = testPlanDataJson,
                isActive = true
            )

            // Assert
            assertEquals(testTargetRace, entity.targetRace)
            assertEquals(testTargetDate, entity.targetDate)
            assertTrue(entity.isActive)
        }

        @Test
        @DisplayName("Should handle null optional fields")
        fun `should handle null optional fields`() {
            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = testName,
                description = testDescription,
                targetRace = null,
                targetDate = null,
                planData = testPlanDataJson,
                isActive = false
            )

            // Assert
            assertNull(entity.targetRace)
            assertNull(entity.targetDate)
            assertFalse(entity.isActive)
        }
    }

    @Nested
    @DisplayName("JSON Plan Data Tests")
    inner class JsonPlanDataTests {

        @Test
        @DisplayName("Should handle complex training plan JSON")
        fun `should handle complex training plan JSON`() {
            // Arrange
            val complexPlanJson = """{
                "totalWeeks": 20,
                "phase1": {
                    "weeks": 1-4,
                    "focus": "base building",
                    "weeklyMileage": [20, 22, 25, 28]
                },
                "phase2": {
                    "weeks": 5-12,
                    "focus": "build",
                    "weeklyMileage": [32, 35, 38, 42, 45, 48, 52, 55]
                },
                "phase3": {
                    "weeks": 13-16,
                    "focus": "peak",
                    "weeklyMileage": [58, 60, 55, 40]
                },
                "phase4": {
                    "weeks": 17-20,
                    "focus": "taper",
                    "weeklyMileage": [30, 25, 20, 15]
                },
                "keyWorkouts": [
                    {
                        "name": "Long Run",
                        "frequency": "weekly",
                        "progression": "linear",
                        "maxDistance": 20
                    },
                    {
                        "name": "Tempo Run",
                        "frequency": "bi-weekly",
                        "duration": "20-60 minutes"
                    }
                ],
                "raceGoal": {
                    "distance": "26.2 miles",
                    "targetTime": "3:30:00",
                    "targetPace": "8:00 min/mile"
                }
            }"""

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Advanced Marathon Plan",
                description = "20-week advanced marathon training",
                planData = complexPlanJson
            )

            // Assert
            assertEquals(complexPlanJson, entity.planData)
            assertNotNull(entity.planData)
            assertTrue(entity.planData.contains("totalWeeks"))
            assertTrue(entity.planData.contains("raceGoal"))
        }

        @Test
        @DisplayName("Should handle simple workout JSON")
        fun `should handle simple workout JSON`() {
            // Arrange
            val simplePlanJson = """{
                "totalWeeks": 8,
                "weeklySchedule": {
                    "monday": "rest",
                    "tuesday": "easy_run_3mi",
                    "wednesday": "speed_work",
                    "thursday": "easy_run_3mi", 
                    "friday": "rest",
                    "saturday": "long_run",
                    "sunday": "cross_training"
                },
                "progressionType": "linear"
            }"""

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "5K Training Plan",
                description = "8-week 5K improvement program",
                planData = simplePlanJson
            )

            // Assert
            assertEquals(simplePlanJson, entity.planData)
            assertTrue(entity.planData.contains("weeklySchedule"))
        }

        @Test
        @DisplayName("Should handle empty JSON object")
        fun `should handle empty JSON object`() {
            // Arrange
            val emptyPlanJson = "{}"

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Empty Plan",
                description = "Plan with empty JSON",
                planData = emptyPlanJson
            )

            // Assert
            assertEquals(emptyPlanJson, entity.planData)
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully at entity level")
        fun `should handle malformed JSON gracefully at entity level`() {
            // Arrange - Malformed JSON (missing closing brace)
            val malformedJson = """{"totalWeeks": 16, "workouts": ["""

            // Act - Entity should accept any string, validation happens at service layer
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Test Plan",
                description = "Plan with malformed JSON",
                planData = malformedJson
            )

            // Assert - Entity stores the raw string, doesn't validate JSON format
            assertEquals(malformedJson, entity.planData)
        }
    }

    @Nested
    @DisplayName("Date Calculations Tests")
    inner class DateCalculationTests {

        @Test
        @DisplayName("Should calculate weeks until target date")
        fun `should calculate weeks until target date`() {
            // Arrange
            val currentTime = System.currentTimeMillis()
            val weeksInFuture = 12
            val targetDate = currentTime + TimeUnit.DAYS.toMillis(weeksInFuture * 7L)

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = testName,
                description = testDescription,
                targetDate = targetDate,
                planData = testPlanDataJson
            )

            // Assert
            assertEquals(targetDate, entity.targetDate)
            val daysUntilTarget = (targetDate - currentTime) / TimeUnit.DAYS.toMillis(1)
            val weeksUntilTarget = daysUntilTarget / 7
            assertTrue(weeksUntilTarget >= 11 && weeksUntilTarget <= 12) // Allow for small timing variations
        }

        @Test
        @DisplayName("Should handle past target dates")
        fun `should handle past target dates`() {
            // Arrange - Target date in the past
            val pastDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30) // 30 days ago

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Past Race Plan",
                description = "Plan for a race that already happened",
                targetDate = pastDate,
                planData = testPlanDataJson,
                isActive = false
            )

            // Assert
            assertEquals(pastDate, entity.targetDate)
            assertFalse(entity.isActive) // Should be inactive for past races
        }

        @Test
        @DisplayName("Should handle immediate target dates")
        fun `should handle immediate target dates`() {
            // Arrange - Target date tomorrow
            val tomorrowDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Last Minute Plan",
                description = "Emergency training plan",
                targetDate = tomorrowDate,
                planData = testPlanDataJson
            )

            // Assert
            assertEquals(tomorrowDate, entity.targetDate)
            assertTrue(entity.targetDate!! > System.currentTimeMillis())
        }
    }

    @Nested
    @DisplayName("Training Plan State Management")
    inner class StateManagementTests {

        @Test
        @DisplayName("Should handle active plan state")
        fun `should handle active plan state`() {
            // Act
            val activePlan = TrainingPlanEntity(
                userId = testUserId,
                name = "Current Marathon Plan",
                description = "Currently following this plan",
                planData = testPlanDataJson,
                isActive = true
            )

            // Assert
            assertTrue(activePlan.isActive)
        }

        @Test
        @DisplayName("Should handle inactive plan state")
        fun `should handle inactive plan state`() {
            // Act
            val inactivePlan = TrainingPlanEntity(
                userId = testUserId,
                name = "Completed Plan",
                description = "Plan that was completed",
                planData = testPlanDataJson,
                isActive = false
            )

            // Assert
            assertFalse(inactivePlan.isActive)
        }

        @Test
        @DisplayName("Should default to active state")
        fun `should default to active state`() {
            // Act - Don't specify isActive parameter
            val defaultPlan = TrainingPlanEntity(
                userId = testUserId,
                name = testName,
                description = testDescription,
                planData = testPlanDataJson
            )

            // Assert
            assertTrue(defaultPlan.isActive)
        }
    }

    @Nested
    @DisplayName("Training Plan Types Tests")
    inner class TrainingPlanTypesTests {

        @Test
        @DisplayName("Should handle marathon training plan")
        fun `should handle marathon training plan`() {
            // Arrange
            val marathonPlan = """{
                "distance": "26.2 miles",
                "totalWeeks": 16,
                "peakMileage": 60,
                "longRunProgression": [8, 10, 12, 14, 16, 18, 20, 22, 20, 18, 16, 14, 12, 10, 8, 6],
                "goalPace": "8:00 min/mile"
            }"""

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Marathon Training",
                description = "16-week marathon program",
                targetRace = "Boston Marathon",
                planData = marathonPlan
            )

            // Assert
            assertTrue(entity.planData.contains("26.2 miles"))
            assertTrue(entity.planData.contains("longRunProgression"))
        }

        @Test
        @DisplayName("Should handle 5K training plan")
        fun `should handle 5K training plan`() {
            // Arrange
            val fiveKPlan = """{
                "distance": "5K",
                "totalWeeks": 8,
                "peakMileage": 25,
                "speedWorkFocus": true,
                "goalTime": "20:00"
            }"""

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "5K Speed Plan",
                description = "8-week 5K improvement",
                planData = fiveKPlan
            )

            // Assert
            assertTrue(entity.planData.contains("5K"))
            assertTrue(entity.planData.contains("speedWorkFocus"))
        }

        @Test
        @DisplayName("Should handle half marathon training plan")
        fun `should handle half marathon training plan`() {
            // Arrange
            val halfMarathonPlan = """{
                "distance": "13.1 miles",
                "totalWeeks": 12,
                "peakMileage": 40,
                "tempoRunFocus": true,
                "goalPace": "7:30 min/mile"
            }"""

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Half Marathon Training",
                description = "12-week half marathon program",
                planData = halfMarathonPlan
            )

            // Assert
            assertTrue(entity.planData.contains("13.1 miles"))
            assertTrue(entity.planData.contains("tempoRunFocus"))
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long plan names")
        fun `should handle very long plan names`() {
            // Arrange
            val longName = "A".repeat(255) // Very long name

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = longName,
                description = testDescription,
                planData = testPlanDataJson
            )

            // Assert
            assertEquals(longName, entity.name)
            assertEquals(255, entity.name.length)
        }

        @Test
        @DisplayName("Should handle very long descriptions")
        fun `should handle very long descriptions`() {
            // Arrange
            val longDescription = "This is a very detailed training plan description. ".repeat(100)

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = testName,
                description = longDescription,
                planData = testPlanDataJson
            )

            // Assert
            assertEquals(longDescription, entity.description)
            assertTrue(entity.description.length > 1000)
        }

        @Test
        @DisplayName("Should handle empty strings")
        fun `should handle empty strings`() {
            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "",
                description = "",
                targetRace = "",
                planData = "{}"
            )

            // Assert
            assertEquals("", entity.name)
            assertEquals("", entity.description)
            assertEquals("", entity.targetRace)
            assertEquals("{}", entity.planData)
        }

        @Test
        @DisplayName("Should handle large JSON plan data")
        fun `should handle large JSON plan data`() {
            // Arrange - Create a large JSON with many workouts
            val largeWorkoutList = (1..365).map { day ->
                """{
                    "day": $day,
                    "type": "${if (day % 7 == 0) "LONG" else "EASY"}",
                    "distance": ${3 + (day % 10)},
                    "description": "Day $day workout description"
                }"""
            }.joinToString(",")
            
            val largePlanJson = """{
                "totalDays": 365,
                "yearPlan": true,
                "workouts": [$largeWorkoutList]
            }"""

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = "Year-Long Training Plan",
                description = "365-day comprehensive training program",
                planData = largePlanJson
            )

            // Assert
            assertEquals(largePlanJson, entity.planData)
            assertTrue(entity.planData.length > 10000) // Large JSON
        }

        @Test
        @DisplayName("Should handle negative user IDs")
        fun `should handle negative user IDs`() {
            // Act
            val entity = TrainingPlanEntity(
                userId = -1L,
                name = testName,
                description = testDescription,
                planData = testPlanDataJson
            )

            // Assert
            assertEquals(-1L, entity.userId)
        }
    }

    @Nested
    @DisplayName("Timestamp and Metadata Tests")
    inner class TimestampTests {

        @Test
        @DisplayName("Should set creation and update timestamps")
        fun `should set creation and update timestamps`() {
            // Arrange
            val beforeCreation = System.currentTimeMillis()
            Thread.sleep(1) // Ensure timestamp difference

            // Act
            val entity = TrainingPlanEntity(
                userId = testUserId,
                name = testName,
                description = testDescription,
                planData = testPlanDataJson
            )

            Thread.sleep(1) // Ensure timestamp difference
            val afterCreation = System.currentTimeMillis()

            // Assert
            assertTrue(entity.createdAt >= beforeCreation)
            assertTrue(entity.createdAt <= afterCreation)
            assertTrue(entity.updatedAt >= beforeCreation)
            assertTrue(entity.updatedAt <= afterCreation)
        }

        @Test
        @DisplayName("Should handle concurrent creation timestamps")
        fun `should handle concurrent creation timestamps`() {
            // Act - Create multiple entities quickly
            val entities = (1..5).map {
                TrainingPlanEntity(
                    userId = testUserId + it,
                    name = "$testName $it",
                    description = "$testDescription $it",
                    planData = testPlanDataJson
                )
            }

            // Assert - All should have valid timestamps
            entities.forEach { entity ->
                assertTrue(entity.createdAt > 0)
                assertTrue(entity.updatedAt > 0)
                assertTrue(entity.createdAt <= System.currentTimeMillis())
                assertTrue(entity.updatedAt <= System.currentTimeMillis())
            }

            // Timestamps should be in chronological order (or very close)
            val timestamps = entities.map { it.createdAt }
            val sortedTimestamps = timestamps.sorted()
            assertEquals(sortedTimestamps, timestamps)
        }
    }

    @Nested
    @DisplayName("Performance and Memory Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle entity creation performance")
        fun `should handle entity creation performance`() {
            // Arrange
            val iterations = 1000
            val startTime = System.currentTimeMillis()

            // Act - Create many entities
            val entities = (1..iterations).map {
                TrainingPlanEntity(
                    userId = testUserId + it,
                    name = "$testName $it",
                    description = testDescription,
                    planData = testPlanDataJson
                )
            }

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            // Assert - Should create 1000 entities reasonably quickly
            assertEquals(iterations, entities.size)
            assertTrue(duration < 1000) // Less than 1 second for 1000 entities
            
            // Verify all entities were created correctly
            entities.forEachIndexed { index, entity ->
                assertEquals(testUserId + index + 1, entity.userId)
                assertEquals("$testName ${index + 1}", entity.name)
            }
        }

        @Test
        @DisplayName("Should handle memory usage efficiently")
        fun `should handle memory usage efficiently`() {
            // Arrange
            val largeEntities = mutableListOf<TrainingPlanEntity>()
            
            // Act - Create entities with large JSON data
            repeat(100) { iteration ->
                val largeJsonData = """{
                    "iteration": $iteration,
                    "data": "${(1..1000).joinToString(",") { "item_$it" }}"
                }"""
                
                largeEntities.add(
                    TrainingPlanEntity(
                        userId = testUserId + iteration,
                        name = "Large Plan $iteration",
                        description = "Plan with large JSON data",
                        planData = largeJsonData
                    )
                )
            }

            // Assert - All entities should be created successfully
            assertEquals(100, largeEntities.size)
            largeEntities.forEach { entity ->
                assertTrue(entity.planData.contains("iteration"))
                assertTrue(entity.planData.length > 1000)
            }
        }
    }
}