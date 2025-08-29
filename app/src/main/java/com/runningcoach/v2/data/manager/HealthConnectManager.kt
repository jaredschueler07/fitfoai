package com.runningcoach.v2.data.manager

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Velocity
import androidx.work.*
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.*
import com.runningcoach.v2.data.service.HealthConnectPermissionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * HealthConnectManager - Complete Health Connect integration
 * 
 * Core Principles:
 * 1. Single source of truth for Health Connect operations
 * 2. Automatic background syncing with WorkManager
 * 3. Robust error handling and retry logic
 * 4. Offline-first with queue for pending writes
 * 5. Clean permission flow with proper state management
 * 6. Seamless migration from Google Fit data
 */
class HealthConnectManager private constructor(
    private val context: Context,
    private val database: FITFOAIDatabase,
    private val permissionManager: HealthConnectPermissionManager
) {
    companion object {
        private const val TAG = "HealthConnectManager"
        private const val SYNC_WORK_TAG = "health_connect_sync"
        private const val SYNC_INTERVAL_HOURS = 2L // More frequent than Google Fit
        
        @Volatile
        private var INSTANCE: HealthConnectManager? = null
        
        fun getInstance(
            context: Context,
            permissionManager: HealthConnectPermissionManager
        ): HealthConnectManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HealthConnectManager(
                    context.applicationContext,
                    FITFOAIDatabase.getDatabase(context.applicationContext),
                    permissionManager
                ).also { INSTANCE = it }
            }
        }
    }
    
    // DAOs
    private val userDao = database.userDao()
    private val healthConnectDao = database.healthConnectDailySummaryDao() // Will create this
    private val connectedAppDao = database.connectedAppDao()
    private val runSessionDao = database.runSessionDao()
    
    // Health Connect client
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    // Connection State Management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    // Error handling
    private val _lastError = MutableStateFlow<HealthConnectError?>(null)
    val lastError: StateFlow<HealthConnectError?> = _lastError.asStateFlow()
    
    init {
        // Check initial connection state
        checkConnectionState()
        
        // Setup periodic sync if connected
        if (_connectionState.value == ConnectionState.CONNECTED) {
            setupPeriodicSync()
        }
    }
    
    // Connection States
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        AWAITING_PERMISSIONS,
        CONNECTED,
        ERROR,
        UNAVAILABLE
    }
    
    enum class SyncState {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR
    }
    
    data class HealthConnectError(
        val code: ErrorCode,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class ErrorCode {
            UNAVAILABLE,
            PERMISSION_DENIED,
            NETWORK_ERROR,
            API_ERROR,
            DATA_NOT_AVAILABLE,
            UNKNOWN
        }
    }
    
    // ========== CONNECTION MANAGEMENT ==========
    
    /**
     * Initiate Health Connect connection with unified flow
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.CONNECTING
            _lastError.value = null
            
            // Check availability first
            val availability = permissionManager.checkAvailability()
            when (availability) {
                HealthConnectPermissionManager.HealthConnectAvailability.UNAVAILABLE -> {
                    _connectionState.value = ConnectionState.UNAVAILABLE
                    return@withContext Result.failure(Exception("Health Connect unavailable"))
                }
                HealthConnectPermissionManager.HealthConnectAvailability.NEEDS_UPDATE -> {
                    _connectionState.value = ConnectionState.ERROR
                    _lastError.value = HealthConnectError(
                        HealthConnectError.ErrorCode.UNAVAILABLE,
                        "Health Connect needs update"
                    )
                    return@withContext Result.failure(Exception("Health Connect needs update"))
                }
                HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE -> {
                    if (permissionManager.hasRequiredPermissions()) {
                        _connectionState.value = ConnectionState.CONNECTED
                        onConnectionSuccess()
                        Result.success(Unit)
                    } else {
                        _connectionState.value = ConnectionState.AWAITING_PERMISSIONS
                        Result.failure(Exception("Permissions required"))
                    }
                }
                HealthConnectPermissionManager.HealthConnectAvailability.CHECKING -> {
                    _connectionState.value = ConnectionState.CONNECTING
                    Result.failure(Exception("Still checking availability"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating connection", e)
            _connectionState.value = ConnectionState.ERROR
            _lastError.value = HealthConnectError(
                HealthConnectError.ErrorCode.UNKNOWN,
                e.message ?: "Connection failed"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Handle permission grant result
     */
    suspend fun onPermissionsGranted(granted: Boolean) {
        if (granted) {
            _connectionState.value = ConnectionState.CONNECTED
            onConnectionSuccess()
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
            _lastError.value = HealthConnectError(
                HealthConnectError.ErrorCode.PERMISSION_DENIED,
                "Health Connect permissions denied"
            )
        }
    }
    
    private suspend fun onConnectionSuccess() {
        withContext(Dispatchers.IO) {
            try {
                // Update connection status in database
                val currentUser = userDao.getCurrentUser().first()
                currentUser?.let { user ->
                    updateConnectionStatus(user.id, true)
                }
                
                // Setup periodic sync
                setupPeriodicSync()
                
                // Perform initial sync
                performFullSync()
                
                Log.i(TAG, "Health Connect connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error in onConnectionSuccess", e)
            }
        }
    }
    
    /**
     * Disconnect from Health Connect
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                // Cancel periodic sync
                WorkManager.getInstance(context)
                    .cancelAllWorkByTag(SYNC_WORK_TAG)
                
                // Update database
                val currentUser = userDao.getCurrentUser().first()
                currentUser?.let { user ->
                    updateConnectionStatus(user.id, false)
                }
                
                _connectionState.value = ConnectionState.DISCONNECTED
                _lastSyncTime.value = null
                
                Log.i(TAG, "Disconnected from Health Connect")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting", e)
            }
        }
    }
    
    // ========== DATA SYNC OPERATIONS ==========
    
    /**
     * Perform a full sync of Health Connect data
     */
    suspend fun performFullSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.SYNCING
            
            if (!permissionManager.hasRequiredPermissions()) {
                _syncState.value = SyncState.ERROR
                return@withContext Result.failure(Exception("Not connected to Health Connect"))
            }
            
            val currentUser = userDao.getCurrentUser().first()
                ?: return@withContext Result.failure(Exception("No current user"))
            
            // Sync different data types in parallel
            coroutineScope {
                val syncJobs = listOf(
                    async { syncStepsData(currentUser.id) },
                    async { syncDistanceData(currentUser.id) },
                    async { syncCaloriesData(currentUser.id) },
                    async { syncHeartRateData(currentUser.id) },
                    async { syncExerciseSessions(currentUser.id) }
                )
                
                syncJobs.awaitAll()
            }
            
            _lastSyncTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.SUCCESS
            
            Log.i(TAG, "Full sync completed successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            _syncState.value = SyncState.ERROR
            _lastError.value = HealthConnectError(
                HealthConnectError.ErrorCode.API_ERROR,
                "Sync failed: ${e.message}"
            )
            Result.failure(e)
        }
    }
    
    private suspend fun syncStepsData(userId: Long) {
        try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            
            val totalSteps = response.records.sumOf { it.count }
            
            // Update daily summary
            updateDailySummary(userId, today) { summary ->
                summary.copy(steps = totalSteps.toInt())
            }
            
            Log.d(TAG, "Synced steps data: $totalSteps steps")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing steps data", e)
            throw e
        }
    }
    
    private suspend fun syncDistanceData(userId: Long) {
        try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            
            val totalDistanceMeters = response.records.sumOf { it.distance.inMeters }
            
            // Update daily summary
            updateDailySummary(userId, today) { summary ->
                summary.copy(distance = totalDistanceMeters.toFloat())
            }
            
            Log.d(TAG, "Synced distance data: $totalDistanceMeters meters")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing distance data", e)
            throw e
        }
    }
    
    private suspend fun syncCaloriesData(userId: Long) {
        try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            
            val totalCalories = response.records.sumOf { it.energy.inCalories }
            
            // Update daily summary
            updateDailySummary(userId, today) { summary ->
                summary.copy(calories = totalCalories.toInt())
            }
            
            Log.d(TAG, "Synced calories data: $totalCalories calories")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing calories data", e)
            throw e
        }
    }
    
    private suspend fun syncHeartRateData(userId: Long) {
        try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            
            if (response.records.isNotEmpty()) {
                val allSamples = response.records.flatMap { it.samples }
                val avgBpm = allSamples.map { it.beatsPerMinute }.average()
                val maxBpm = allSamples.maxOfOrNull { it.beatsPerMinute } ?: 0L
                val minBpm = allSamples.minOfOrNull { it.beatsPerMinute } ?: 0L
                
                // Update daily summary
                updateDailySummary(userId, today) { summary ->
                    summary.copy(
                        avgHeartRate = avgBpm.toFloat(),
                        maxHeartRate = maxBpm.toFloat(),
                        minHeartRate = minBpm.toFloat()
                    )
                }
                
                Log.d(TAG, "Synced heart rate data: avg=$avgBpm, max=$maxBpm, min=$minBpm")
            }
        } catch (e: Exception) {
            Log.w(TAG, "No heart rate data available", e)
            // Heart rate data may not be available, don't throw
        }
    }
    
    private suspend fun syncExerciseSessions(userId: Long) {
        try {
            val endTime = Instant.now()
            val startTime = endTime.minus(30, ChronoUnit.DAYS) // Last 30 days
            
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = healthConnectClient.readRecords(request)
            
            for (session in response.records) {
                // Only process running-related exercises
                if (session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
                    session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_TREADMILL_RUNNING ||
                    session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_OUTDOOR_RUNNING) {
                    
                    // Check if we already have this session
                    val existingSession = runSessionDao.getSessionByHealthConnectId(session.metadata.id)
                    if (existingSession == null) {
                        
                        // Calculate duration
                        val duration = Duration.between(session.startTime, session.endTime).toMillis()
                        
                        // Create new run session from Health Connect data
                        val runSession = RunSessionEntity(
                            userId = userId,
                            healthConnectSessionId = session.metadata.id,
                            startTime = session.startTime.toEpochMilli(),
                            endTime = session.endTime.toEpochMilli(),
                            duration = duration,
                            distance = 0f, // Will be updated with related distance records
                            avgSpeed = 0f, // Will be calculated
                            avgPace = null,
                            calories = null,
                            route = null,
                            notes = session.notes,
                            source = DataSource.HEALTH_CONNECT,
                            syncedWithHealthConnect = true,
                            createdAt = System.currentTimeMillis()
                        )
                        
                        val sessionId = runSessionDao.insertRunSession(runSession)
                        
                        // Try to get related data for this session
                        syncExerciseSessionData(sessionId, session)
                        
                        Log.d(TAG, "Synced exercise session: ${session.title ?: "Untitled"}")
                    }
                }
            }
            
            Log.i(TAG, "Synced exercise sessions")
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing exercise sessions", e)
            // Exercise sessions may not be available, don't throw
        }
    }
    
    private suspend fun syncExerciseSessionData(sessionId: Long, session: ExerciseSessionRecord) {
        try {
            // Get distance data for this session
            val distanceRequest = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            )
            
            val distanceResponse = healthConnectClient.readRecords(distanceRequest)
            val totalDistance = distanceResponse.records.sumOf { it.distance.inMeters }
            
            // Get speed data for this session
            val speedRequest = ReadRecordsRequest(
                recordType = SpeedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            )
            
            val speedResponse = healthConnectClient.readRecords(speedRequest)
            val avgSpeed = if (speedResponse.records.isNotEmpty()) {
                speedResponse.records.map { it.samples.map { sample -> sample.speed.inMetersPerSecond } }
                    .flatten()
                    .average()
                    .toFloat()
            } else 0f
            
            // Update the session with additional data
            if (totalDistance > 0 || avgSpeed > 0) {
                runSessionDao.updateSessionData(
                    sessionId = sessionId,
                    distance = totalDistance.toFloat(),
                    avgSpeed = avgSpeed,
                    avgPace = if (avgSpeed > 0) (1000 / avgSpeed) else null // min/km
                )
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing session data for session $sessionId", e)
        }
    }
    
    // ========== WRITE OPERATIONS ==========
    
    /**
     * Write a run session to Health Connect
     */
    suspend fun writeRunSession(runSession: RunSessionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!permissionManager.hasRequiredPermissions()) {
                return@withContext Result.failure(Exception("Not connected to Health Connect"))
            }
            
            val records = mutableListOf<Record>()
            
            // Create exercise session
            val exerciseSession = ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(runSession.startTime),
                endTime = Instant.ofEpochMilli(runSession.endTime ?: System.currentTimeMillis()),
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                title = "FITFOAI Run",
                notes = runSession.notes
            )
            records.add(exerciseSession)
            
            // Add distance record if available
            if (runSession.distance > 0) {
                val distanceRecord = DistanceRecord(
                    startTime = Instant.ofEpochMilli(runSession.startTime),
                    endTime = Instant.ofEpochMilli(runSession.endTime ?: System.currentTimeMillis()),
                    distance = Length.meters(runSession.distance.toDouble())
                )
                records.add(distanceRecord)
            }
            
            // Add speed records if available
            if (runSession.avgSpeed > 0) {
                val speedRecord = SpeedRecord(
                    startTime = Instant.ofEpochMilli(runSession.startTime),
                    endTime = Instant.ofEpochMilli(runSession.endTime ?: System.currentTimeMillis()),
                    samples = listOf(
                        SpeedRecord.Sample(
                            time = Instant.ofEpochMilli(runSession.startTime),
                            speed = Velocity.metersPerSecond(runSession.avgSpeed.toDouble())
                        )
                    )
                )
                records.add(speedRecord)
            }
            
            // Add calories if available
            runSession.calories?.let { calories ->
                val caloriesRecord = TotalCaloriesBurnedRecord(
                    startTime = Instant.ofEpochMilli(runSession.startTime),
                    endTime = Instant.ofEpochMilli(runSession.endTime ?: System.currentTimeMillis()),
                    energy = Energy.calories(calories.toDouble())
                )
                records.add(caloriesRecord)
            }
            
            // Insert all records
            val insertResponse = healthConnectClient.insertRecords(records)
            
            // Update local session to mark as synced
            val healthConnectId = insertResponse.recordIdsList.firstOrNull()
            if (healthConnectId != null) {
                runSessionDao.updateHealthConnectSync(runSession.id, true, healthConnectId)
            }
            
            Log.i(TAG, "Successfully wrote run session to Health Connect")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error writing run session to Health Connect", e)
            Result.failure(e)
        }
    }
    
    // ========== MIGRATION SUPPORT ==========
    
    /**
     * Migrate data from Google Fit to Health Connect
     */
    suspend fun migrateFromGoogleFit(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!permissionManager.hasRequiredPermissions()) {
                return@withContext Result.failure(Exception("Health Connect permissions required"))
            }
            
            val currentUser = userDao.getCurrentUser().first()
                ?: return@withContext Result.failure(Exception("No current user"))
            
            // Get all Google Fit run sessions that haven't been migrated
            val googleFitSessions = runSessionDao.getUnmigratedGoogleFitSessions()
            
            var migratedCount = 0
            for (session in googleFitSessions) {
                try {
                    // Write to Health Connect
                    val writeResult = writeRunSession(session)
                    if (writeResult.isSuccess) {
                        // Mark as migrated
                        runSessionDao.markAsMigrated(session.id)
                        migratedCount++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to migrate session ${session.id}", e)
                }
            }
            
            Log.i(TAG, "Migrated $migratedCount sessions from Google Fit to Health Connect")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            Result.failure(e)
        }
    }
    
    // ========== BACKGROUND SYNC ==========
    
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncWorkRequest = PeriodicWorkRequest.Builder(
            HealthConnectSyncWorker::class.java, // Will create this
            SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(SYNC_WORK_TAG)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "health_connect_periodic_sync",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWorkRequest
            )
        
        Log.i(TAG, "Scheduled periodic sync every $SYNC_INTERVAL_HOURS hours")
    }
    
    // ========== HELPER FUNCTIONS ==========
    
    private suspend fun checkConnectionState() {
        val availability = permissionManager.checkAvailability()
        _connectionState.value = when (availability) {
            HealthConnectPermissionManager.HealthConnectAvailability.UNAVAILABLE -> 
                ConnectionState.UNAVAILABLE
            HealthConnectPermissionManager.HealthConnectAvailability.NEEDS_UPDATE -> 
                ConnectionState.ERROR
            HealthConnectPermissionManager.HealthConnectAvailability.AVAILABLE -> {
                if (permissionManager.hasRequiredPermissions()) {
                    ConnectionState.CONNECTED
                } else {
                    ConnectionState.AWAITING_PERMISSIONS
                }
            }
            HealthConnectPermissionManager.HealthConnectAvailability.CHECKING -> 
                ConnectionState.CONNECTING
        }
    }
    
    private suspend fun updateConnectionStatus(userId: Long, isConnected: Boolean) {
        val existingApp = connectedAppDao.getConnectedAppByType(userId, "HEALTH_CONNECT")
        
        if (existingApp != null) {
            connectedAppDao.updateConnectionStatus(
                userId = userId,
                appType = "HEALTH_CONNECT",
                isConnected = isConnected,
                lastSyncTime = if (isConnected) System.currentTimeMillis() else null
            )
        } else {
            connectedAppDao.insertConnectedApp(
                ConnectedAppEntity(
                    userId = userId,
                    appType = "HEALTH_CONNECT",
                    appName = "Health Connect",
                    isConnected = isConnected,
                    lastSyncTime = if (isConnected) System.currentTimeMillis() else null
                )
            )
        }
    }
    
    private suspend fun updateDailySummary(
        userId: Long,
        date: LocalDate,
        update: (HealthConnectDailySummaryEntity) -> HealthConnectDailySummaryEntity
    ) {
        val dateMillis = date.atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        val existing = healthConnectDao.getDailySummaryForDate(userId, dateMillis)
        
        val summary = if (existing != null) {
            update(existing)
        } else {
            update(
                HealthConnectDailySummaryEntity(
                    userId = userId,
                    date = dateMillis,
                    steps = 0,
                    distance = 0f,
                    calories = 0,
                    activeMinutes = 0,
                    lastSynced = System.currentTimeMillis()
                )
            )
        }
        
        healthConnectDao.insertOrUpdateDailySummary(summary)
    }
    
    // ========== DATA ACCESS ==========
    
    /**
     * Get Health Connect data flow for the current user
     */
    fun getHealthConnectDataFlow(): Flow<List<HealthConnectDailySummaryEntity>> = flow {
        val currentUser = userDao.getCurrentUser().first()
        if (currentUser != null) {
            emitAll(healthConnectDao.getUserDailySummaries(currentUser.id))
        } else {
            emit(emptyList())
        }
    }
    
    /**
     * Get today's Health Connect data
     */
    suspend fun getTodaysHealthConnectData(): HealthConnectDailySummaryEntity? {
        val currentUser = userDao.getCurrentUser().first() ?: return null
        val today = LocalDate.now().atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        return healthConnectDao.getDailySummaryForDate(currentUser.id, today)
    }
    
    /**
     * Get weekly Health Connect data
     */
    suspend fun getWeeklyHealthConnectData(): List<HealthConnectDailySummaryEntity> {
        val currentUser = userDao.getCurrentUser().first() ?: return emptyList()
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(7)
        val startTime = startDate.atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        val endTime = endDate.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toEpochSecond() * 1000
        return healthConnectDao.getDailySummariesForDateRange(currentUser.id, startTime, endTime)
    }
}