package com.runningcoach.v2.data.local.entity

/**
 * Enum representing different types of training workouts
 * Used to categorize workouts in training plans based on intensity and purpose
 */
enum class WorkoutType {
    /**
     * Easy recovery run - low intensity, aerobic base building
     */
    EASY,
    
    /**
     * Tempo run - comfortably hard, lactate threshold training
     */
    TEMPO,
    
    /**
     * Interval training - high intensity with rest periods
     */
    INTERVAL,
    
    /**
     * Long run - extended distance for endurance building
     */
    LONG,
    
    /**
     * Recovery run - very easy, active recovery
     */
    RECOVERY
}