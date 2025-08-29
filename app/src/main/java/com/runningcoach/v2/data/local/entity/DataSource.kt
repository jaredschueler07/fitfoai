package com.runningcoach.v2.data.local.entity

/**
 * Enum representing the source of run session data
 * Used to track whether data originated from FITFOAI app or external sources like Google Fit
 */
enum class DataSource {
    /**
     * Data was recorded directly in the FITFOAI application
     */
    FITFOAI,
    
    /**
     * Data was imported from Google Fit
     */
    GOOGLE_FIT
}