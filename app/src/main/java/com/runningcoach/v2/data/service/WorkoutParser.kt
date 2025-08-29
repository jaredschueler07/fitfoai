package com.runningcoach.v2.data.service

import com.runningcoach.v2.domain.model.Workout
import com.runningcoach.v2.domain.model.WorkoutIntensity
import com.runningcoach.v2.domain.model.WorkoutSegment
import com.runningcoach.v2.domain.model.WorkoutType

class WorkoutParser {

    fun parseWorkout(workout: Workout): List<WorkoutSegment> {
        return when (workout.type) {
            WorkoutType.EASY_RUN, WorkoutType.RECOVERY_RUN -> parseSimpleRun(workout, workout.type.name)
            WorkoutType.TEMPO_RUN -> parseTempoRun(workout)
            WorkoutType.INTERVAL_RUN, WorkoutType.SPEED_WORK -> parseIntervalRun(workout)
            WorkoutType.LONG_RUN -> parseLongRun(workout)
            else -> emptyList()
        }
    }

    private fun parseSimpleRun(workout: Workout, type: String): List<WorkoutSegment> {
        val intensity = workout.intensity
        val (minBPM, maxBPM) = getBpmForIntensity(intensity)
        return listOf(WorkoutSegment(workout.duration, minBPM, maxBPM, type))
    }

    private fun parseTempoRun(workout: Workout): List<WorkoutSegment> {
        val warmUpDuration = (workout.duration * 0.2).toInt() // 20% warm-up
        val tempoDuration = (workout.duration * 0.6).toInt()  // 60% tempo
        val coolDownDuration = (workout.duration * 0.2).toInt() // 20% cool-down

        val (warmUpMin, warmUpMax) = getBpmForIntensity(WorkoutIntensity.EASY)
        val (tempoMin, tempoMax) = getBpmForIntensity(WorkoutIntensity.HARD)

        return listOf(
            WorkoutSegment(warmUpDuration, warmUpMin, warmUpMax, "Warm-up"),
            WorkoutSegment(tempoDuration, tempoMin, tempoMax, "Tempo"),
            WorkoutSegment(coolDownDuration, warmUpMin, warmUpMax, "Cool-down")
        )
    }

    private fun parseIntervalRun(workout: Workout): List<WorkoutSegment> {
        val warmUpDuration = (workout.duration * 0.2).toInt()
        val coolDownDuration = (workout.duration * 0.2).toInt()
        val intervalPortion = workout.duration - warmUpDuration - coolDownDuration

        // Assume 5 sets of intervals
        val intervalDuration = (intervalPortion * 0.6 / 5).toInt()
        val recoveryDuration = (intervalPortion * 0.4 / 5).toInt()

        val (warmUpMin, warmUpMax) = getBpmForIntensity(WorkoutIntensity.EASY)
        val (intervalMin, intervalMax) = getBpmForIntensity(WorkoutIntensity.VERY_HARD)
        val (recoveryMin, recoveryMax) = getBpmForIntensity(WorkoutIntensity.RECOVERY)
        
        val segments = mutableListOf<WorkoutSegment>()
        segments.add(WorkoutSegment(warmUpDuration, warmUpMin, warmUpMax, "Warm-up"))
        repeat(5) {
            segments.add(WorkoutSegment(intervalDuration, intervalMin, intervalMax, "Interval"))
            segments.add(WorkoutSegment(recoveryDuration, recoveryMin, recoveryMax, "Recovery"))
        }
        segments.add(WorkoutSegment(coolDownDuration, warmUpMin, warmUpMax, "Cool-down"))

        return segments
    }

    private fun parseLongRun(workout: Workout): List<WorkoutSegment> {
        val (minBPM, maxBPM) = getBpmForIntensity(WorkoutIntensity.MODERATE)
        return listOf(WorkoutSegment(workout.duration, minBPM, maxBPM, "Long Run"))
    }

    private fun getBpmForIntensity(intensity: WorkoutIntensity): Pair<Int, Int> {
        return when (intensity) {
            WorkoutIntensity.RECOVERY -> 100 to 120
            WorkoutIntensity.EASY -> 120 to 140
            WorkoutIntensity.MODERATE -> 140 to 160
            WorkoutIntensity.HARD -> 160 to 180
            WorkoutIntensity.VERY_HARD -> 180 to 200
        }
    }
}