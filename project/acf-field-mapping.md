# 🔗 RunningCoach App - Advanced Custom Fields & Data Relationships

## 📋 Overview

This document outlines the advanced custom fields, complex data relationships, and sophisticated data modeling for the RunningCoach app. It covers entity relationships, computed fields, derived data, and complex data structures.

## 🗂️ Entity Relationship Model

### Core Entity Relationships
```
User (1) ──── (1) UserProfile
User (1) ──── (N) TrainingPlan
User (1) ──── (N) Run
User (1) ──── (1) CoachPreference

TrainingPlan (1) ──── (N) TrainingWeek
TrainingPlan (1) ──── (N) PlanAdaptation
TrainingPlan (1) ──── (N) Run

TrainingWeek (1) ──── (N) TrainingRun
TrainingWeek (1) ──── (1) WeekMetrics

Run (1) ──── (N) RunSegment
Run (1) ──── (N) CoachingEvent
Run (1) ──── (1) RunMetrics
Run (1) ──── (N) LocationPoint

Coach (1) ──── (1) CoachPersonality
Coach (1) ──── (N) CoachingTemplate
Coach (1) ──── (1) VoiceProfile
```

### Database Schema with Relationships
```sql
-- Users and Profiles
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE user_profiles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    age INTEGER,
    gender TEXT,
    weight REAL,
    height REAL,
    experience_level TEXT,
    preferred_units TEXT,
    timezone TEXT,
    location TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Training Plans and Related Data
CREATE TABLE training_plans (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    goal_distance TEXT NOT NULL,
    goal_time INTEGER NOT NULL,
    goal_date INTEGER NOT NULL,
    goal_priority TEXT NOT NULL,
    start_date INTEGER NOT NULL,
    end_date INTEGER NOT NULL,
    status TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE training_weeks (
    id TEXT PRIMARY KEY,
    plan_id TEXT NOT NULL,
    week_number INTEGER NOT NULL,
    total_distance REAL NOT NULL,
    total_time INTEGER NOT NULL,
    intensity TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES training_plans(id) ON DELETE CASCADE
);

CREATE TABLE training_runs (
    id TEXT PRIMARY KEY,
    week_id TEXT NOT NULL,
    day_of_week TEXT NOT NULL,
    run_type TEXT NOT NULL,
    distance REAL NOT NULL,
    duration INTEGER NOT NULL,
    pace_minutes REAL NOT NULL,
    pace_units TEXT NOT NULL,
    description TEXT,
    coaching_notes TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (week_id) REFERENCES training_weeks(id) ON DELETE CASCADE
);

-- Runs and Performance Data
CREATE TABLE runs (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    plan_id TEXT,
    start_time INTEGER NOT NULL,
    end_time INTEGER,
    distance REAL NOT NULL,
    duration INTEGER NOT NULL,
    average_pace_minutes REAL NOT NULL,
    average_pace_units TEXT NOT NULL,
    calories INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES training_plans(id) ON DELETE SET NULL
);

CREATE TABLE run_segments (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    segment_number INTEGER NOT NULL,
    distance REAL NOT NULL,
    duration INTEGER NOT NULL,
    pace_minutes REAL NOT NULL,
    pace_units TEXT NOT NULL,
    heart_rate INTEGER,
    elevation REAL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
);

CREATE TABLE location_points (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    altitude REAL,
    accuracy REAL,
    timestamp INTEGER NOT NULL,
    speed REAL,
    bearing REAL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
);

-- Coaching System
CREATE TABLE coaches (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    personality_type TEXT NOT NULL,
    voice_id TEXT,
    color_theme TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL
);

CREATE TABLE coach_personalities (
    id TEXT PRIMARY KEY,
    coach_id TEXT NOT NULL,
    personality_name TEXT NOT NULL,
    description TEXT,
    coaching_style TEXT,
    motivation_approach TEXT,
    voice_characteristics TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (coach_id) REFERENCES coaches(id) ON DELETE CASCADE
);

CREATE TABLE coaching_templates (
    id TEXT PRIMARY KEY,
    coach_id TEXT NOT NULL,
    template_name TEXT NOT NULL,
    template_type TEXT NOT NULL,
    content TEXT NOT NULL,
    variables TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (coach_id) REFERENCES coaches(id) ON DELETE CASCADE
);

CREATE TABLE coaching_events (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    message TEXT,
    context TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
);

-- Performance Analytics
CREATE TABLE performance_metrics (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    metric_type TEXT NOT NULL,
    metric_value REAL NOT NULL,
    metric_unit TEXT NOT NULL,
    calculated_at INTEGER NOT NULL,
    period_start INTEGER NOT NULL,
    period_end INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE training_load (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    date INTEGER NOT NULL,
    acute_load REAL NOT NULL,
    chronic_load REAL NOT NULL,
    acute_chronic_ratio REAL NOT NULL,
    training_stress_score REAL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Achievements and Goals
CREATE TABLE achievements (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    achievement_type TEXT NOT NULL,
    achievement_name TEXT NOT NULL,
    description TEXT,
    earned_at INTEGER NOT NULL,
    metadata TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE goals (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    goal_type TEXT NOT NULL,
    goal_name TEXT NOT NULL,
    target_value REAL NOT NULL,
    current_value REAL NOT NULL,
    target_date INTEGER,
    status TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## 🔗 Complex Data Relationships

### Many-to-Many Relationships
```kotlin
// User-Coach Preferences (Many-to-Many)
@Entity(tableName = "user_coach_preferences")
data class UserCoachPreference(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val coachId: String,
    val preferenceScore: Float,
    val lastUsed: Long,
    val usageCount: Int,
    val isFavorite: Boolean = false
)

