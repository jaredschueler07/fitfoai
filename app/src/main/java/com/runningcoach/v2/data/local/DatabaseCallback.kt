package com.runningcoach.v2.data.local

import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.runningcoach.v2.BuildConfig
import kotlin.system.measureTimeMillis

/**
 * Room database callback for performance monitoring and initialization
 * Tracks database operations and logs performance metrics in debug builds
 */
class DatabaseCallback : RoomDatabase.Callback() {
    
    companion object {
        private const val TAG = "DatabaseCallback"
        private const val PERFORMANCE_TAG = "DatabasePerformance"
    }
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        if (BuildConfig.DEBUG) {
            val createTime = measureTimeMillis {
                initializeDatabase(db)
            }
            Log.i(PERFORMANCE_TAG, "Database created in ${createTime}ms")
        } else {
            initializeDatabase(db)
        }
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        
        if (BuildConfig.DEBUG) {
            val openTime = measureTimeMillis {
                configureDatabase(db)
            }
            Log.d(PERFORMANCE_TAG, "Database opened and configured in ${openTime}ms")
        } else {
            configureDatabase(db)
        }
    }
    
    /**
     * Initialize database with default data and optimizations
     */
    private fun initializeDatabase(db: SupportSQLiteDatabase) {
        Log.d(TAG, "Initializing database with default data...")
        
        try {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON")
            
            // Set WAL mode for better performance
            db.execSQL("PRAGMA journal_mode=WAL")
            
            // Optimize synchronization for better performance
            db.execSQL("PRAGMA synchronous=NORMAL")
            
            // Set cache size (negative value means KB)
            db.execSQL("PRAGMA cache_size=-10240") // 10MB cache
            
            // Enable query planner optimizations
            db.execSQL("PRAGMA optimize")
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Database optimization pragmas applied")
            }
            
            // Insert default coach personalities if they don't exist
            insertDefaultCoachPersonalities(db)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during database initialization", e)
            throw e
        }
    }
    
    /**
     * Configure database settings on each connection
     */
    private fun configureDatabase(db: SupportSQLiteDatabase) {
        try {
            // Ensure foreign key constraints are enabled
            db.execSQL("PRAGMA foreign_keys=ON")
            
            // Verify WAL mode is active
            if (BuildConfig.DEBUG) {
                val cursor = db.query("PRAGMA journal_mode")
                if (cursor.moveToFirst()) {
                    val journalMode = cursor.getString(0)
                    Log.d(TAG, "Journal mode: $journalMode")
                }
                cursor.close()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during database configuration", e)
        }
    }
    
    /**
     * Insert default coach personalities
     */
    private fun insertDefaultCoachPersonalities(db: SupportSQLiteDatabase) {
        val currentTime = System.currentTimeMillis()
        
        val defaultCoaches = listOf(
            Triple(
                "bennett", "Bennett",
                "Professional and encouraging coach focused on proper form and steady progress"
            ),
            Triple(
                "mariana", "Mariana", 
                "Energetic and motivational coach who celebrates every achievement"
            ),
            Triple(
                "becs", "Becs",
                "Mindful and supportive coach emphasizing mental wellness and balance"
            ),
            Triple(
                "goggins", "Goggins",
                "Intense and challenging coach pushing you beyond your limits"
            )
        )
        
        try {
            db.beginTransaction()
            
            for ((id, name, description) in defaultCoaches) {
                // Check if coach already exists
                val cursor = db.query(
                    "SELECT COUNT(*) FROM coach_personalities WHERE id = ?",
                    arrayOf(id)
                )
                
                val exists = if (cursor.moveToFirst()) cursor.getInt(0) > 0 else false
                cursor.close()
                
                if (!exists) {
                    db.execSQL(
                        """
                        INSERT INTO coach_personalities (
                            id, name, description, voiceId, motivationalStyle, 
                            coachingFrequency, personalityTraits, isActive, createdAt
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf(
                            id,
                            name,
                            description,
                            id, // voiceId same as id
                            "balanced", // motivationalStyle
                            "moderate", // coachingFrequency
                            "{}", // personalityTraits (empty JSON)
                            if (id == "bennett") 1 else 0, // isActive (Bennett default)
                            currentTime
                        )
                    )
                    
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Inserted default coach: $name")
                    }
                }
            }
            
            db.setTransactionSuccessful()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting default coach personalities", e)
        } finally {
            db.endTransaction()
        }
    }
}

/**
 * Performance-aware database query executor for debug builds
 */
object DatabasePerformanceMonitor {
    
    private const val TAG = "DatabasePerf"
    
    /**
     * Execute a query with performance monitoring (debug builds only)
     */
    internal inline fun <T> executeWithMonitoring(
        operationType: String,
        tableName: String,
        query: String? = null,
        crossinline operation: () -> T
    ): T {
        return if (BuildConfig.DEBUG) {
            var result: T
            val executionTime = measureTimeMillis {
                result = operation()
            }
            
            // Import PerformanceConfig only in debug builds to avoid class loading issues
            try {
                val performanceConfigClass = Class.forName("com.runningcoach.v2.config.PerformanceConfig")
                val logMethod = performanceConfigClass.getMethod(
                    "logDatabaseOperation", 
                    String::class.java, 
                    String::class.java, 
                    String::class.java, 
                    Long::class.java
                )
                logMethod.invoke(null, operationType, tableName, query, executionTime)
            } catch (e: Exception) {
                // Fallback logging if PerformanceConfig is not available
                Log.d(TAG, "$operationType on $tableName: ${executionTime}ms")
            }
            
            result
        } else {
            operation()
        }
    }
    
    /**
     * Execute a transaction with performance monitoring (debug builds only)
     */
    internal inline fun <T> executeTransactionWithMonitoring(
        operationName: String,
        crossinline operation: () -> T
    ): T {
        return if (BuildConfig.DEBUG) {
            var result: T
            val executionTime = measureTimeMillis {
                result = operation()
            }
            
            try {
                val performanceConfigClass = Class.forName("com.runningcoach.v2.config.PerformanceConfig")
                val logMethod = performanceConfigClass.getMethod(
                    "logTransaction",
                    String::class.java,
                    Long::class.java
                )
                logMethod.invoke(null, operationName, executionTime)
            } catch (e: Exception) {
                // Fallback logging if PerformanceConfig is not available
                Log.d(TAG, "Transaction $operationName: ${executionTime}ms")
            }
            
            result
        } else {
            operation()
        }
    }
}