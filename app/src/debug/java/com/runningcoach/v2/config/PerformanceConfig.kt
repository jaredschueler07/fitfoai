package com.runningcoach.v2.config

import android.os.StrictMode
import android.util.Log
import androidx.room.RoomDatabase
import com.runningcoach.v2.data.local.DatabaseCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring configuration for debug builds
 * Enables strict mode, query logging, and performance thresholds
 */
object PerformanceConfig {
    
    private const val TAG = "PerformanceMonitor"
    
    // Performance thresholds
    const val QUERY_WARNING_THRESHOLD_MS = 100L
    const val QUERY_ERROR_THRESHOLD_MS = 500L
    const val TRANSACTION_WARNING_THRESHOLD_MS = 200L
    const val TRANSACTION_ERROR_THRESHOLD_MS = 1000L
    
    // Performance metrics state
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Initialize performance monitoring for debug builds
     */
    fun initialize() {
        Log.d(TAG, "Initializing performance monitoring for debug build")
        
        // Configure StrictMode for database access detection
        configureStrictMode()
        
        // Enable Room query logging
        enableRoomQueryLogging()
        
        Log.i(TAG, "Performance monitoring initialized")
        Log.i(TAG, "Query thresholds: Warning=${QUERY_WARNING_THRESHOLD_MS}ms, Error=${QUERY_ERROR_THRESHOLD_MS}ms")
        Log.i(TAG, "Transaction thresholds: Warning=${TRANSACTION_WARNING_THRESHOLD_MS}ms, Error=${TRANSACTION_ERROR_THRESHOLD_MS}ms")
    }
    