// Run-Tags (Many-to-Many)
@Entity(tableName = "run_tags")
data class RunTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val tagName: String,
    val tagColor: String,
    val createdBy: String
)

// Training Plan-Templates (Many-to-Many)
@Entity(tableName = "plan_templates")
data class PlanTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: String,
    val templateId: String,
    val customizationLevel: String,
    val appliedAt: Long
)
```

### Hierarchical Relationships
```kotlin
// Training Plan Hierarchy
data class TrainingPlanHierarchy(
    val plan: TrainingPlan,
    val weeks: List<TrainingWeekHierarchy>,
    val adaptations: List<PlanAdaptation>,
    val metrics: PlanMetrics
)

data class TrainingWeekHierarchy(
    val week: TrainingWeek,
    val runs: List<TrainingRun>,
    val metrics: WeekMetrics
)

// Run Hierarchy
data class RunHierarchy(
    val run: Run,
    val segments: List<RunSegment>,
    val locationPoints: List<LocationPoint>,
    val coachingEvents: List<CoachingEvent>,
    val metrics: RunMetrics
)
```

### Polymorphic Relationships
```kotlin
// Base Event Interface
interface Event {
    val id: String
    val timestamp: Long
    val eventType: String
}

// Different Event Types
@Entity(tableName = "coaching_events")
data class CoachingEvent(
    @PrimaryKey val id: String,
    val runId: String,
    val timestamp: Long,
    val eventType: String,
    val message: String,
    val context: String,
    val coachId: String?,
    val audioUrl: String?
) : Event

@Entity(tableName = "achievement_events")
data class AchievementEvent(
    @PrimaryKey val id: String,
    val userId: String,
    val timestamp: Long,
    val eventType: String,
    val achievementType: String,
    val achievementName: String,
    val metadata: String
) : Event

@Entity(tableName = "system_events")
data class SystemEvent(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val eventType: String,
    val severity: String,
    val message: String,
    val stackTrace: String?
) : Event
```

## 📊 Computed Fields and Derived Data

### Performance Metrics Calculation
```kotlin
@Entity(tableName = "computed_metrics")
data class ComputedMetrics(
    @PrimaryKey val id: String,
    val userId: String,
    val metricType: MetricType,
    val value: Double,
    val unit: String,
    val calculatedAt: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val confidence: Double,
    val dataPoints: Int
)

enum class MetricType {
    // Pace Metrics
    AVERAGE_PACE_7_DAYS,
    AVERAGE_PACE_30_DAYS,
    BEST_PACE_5K,
    BEST_PACE_10K,
    BEST_PACE_HALF_MARATHON,
    BEST_PACE_MARATHON,
    
