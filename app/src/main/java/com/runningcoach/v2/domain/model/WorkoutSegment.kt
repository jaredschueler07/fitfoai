package com.runningcoach.v2.domain.model

/**
 * Represents a single, distinct segment of a workout plan.
 *
 * @property durationMinutes The length of this workout segment in minutes.
 * @property minBPM The minimum recommended beats per minute for music during this segment.
 * @property maxBPM The maximum recommended beats per minute for music during this segment.
 * @property segmentType A descriptive name for the segment (e.g., "Warm-up", "Tempo", "Cool-down").
 */
data class WorkoutSegment(
    val durationMinutes: Int,
    val minBPM: Int,
    val maxBPM: Int,
    val segmentType: String
)