package com.runningcoach.v2.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

data class TrainingPlan(
    val id: String,
    val userId: String,
    val goal: RaceGoal,
    val weeks: List<TrainingWeek>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: PlanStatus = PlanStatus.ACTIVE
)

data class TrainingWeek(
    val weekNumber: Int,
    val workouts: List<Workout>,
    val totalDistance: Double,
    val focusArea: String
)

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val type: WorkoutType,
    val duration: Int, // minutes
    val distance: Double? = null, // kilometers
    val intensity: WorkoutIntensity,
    val description: String,
    val isCompleted: Boolean = false,
    val scheduledDate: LocalDate? = null
)

@Serializable
enum class WorkoutType {
    EASY_RUN,
    TEMPO_RUN,
    INTERVAL_RUN,
    LONG_RUN,
    RECOVERY_RUN,
    SPEED_WORK,
    REST_DAY,
    CROSS_TRAINING
}

@Serializable
enum class WorkoutIntensity {
    RECOVERY,
    EASY,
    MODERATE,
    HARD,
    VERY_HARD
}

enum class PlanStatus {
    ACTIVE,
    COMPLETED,
    PAUSED,
    CANCELLED
}

// Sample training data based on dashboard wireframe
object SampleTrainingData {
    val todaysWorkout = Workout(
        id = "today_1",
        name = "Recovery Run",
        type = WorkoutType.RECOVERY_RUN,
        duration = 30,
        distance = 5.0,
        intensity = WorkoutIntensity.EASY,
        description = "Easy-paced recovery run to help muscles recover"
    )
    
    val upcomingWorkouts = listOf(
        Workout(
            id = "week1_day2",
            name = "Speed Run",
            type = WorkoutType.SPEED_WORK,
            duration = 25,
            intensity = WorkoutIntensity.HARD,
            description = "Week 1, Day 2: Speed intervals"
        ),
        Workout(
            id = "week1_day3",
            name = "Long Run", 
            type = WorkoutType.LONG_RUN,
            duration = 45,
            distance = 8.0,
            intensity = WorkoutIntensity.MODERATE,
            description = "Week 1, Day 3: Build endurance"
        )
    )
    
    val pastWorkouts = listOf(
        CompletedWorkout("May 28", "Speed Run", "25:10"),
        CompletedWorkout("May 26", "Recovery Run", "30:05"), 
        CompletedWorkout("May 25", "Long Run", "45:52")
    )
}

data class CompletedWorkout(
    val date: String,
    val type: String,
    val duration: String
)