    // Distance Metrics
    TOTAL_DISTANCE_WEEK,
    TOTAL_DISTANCE_MONTH,
    TOTAL_DISTANCE_YEAR,
    LONGEST_RUN,
    AVERAGE_RUN_DISTANCE,
    
    // Frequency Metrics
    RUNS_PER_WEEK,
    RUNS_PER_MONTH,
    CURRENT_STREAK,
    LONGEST_STREAK,
    
    // Training Load Metrics
    ACUTE_TRAINING_LOAD,
    CHRONIC_TRAINING_LOAD,
    ACUTE_CHRONIC_RATIO,
    TRAINING_STRESS_SCORE,
    
    // Improvement Metrics
    PACE_IMPROVEMENT_30_DAYS,
    PACE_IMPROVEMENT_90_DAYS,
    CONSISTENCY_SCORE,
    RECOVERY_SCORE
}

class MetricsCalculator(
    private val runRepository: RunRepository,
    private val performanceRepository: PerformanceRepository
) {
    
    suspend fun calculatePaceMetrics(userId: String, period: TimePeriod): List<ComputedMetrics> {
        val runs = runRepository.getRunsInPeriod(userId, period.start, period.end)
        val metrics = mutableListOf<ComputedMetrics>()
        
        // Calculate average pace
        val averagePace = runs.map { it.averagePace }.average()
        metrics.add(ComputedMetrics(
            id = generateMetricId(userId, MetricType.AVERAGE_PACE_7_DAYS, period),
            userId = userId,
            metricType = MetricType.AVERAGE_PACE_7_DAYS,
            value = averagePace,
            unit = "min/km",
            calculatedAt = System.currentTimeMillis(),
            periodStart = period.start,
            periodEnd = period.end,
            confidence = calculateConfidence(runs.size),
            dataPoints = runs.size
        ))
        
        // Calculate best pace
        val bestPace = runs.minOfOrNull { it.averagePace } ?: 0.0
        metrics.add(ComputedMetrics(
            id = generateMetricId(userId, MetricType.BEST_PACE_5K, period),
            userId = userId,
            metricType = MetricType.BEST_PACE_5K,
            value = bestPace,
            unit = "min/km",
            calculatedAt = System.currentTimeMillis(),
            periodStart = period.start,
            periodEnd = period.end,
            confidence = calculateConfidence(runs.size),
            dataPoints = runs.size
        ))
        
        return metrics
    }
    
    suspend fun calculateTrainingLoad(userId: String): TrainingLoad {
        val runs = runRepository.getRecentRuns(userId, 42) // Last 6 weeks
        
        val acuteLoad = calculateAcuteLoad(runs.takeLast(7)) // Last 7 days
        val chronicLoad = calculateChronicLoad(runs.takeLast(28)) // Last 28 days
        val acuteChronicRatio = acuteLoad / chronicLoad
        
        return TrainingLoad(
            id = generateId(),
            userId = userId,
            date = System.currentTimeMillis(),
            acuteLoad = acuteLoad,
            chronicLoad = chronicLoad,
            acuteChronicRatio = acuteChronicRatio,
            trainingStressScore = calculateTSS(runs),
            createdAt = System.currentTimeMillis()
        )
    }
    
    private fun calculateAcuteLoad(runs: List<Run>): Double {
        return runs.sumOf { run ->
            val duration = run.duration.toMinutes()
            val intensity = calculateIntensity(run)
            duration * intensity * 0.1
        }
    }
    
    private fun calculateChronicLoad(runs: List<Run>): Double {
        return runs.sumOf { run ->
            val duration = run.duration.toMinutes()
            val intensity = calculateIntensity(run)
            duration * intensity * 0.025 // 28-day decay factor
        }
    }
    
    private fun calculateIntensity(run: Run): Double {
        val pace = run.averagePace.minutesPerUnit
        val distance = run.distance
        
        // Calculate intensity based on pace and distance
        return when {
            distance <= 5.0 -> pace * 0.8 // Shorter runs are less intense
            distance <= 10.0 -> pace * 1.0 // Medium runs
            else -> pace * 1.2 // Longer runs are more intense
        }
    }
}
```

### Adaptive Training Calculations
```kotlin
@Entity(tableName = "adaptive_metrics")
data class AdaptiveMetrics(
    @PrimaryKey val id: String,
    val userId: String,
    val metricType: AdaptiveMetricType,
    val value: Double,
    val confidence: Double,
    val recommendations: String,
    val calculatedAt: Long
)