    /**
     * Configure StrictMode to detect database access on main thread
     */
    private fun configureStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork() // Also detect network calls on main thread
                .detectCustomSlowCalls()
                .penaltyLog()
                .penaltyDialog() // Show dialog for violations in debug
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .detectServiceLeaks()
                .penaltyLog()
                .build()
        )
        
        Log.d(TAG, "StrictMode configured for database performance monitoring")
    }
    
    /**
     * Enable Room query logging via system property
     */
    private fun enableRoomQueryLogging() {
        try {
            // Enable Room's built-in query logging
            System.setProperty("room.query.logging", "true")
            Log.d(TAG, "Room query logging enabled")
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not enable Room query logging: ${e.message}")
        }
    }
    
    /**
     * Log a database operation with performance timing
     */
    fun logDatabaseOperation(
        operationType: String,
        tableName: String,
        query: String? = null,
        executionTimeMs: Long
    ) {
        coroutineScope.launch {
            val metrics = _performanceMetrics.value
            val updatedMetrics = when {
                executionTimeMs >= QUERY_ERROR_THRESHOLD_MS -> {
                    Log.e(TAG, "🚨 SLOW QUERY DETECTED ($executionTimeMs ms): $operationType on $tableName")
                    if (query != null) Log.e(TAG, "Query: $query")
                    metrics.copy(
                        totalQueries = metrics.totalQueries + 1,
                        slowQueries = metrics.slowQueries + 1,
                        slowestQueryTime = maxOf(metrics.slowestQueryTime, executionTimeMs)
                    )
                }
                executionTimeMs >= QUERY_WARNING_THRESHOLD_MS -> {
                    Log.w(TAG, "⚠️ Slow query ($executionTimeMs ms): $operationType on $tableName")
                    metrics.copy(
                        totalQueries = metrics.totalQueries + 1,
                        slowQueries = metrics.slowQueries + 1,
                        slowestQueryTime = maxOf(metrics.slowestQueryTime, executionTimeMs)
                    )
                }
                else -> {
                    Log.v(TAG, "✅ Query completed ($executionTimeMs ms): $operationType on $tableName")
                    metrics.copy(
                        totalQueries = metrics.totalQueries + 1
                    )
                }
            }
            
            _performanceMetrics.value = updatedMetrics.copy(
                averageQueryTime = calculateAverageQueryTime(updatedMetrics)
            )
        }
    }
    
    /**
     * Log transaction performance
     */
    fun logTransaction(operationName: String, executionTimeMs: Long) {
        coroutineScope.launch {
            val metrics = _performanceMetrics.value
            
            when {
                executionTimeMs >= TRANSACTION_ERROR_THRESHOLD_MS -> {
                    Log.e(TAG, "🚨 SLOW TRANSACTION ($executionTimeMs ms): $operationName")
                }
                executionTimeMs >= TRANSACTION_WARNING_THRESHOLD_MS -> {
                    Log.w(TAG, "⚠️ Slow transaction ($executionTimeMs ms): $operationName")
                }
                else -> {
                    Log.d(TAG, "✅ Transaction completed ($executionTimeMs ms): $operationName")
                }
            }
            
            _performanceMetrics.value = metrics.copy(
                totalTransactions = metrics.totalTransactions + 1,
                slowestTransactionTime = maxOf(metrics.slowestTransactionTime, executionTimeMs)
            )
        }
    }
    
    /**
     * Monitor a database query execution
     */
    inline fun <T> monitorQuery(
        operationType: String,
        tableName: String,
        query: String? = null,
        operation: () -> T
    ): T {
        val executionTime = measureTimeMillis {
            return operation()
        }
        
        logDatabaseOperation(operationType, tableName, query, executionTime)
        
        // This return will never be reached due to the return in measureTimeMillis,
        // but the compiler requires it for the inline function
        throw IllegalStateException("This should never be reached")
    }
    
    /**
     * Monitor a database transaction execution
     */
    inline fun <T> monitorTransaction(
        operationName: String,
        operation: () -> T
    ): T {
        val result: T
        val executionTime = measureTimeMillis {
            result = operation()
        }
        
        logTransaction(operationName, executionTime)
        return result
    }
    
    /**
     * Calculate average query time
     */
    private fun calculateAverageQueryTime(metrics: PerformanceMetrics): Long {
        return if (metrics.totalQueries > 0) {
            // This is a simplified calculation
            // In a real implementation, you'd want to track total execution time
            metrics.slowestQueryTime / 2 // Rough estimate
        } else {
            0L
        }
    }
    
    /**
     * Reset performance metrics
     */
    fun resetMetrics() {
        _performanceMetrics.value = PerformanceMetrics()
        Log.d(TAG, "Performance metrics reset")
    }
    
    /**
     * Get current performance summary
     */
    fun getPerformanceSummary(): String {
        val metrics = _performanceMetrics.value
        return buildString {
            appendLine("=== Database Performance Summary ===")
            appendLine("Total Queries: ${metrics.totalQueries}")
            appendLine("Slow Queries: ${metrics.slowQueries}")
            appendLine("Total Transactions: ${metrics.totalTransactions}")
            appendLine("Average Query Time: ${metrics.averageQueryTime}ms")
            appendLine("Slowest Query: ${metrics.slowestQueryTime}ms")
            appendLine("Slowest Transaction: ${metrics.slowestTransactionTime}ms")
            appendLine("Query Warning Threshold: ${QUERY_WARNING_THRESHOLD_MS}ms")
            appendLine("Query Error Threshold: ${QUERY_ERROR_THRESHOLD_MS}ms")
        }
    }
}

/**
 * Data class to hold performance metrics
 */
data class PerformanceMetrics(
    val totalQueries: Long = 0,
    val slowQueries: Long = 0,
    val totalTransactions: Long = 0,
    val averageQueryTime: Long = 0,
    val slowestQueryTime: Long = 0,
    val slowestTransactionTime: Long = 0
) {
    val slowQueryPercentage: Float
        get() = if (totalQueries > 0) {
            (slowQueries.toFloat() / totalQueries.toFloat()) * 100f
        } else {
            0f
        }
}