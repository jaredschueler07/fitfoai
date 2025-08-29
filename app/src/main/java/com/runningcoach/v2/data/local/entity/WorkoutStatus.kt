package com.runningcoach.v2.data.local.entity

/**
 * Enum representing the completion status of a workout
 * Used to track workout progress within training plans
 */
enum class WorkoutStatus {
    /**
     * Workout is scheduled but not yet started
     */
    PENDING,
    
    /**
     * Workout has been completed successfully
     */
    COMPLETED,
    
    /**
     * Workout was intentionally skipped
     */
    SKIPPED,
    
    /**
     * Workout was modified from the original plan before completion
     */
    MODIFIED
}