enum class AdaptiveMetricType {
    READINESS_SCORE,
    FATIGUE_LEVEL,
    RECOVERY_STATUS,
    NEXT_WORKOUT_INTENSITY,
    PLAN_ADJUSTMENT_NEEDED,
    INJURY_RISK
}

class AdaptiveTrainingEngine(
    private val runRepository: RunRepository,
    private val performanceRepository: PerformanceRepository,
    private val planRepository: PlanRepository
) {
    
    suspend fun calculateReadinessScore(userId: String): AdaptiveMetrics {
        val recentRuns = runRepository.getRecentRuns(userId, 7)
        val trainingLoad = performanceRepository.getLatestTrainingLoad(userId)
        
        val readinessScore = calculateReadiness(recentRuns, trainingLoad)
        val recommendations = generateReadinessRecommendations(readinessScore)
        
        return AdaptiveMetrics(
            id = generateId(),
            userId = userId,
            metricType = AdaptiveMetricType.READINESS_SCORE,
            value = readinessScore,
            confidence = calculateConfidence(recentRuns.size),
            recommendations = recommendations,
            calculatedAt = System.currentTimeMillis()
        )
    }
    
    private fun calculateReadiness(runs: List<Run>, trainingLoad: TrainingLoad?): Double {
        if (runs.isEmpty() || trainingLoad == null) return 0.5
        
        val recentIntensity = runs.map { calculateIntensity(it) }.average()
        val acuteChronicRatio = trainingLoad.acuteChronicRatio
        
        return when {
            acuteChronicRatio < 0.8 -> 0.8 // Well recovered
            acuteChronicRatio < 1.2 -> 0.6 // Moderate fatigue
            acuteChronicRatio < 1.5 -> 0.4 // High fatigue
            else -> 0.2 // Very high fatigue
        }
    }
    
    suspend fun generatePlanAdjustments(userId: String): List<PlanAdjustment> {
        val readiness = calculateReadinessScore(userId)
        val currentPlan = planRepository.getActivePlan(userId)
        
        if (currentPlan == null || readiness.value > 0.7) {
            return emptyList() // No adjustments needed
        }
        
        val adjustments = mutableListOf<PlanAdjustment>()
        
        // Reduce intensity if readiness is low
        if (readiness.value < 0.4) {
            adjustments.add(PlanAdjustment(
                type = AdjustmentType.REDUCE_INTENSITY,
                reason = "Low readiness score (${readiness.value})",
                affectedRuns = currentPlan.weeks.flatMap { it.runs }.take(3),
                newValues = mapOf("intensity" to "reduced by 20%")
            ))
        }
        
        // Add recovery days if needed
        if (readiness.value < 0.3) {
            adjustments.add(PlanAdjustment(
                type = AdjustmentType.ADD_RECOVERY,
                reason = "Very low readiness score (${readiness.value})",
                affectedRuns = emptyList(),
                newValues = mapOf("recovery_days" to "add 2 recovery days")
            ))
        }
        
        return adjustments
    }
}

data class PlanAdjustment(
    val type: AdjustmentType,
    val reason: String,
    val affectedRuns: List<TrainingRun>,
    val newValues: Map<String, String>
)

enum class AdjustmentType {
    REDUCE_INTENSITY,
    INCREASE_INTENSITY,
    ADD_RECOVERY,
    REMOVE_RECOVERY,
    CHANGE_DISTANCE,
    CHANGE_PACE
}
```

## 🔄 Complex Data Transformations

### Data Aggregation and Summarization
```kotlin
@Entity(tableName = "aggregated_metrics")
data class AggregatedMetrics(
    @PrimaryKey val id: String,
    val userId: String,
    val aggregationType: AggregationType,
    val period: String,
    val metrics: String, // JSON string of aggregated data
    val calculatedAt: Long
)

