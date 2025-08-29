package com.runningcoach.v2.data.local.entity

/**
 * Enum representing the source of run session data
 * Used to track whether data originated from FITFOAI app or external sources
 */
enum class DataSource {
    /**
     * Data was recorded directly in the FITFOAI application
     */
    FITFOAI,
    
    /**
     * Data was imported from Google Fit
     */
    GOOGLE_FIT,
    
    /**
     * Data was imported from Health Connect
     */
    HEALTH_CONNECT,
    
    /**
     * Data was migrated from Google Fit to Health Connect
     */
    GOOGLE_FIT_MIGRATION
}