enum class AggregationType {
    DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

class DataAggregator(
    private val runRepository: RunRepository,
    private val performanceRepository: PerformanceRepository
) {
    
    suspend fun aggregateDailyMetrics(userId: String, date: LocalDate): AggregatedMetrics {
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        
        val runs = runRepository.getRunsInPeriod(userId, startOfDay, endOfDay)
        
        val aggregatedData = mapOf(
            "totalRuns" to runs.size,
            "totalDistance" to runs.sumOf { it.distance },
            "totalDuration" to runs.sumOf { it.duration.toMinutes() },
            "averagePace" to runs.map { it.averagePace.minutesPerUnit }.average(),
            "totalCalories" to runs.sumOf { it.calories ?: 0 },
            "fastestPace" to runs.minOfOrNull { it.averagePace.minutesPerUnit } ?: 0.0,
            "longestRun" to runs.maxOfOrNull { it.distance } ?: 0.0
        )
        
        return AggregatedMetrics(
            id = generateId(),
            userId = userId,
            aggregationType = AggregationType.DAILY,
            period = date.toString(),
            metrics = Json.encodeToString(aggregatedData),
            calculatedAt = System.currentTimeMillis()
        )
    }
    
    suspend fun aggregateWeeklyMetrics(userId: String, weekStart: LocalDate): AggregatedMetrics {
        val endOfWeek = weekStart.plusWeeks(1)
        val startTime = weekStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endTime = endOfWeek.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        
        val runs = runRepository.getRunsInPeriod(userId, startTime, endTime)
        
        val aggregatedData = mapOf(
            "totalRuns" to runs.size,
            "totalDistance" to runs.sumOf { it.distance },
            "totalDuration" to runs.sumOf { it.duration.toMinutes() },
            "averagePace" to runs.map { it.averagePace.minutesPerUnit }.average(),
            "totalCalories" to runs.sumOf { it.calories ?: 0 },
            "runsByDay" to runs.groupBy { 
                Instant.ofEpochMilli(it.startTime).atZone(ZoneOffset.UTC).toLocalDate() 
            }.mapValues { it.value.size },
            "distanceByDay" to runs.groupBy { 
                Instant.ofEpochMilli(it.startTime).atZone(ZoneOffset.UTC).toLocalDate() 
            }.mapValues { it.value.sumOf { run -> run.distance } },
            "consistencyScore" to calculateConsistencyScore(runs)
        )
        
        return AggregatedMetrics(
            id = generateId(),
            userId = userId,
            aggregationType = AggregationType.WEEKLY,
            period = "${weekStart} to ${endOfWeek.minusDays(1)}",
            metrics = Json.encodeToString(aggregatedData),
            calculatedAt = System.currentTimeMillis()
        )
    }
    
    private fun calculateConsistencyScore(runs: List<Run>): Double {
        if (runs.isEmpty()) return 0.0
        
        val daysWithRuns = runs.map { 
            Instant.ofEpochMilli(it.startTime).atZone(ZoneOffset.UTC).toLocalDate() 
        }.distinct().size
        
        val totalDays = 7 // Week
        return daysWithRuns.toDouble() / totalDays
    }
}
```

### Data Normalization and Standardization
```kotlin
class DataNormalizer {
    
    fun normalizePace(pace: Pace, targetUnits: Units): Pace {
        return when {
            pace.units == targetUnits -> pace
            pace.units == Units.MILES && targetUnits == Units.KILOMETERS -> {
                Pace(
                    minutesPerUnit = pace.minutesPerUnit * 1.60934, // Convert to km
                    units = Units.KILOMETERS
                )
            }
            pace.units == Units.KILOMETERS && targetUnits == Units.MILES -> {
                Pace(
                    minutesPerUnit = pace.minutesPerUnit / 1.60934, // Convert to miles
                    units = Units.MILES
                )
            }
            else -> pace
        }
    }
    
    fun normalizeDistance(distance: Float, fromUnits: Units, toUnits: Units): Float {
        return when {
            fromUnits == toUnits -> distance
            fromUnits == Units.MILES && toUnits == Units.KILOMETERS -> distance * 1.60934f
            fromUnits == Units.KILOMETERS && toUnits == Units.MILES -> distance / 1.60934f
            else -> distance
        }
    }
    
    fun standardizeLocationData(locations: List<LocationPoint>): List<LocationPoint> {
        return locations.map { location ->
            // Remove outliers based on accuracy
            if (location.accuracy > 50) {
                location.copy(
                    latitude = 0.0,
                    longitude = 0.0,
                    altitude = null
                )
            } else {
                location
            }
        }.filter { it.latitude != 0.0 && it.longitude != 0.0 }
    }
    
    fun standardizeRunMetrics(run: Run): Run {
        val normalizedPace = normalizePace(run.averagePace, Units.KILOMETERS)
        val normalizedDistance = normalizeDistance(run.distance, Units.MILES, Units.KILOMETERS)
        
        return run.copy(
            distance = normalizedDistance,
            averagePace = normalizedPace
        )
    }
}
```

## 📈 Advanced Analytics Fields

### Predictive Analytics
```kotlin
@Entity(tableName = "predictive_metrics")
data class PredictiveMetrics(
    @PrimaryKey val id: String,
    val userId: String,
    val predictionType: PredictionType,
    val predictedValue: Double,
    val confidence: Double,
    val predictionDate: Long,
    val targetDate: Long,
    val factors: String, // JSON string of contributing factors
    val modelVersion: String,
    val calculatedAt: Long
)

enum class PredictionType {
    RACE_TIME_5K,
    RACE_TIME_10K,
    RACE_TIME_HALF_MARATHON,
    RACE_TIME_MARATHON,
    NEXT_WEEK_DISTANCE,
    NEXT_WEEK_INTENSITY,
    INJURY_RISK_30_DAYS,
    PERFORMANCE_PEAK_DATE
}

class PredictiveAnalyticsEngine(
    private val runRepository: RunRepository,
    private val performanceRepository: PerformanceRepository
) {
    
    suspend fun predictRaceTime(userId: String, distance: RaceDistance): PredictiveMetrics {
        val recentRuns = runRepository.getRecentRuns(userId, 30)
        val performanceHistory = performanceRepository.getPerformanceHistory(userId, 90)
        
        val prediction = calculateRaceTimePrediction(recentRuns, performanceHistory, distance)
        
        return PredictiveMetrics(
            id = generateId(),
            userId = userId,
            predictionType = when (distance) {
                RaceDistance.FIVE_K -> PredictionType.RACE_TIME_5K
                RaceDistance.TEN_K -> PredictionType.RACE_TIME_10K
                RaceDistance.HALF_MARATHON -> PredictionType.RACE_TIME_HALF_MARATHON
                RaceDistance.MARATHON -> PredictionType.RACE_TIME_MARATHON
            },
            predictedValue = prediction.time,
            confidence = prediction.confidence,
            predictionDate = System.currentTimeMillis(),
            targetDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000), // 30 days
            factors = Json.encodeToString(prediction.factors),
            modelVersion = "1.0",
            calculatedAt = System.currentTimeMillis()
        )
    }
    
    private fun calculateRaceTimePrediction(
        runs: List<Run>,
        performanceHistory: List<PerformanceMetrics>,
        distance: RaceDistance
    ): RaceTimePrediction {
        // Implement race time prediction algorithm
        // This is a simplified version - in practice, you'd use more sophisticated ML models
        
        val recentPace = runs.map { it.averagePace.minutesPerUnit }.average()
        val paceImprovement = calculatePaceImprovement(performanceHistory)
        val predictedPace = recentPace * (1 - paceImprovement)
        
        val predictedTime = when (distance) {
            RaceDistance.FIVE_K -> predictedPace * 5.0
            RaceDistance.TEN_K -> predictedPace * 10.0
            RaceDistance.HALF_MARATHON -> predictedPace * 21.1
            RaceDistance.MARATHON -> predictedPace * 42.2
        }
        
        val confidence = calculatePredictionConfidence(runs.size, performanceHistory.size)
        
        val factors = mapOf(
            "recentPace" to recentPace,
            "paceImprovement" to paceImprovement,
            "predictedPace" to predictedPace,
            "trainingConsistency" to calculateTrainingConsistency(runs),
            "performanceTrend" to calculatePerformanceTrend(performanceHistory)
        )
        
        return RaceTimePrediction(
            time = predictedTime,
            confidence = confidence,
            factors = factors
        )
    }
    
    private fun calculatePaceImprovement(performanceHistory: List<PerformanceMetrics>): Double {
        if (performanceHistory.size < 2) return 0.0
        
        val recent = performanceHistory.takeLast(7).map { it.metricValue }.average()
        val older = performanceHistory.take(7).map { it.metricValue }.average()
        
        return (older - recent) / older // Positive means improvement
    }
    
    private fun calculatePredictionConfidence(runCount: Int, historyCount: Int): Double {
        val dataQuality = minOf(runCount / 30.0, 1.0) // More runs = better quality
        val historyQuality = minOf(historyCount / 90.0, 1.0) // More history = better quality
        
        return (dataQuality + historyQuality) / 2.0
    }
}

data class RaceTimePrediction(
    val time: Double,
    val confidence: Double,
    val factors: Map<String, Double>
)
```

### Trend Analysis
```kotlin
@Entity(tableName = "trend_analysis")
data class TrendAnalysis(
    @PrimaryKey val id: String,
    val userId: String,
    val metricType: String,
    val trendDirection: TrendDirection,
    val trendStrength: Double,
    val period: String,
    val dataPoints: Int,
    val slope: Double,
    val rSquared: Double,
    val calculatedAt: Long
)

enum class TrendDirection {
    IMPROVING, DECLINING, STABLE, VOLATILE
}

class TrendAnalyzer(
    private val performanceRepository: PerformanceRepository
) {
    
    suspend fun analyzeTrend(userId: String, metricType: MetricType, period: TimePeriod): TrendAnalysis {
        val metrics = performanceRepository.getMetricsInPeriod(userId, metricType, period.start, period.end)
        
        if (metrics.size < 3) {
            return TrendAnalysis(
                id = generateId(),
                userId = userId,
                metricType = metricType.name,
                trendDirection = TrendDirection.STABLE,
                trendStrength = 0.0,
                period = "${period.start} to ${period.end}",
                dataPoints = metrics.size,
                slope = 0.0,
                rSquared = 0.0,
                calculatedAt = System.currentTimeMillis()
            )
        }
        
        val trend = calculateLinearTrend(metrics)
        val direction = determineTrendDirection(trend.slope, trend.rSquared)
        val strength = calculateTrendStrength(trend.rSquared, metrics.size)
        
        return TrendAnalysis(
            id = generateId(),
            userId = userId,
            metricType = metricType.name,
            trendDirection = direction,
            trendStrength = strength,
            period = "${period.start} to ${period.end}",
            dataPoints = metrics.size,
            slope = trend.slope,
            rSquared = trend.rSquared,
            calculatedAt = System.currentTimeMillis()
        )
    }
    
    private fun calculateLinearTrend(metrics: List<ComputedMetrics>): LinearTrend {
        val xValues = metrics.mapIndexed { index, _ -> index.toDouble() }
        val yValues = metrics.map { it.value }
        
        val n = xValues.size
        val sumX = xValues.sum()
        val sumY = yValues.sum()
        val sumXY = xValues.zip(yValues).sumOf { it.first * it.second }
        val sumX2 = xValues.sumOf { it * it }
        val sumY2 = yValues.sumOf { it * it }
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n
        
        val rSquared = calculateRSquared(xValues, yValues, slope, intercept)
        
        return LinearTrend(slope, intercept, rSquared)
    }
    
    private fun calculateRSquared(xValues: List<Double>, yValues: List<Double>, slope: Double, intercept: Double): Double {
        val predictedValues = xValues.map { slope * it + intercept }
        val meanY = yValues.average()
        
        val ssRes = yValues.zip(predictedValues).sumOf { (actual, predicted) ->
            (actual - predicted) * (actual - predicted)
        }
        val ssTot = yValues.sumOf { (it - meanY) * (it - meanY) }
        
        return 1 - (ssRes / ssTot)
    }
    
    private fun determineTrendDirection(slope: Double, rSquared: Double): TrendDirection {
        return when {
            rSquared < 0.3 -> TrendDirection.VOLATILE
            slope > 0.01 -> TrendDirection.IMPROVING
            slope < -0.01 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun calculateTrendStrength(rSquared: Double, dataPoints: Int): Double {
        val dataQuality = minOf(dataPoints / 10.0, 1.0)
        return rSquared * dataQuality
    }
}

data class LinearTrend(
    val slope: Double,
    val intercept: Double,
    val rSquared: Double
)
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
