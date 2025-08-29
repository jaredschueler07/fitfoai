package com.runningcoach.v2.data.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataUpdateRequest
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import com.google.android.gms.tasks.Tasks
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * GoogleFitManager - Complete reimagined Google Fit integration
 * 
 * Core Principles:
 * 1. Single source of truth for Google Fit operations
 * 2. Automatic background syncing with WorkManager
 * 3. Robust error handling and retry logic
 * 4. Offline-first with queue for pending writes
 * 5. Clean permission flow with proper state management
 */
class GoogleFitManager private constructor(
    private val context: Context,
    private val database: FITFOAIDatabase
) {
    companion object {
        private const val TAG = "GoogleFitManager"
        const val REQUEST_CODE_SIGN_IN = 1001
        const val REQUEST_CODE_PERMISSIONS = 1002
        private const val SYNC_WORK_TAG = "google_fit_sync"
        private const val SYNC_INTERVAL_HOURS = 4L
        
        @Volatile
        private var INSTANCE: GoogleFitManager? = null
        
        fun getInstance(context: Context): GoogleFitManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleFitManager(
                    context.applicationContext,
                    FITFOAIDatabase.getDatabase(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }
    
    // DAOs
    private val userDao = database.userDao()
    private val googleFitDao = database.googleFitDailySummaryDao()
    private val connectedAppDao = database.connectedAppDao()
    private val runSessionDao = database.runSessionDao()
    
    // Google Fit Configuration
    private val fitnessOptions = FitnessOptions.builder()
        // Read permissions for comprehensive data
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
        // Aggregate types for efficient queries
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_SPEED_SUMMARY, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
        // Write permissions for run tracking
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
        // Session permissions for run tracking
        .accessSessionsRead()
        .accessSessionsWrite()
        .build()
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .addExtension(fitnessOptions)
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    // Connection State Management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    // Error handling
    private val _lastError = MutableStateFlow<GoogleFitError?>(null)
    val lastError: StateFlow<GoogleFitError?> = _lastError.asStateFlow()
    
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
        ERROR
    }
    
    enum class SyncState {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR
    }
    
    data class GoogleFitError(
        val code: ErrorCode,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class ErrorCode {
            SIGN_IN_FAILED,
            PERMISSION_DENIED,
            NETWORK_ERROR,
            API_ERROR,
            DATA_NOT_AVAILABLE,
            UNKNOWN
        }
    }
    
    // ========== CONNECTION MANAGEMENT ==========
    
    /**
     * Initiate Google Fit connection with unified flow
     */
    fun connect(activity: Activity) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                _lastError.value = null
                
                val account = getSignedInAccount()
                
                if (account == null) {
                    // Need to sign in first
                    val signInIntent = googleSignInClient.signInIntent
                    activity.startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
                } else if (!hasPermissions(account)) {
                    // Signed in but need permissions
                    _connectionState.value = ConnectionState.AWAITING_PERMISSIONS
                    requestPermissions(activity, account)
                } else {
                    // Already fully connected
                    _connectionState.value = ConnectionState.CONNECTED
                    onConnectionSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating connection", e)
                _connectionState.value = ConnectionState.ERROR
                _lastError.value = GoogleFitError(
                    GoogleFitError.ErrorCode.UNKNOWN,
                    e.message ?: "Connection failed"
                )
            }
        }
    }
    
    /**
     * Handle activity results from sign-in or permission requests
     */
    fun handleActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            when (requestCode) {
                REQUEST_CODE_SIGN_IN -> handleSignInResult(activity, resultCode, data)
                REQUEST_CODE_PERMISSIONS -> handlePermissionResult(resultCode)
            }
        }
    }
    
    private suspend fun handleSignInResult(activity: Activity, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
                
                if (!hasPermissions(account)) {
                    _connectionState.value = ConnectionState.AWAITING_PERMISSIONS
                    requestPermissions(activity, account)
                } else {
                    _connectionState.value = ConnectionState.CONNECTED
                    onConnectionSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed", e)
                _connectionState.value = ConnectionState.ERROR
                _lastError.value = GoogleFitError(
                    GoogleFitError.ErrorCode.SIGN_IN_FAILED,
                    "Google Sign-In failed: ${e.message}"
                )
            }
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
            _lastError.value = GoogleFitError(
                GoogleFitError.ErrorCode.SIGN_IN_FAILED,
                "User cancelled sign-in"
            )
        }
    }
    
    private fun handlePermissionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            _connectionState.value = ConnectionState.CONNECTED
            onConnectionSuccess()
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
            _lastError.value = GoogleFitError(
                GoogleFitError.ErrorCode.PERMISSION_DENIED,
                "Fitness permissions denied"
            )
        }
    }
    
    private fun requestPermissions(activity: Activity, account: GoogleSignInAccount) {
        GoogleSignIn.requestPermissions(
            activity,
            REQUEST_CODE_PERMISSIONS,
            account,
            fitnessOptions
        )
    }
    
    private fun onConnectionSuccess() {
        CoroutineScope(Dispatchers.IO).launch {
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
                
                Log.i(TAG, "Google Fit connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error in onConnectionSuccess", e)
            }
        }
    }
    
    /**
     * Disconnect from Google Fit
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                // Cancel periodic sync
                WorkManager.getInstance(context)
                    .cancelAllWorkByTag(SYNC_WORK_TAG)
                
                // Sign out
                googleSignInClient.signOut().await()
                
                // Update database
                val currentUser = userDao.getCurrentUser().first()
                currentUser?.let { user ->
                    updateConnectionStatus(user.id, false)
                }
                
                _connectionState.value = ConnectionState.DISCONNECTED
                _lastSyncTime.value = null
                
                Log.i(TAG, "Disconnected from Google Fit")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting", e)
            }
        }
    }
    
    // ========== DATA SYNC OPERATIONS ==========
    
    /**
     * Perform a full sync of fitness data
     */
    suspend fun performFullSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.SYNCING
            
            val account = getSignedInAccount()
            if (account == null || !hasPermissions(account)) {
                _syncState.value = SyncState.ERROR
                return@withContext Result.failure(Exception("Not connected to Google Fit"))
            }
            
            val currentUser = userDao.getCurrentUser().first()
                ?: return@withContext Result.failure(Exception("No current user"))
            
            // Sync different data types in parallel
            coroutineScope {
                val syncJobs = listOf(
                    async { syncDailyData(account, currentUser.id) },
                    async { syncWeeklyData(account, currentUser.id) },
                    async { syncUserProfile(account, currentUser.id) },
                    async { syncRecentRuns(account, currentUser.id) }
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
            _lastError.value = GoogleFitError(
                GoogleFitError.ErrorCode.API_ERROR,
                "Sync failed: ${e.message}"
            )
            Result.failure(e)
        }
    }
    
    private suspend fun syncDailyData(account: GoogleSignInAccount, userId: Long) {
        try {
            val today = LocalDate.now()
            val startTime = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val endTime = System.currentTimeMillis()
            
            // Build comprehensive read request
            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_HEART_POINTS)
                .aggregate(DataType.TYPE_MOVE_MINUTES)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()
            
            val response = Tasks.await(
                Fitness.getHistoryClient(context, account).readData(readRequest)
            )
            
            // Process and store data
            for (bucket in response.buckets) {
                var steps = 0
                var distance = 0f
                var calories = 0
                var heartPoints = 0
                var moveMinutes = 0
                
                for (dataSet in bucket.dataSets) {
                    when (dataSet.dataType) {
                        DataType.AGGREGATE_STEP_COUNT_DELTA -> {
                            dataSet.dataPoints.forEach { dp ->
                                steps += dp.getValue(Field.FIELD_STEPS).asInt()
                            }
                        }
                        DataType.AGGREGATE_DISTANCE_DELTA -> {
                            dataSet.dataPoints.forEach { dp ->
                                distance += dp.getValue(Field.FIELD_DISTANCE).asFloat()
                            }
                        }
                        DataType.AGGREGATE_CALORIES_EXPENDED -> {
                            dataSet.dataPoints.forEach { dp ->
                                calories += dp.getValue(Field.FIELD_CALORIES).asFloat().toInt()
                            }
                        }
                        DataType.AGGREGATE_HEART_POINTS -> {
                            dataSet.dataPoints.forEach { dp ->
                                heartPoints += dp.getValue(Field.FIELD_INTENSITY).asFloat().toInt()
                            }
                        }
                        DataType.AGGREGATE_MOVE_MINUTES -> {
                            dataSet.dataPoints.forEach { dp ->
                                moveMinutes += dp.getValue(Field.FIELD_DURATION).asInt()
                            }
                        }
                    }
                }
                
                // Store in database
                val summary = GoogleFitDailySummaryEntity(
                    userId = userId,
                    date = startTime,
                    steps = steps,
                    distance = distance,
                    calories = calories,
                    activeMinutes = moveMinutes,
                    heartPoints = heartPoints,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                googleFitDao.insertOrUpdateDailySummary(summary)
                
                Log.d(TAG, "Synced daily data: steps=$steps, distance=$distance, calories=$calories")
            }
            
            // Also get heart rate data separately
            syncHeartRateData(account, userId, startTime, endTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing daily data", e)
            throw e
        }
    }
    
    private suspend fun syncWeeklyData(account: GoogleSignInAccount, userId: Long) {
        try {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(7)
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val endTime = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
            
            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()
            
            val response = Tasks.await(
                Fitness.getHistoryClient(context, account).readData(readRequest)
            )
            
            // Process each day
            for (bucket in response.buckets) {
                val bucketStart = bucket.getStartTime(TimeUnit.MILLISECONDS)
                var steps = 0
                var distance = 0f
                var calories = 0
                
                for (dataSet in bucket.dataSets) {
                    when (dataSet.dataType) {
                        DataType.AGGREGATE_STEP_COUNT_DELTA -> {
                            dataSet.dataPoints.forEach { dp ->
                                steps += dp.getValue(Field.FIELD_STEPS).asInt()
                            }
                        }
                        DataType.AGGREGATE_DISTANCE_DELTA -> {
                            dataSet.dataPoints.forEach { dp ->
                                distance += dp.getValue(Field.FIELD_DISTANCE).asFloat()
                            }
                        }
                        DataType.AGGREGATE_CALORIES_EXPENDED -> {
                            dataSet.dataPoints.forEach { dp ->
                                calories += dp.getValue(Field.FIELD_CALORIES).asFloat().toInt()
                            }
                        }
                    }
                }
                
                // Store each day's summary
                val summary = GoogleFitDailySummaryEntity(
                    userId = userId,
                    date = bucketStart,
                    steps = steps,
                    distance = distance,
                    calories = calories,
                    activeMinutes = 0, // Will be updated separately if needed
                    heartPoints = 0,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                googleFitDao.insertOrUpdateDailySummary(summary)
            }
            
            Log.i(TAG, "Synced weekly data for 7 days")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing weekly data", e)
            throw e
        }
    }
    
    private suspend fun syncHeartRateData(
        account: GoogleSignInAccount,
        userId: Long,
        startTime: Long,
        endTime: Long
    ) {
        try {
            val readRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()
            
            val response = Tasks.await(
                Fitness.getHistoryClient(context, account).readData(readRequest)
            )
            
            var totalBpm = 0f
            var count = 0
            var maxBpm = 0f
            var minBpm = Float.MAX_VALUE
            
            for (dataSet in response.dataSets) {
                for (dataPoint in dataSet.dataPoints) {
                    val bpm = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                    totalBpm += bpm
                    count++
                    maxBpm = maxOf(maxBpm, bpm)
                    minBpm = minOf(minBpm, bpm)
                }
            }
            
            if (count > 0) {
                val avgHeartRate = totalBpm / count
                
                // Update today's summary with heart rate data
                val todaySummary = googleFitDao.getDailySummaryForDate(userId, startTime)
                todaySummary?.let { summary ->
                    val updated = summary.copy(
                        avgHeartRate = avgHeartRate,
                        maxHeartRate = maxBpm,
                        minHeartRate = if (minBpm == Float.MAX_VALUE) null else minBpm
                    )
                    googleFitDao.insertOrUpdateDailySummary(updated)
                }
                
                Log.d(TAG, "Synced heart rate data: avg=$avgHeartRate, max=$maxBpm, min=$minBpm")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "No heart rate data available", e)
            // Heart rate data may not be available, don't throw
        }
    }
    
    private suspend fun syncUserProfile(account: GoogleSignInAccount, userId: Long) {
        try {
            // Get latest weight
            val weightRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_WEIGHT)
                .setTimeRange(
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30),
                    System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
                )
                .setLimit(1)
                .build()
            
            val weightResponse = Tasks.await(
                Fitness.getHistoryClient(context, account).readData(weightRequest)
            )
            
            var latestWeight: Float? = null
            for (dataSet in weightResponse.dataSets) {
                dataSet.dataPoints.lastOrNull()?.let { dp ->
                    latestWeight = dp.getValue(Field.FIELD_WEIGHT).asFloat()
                }
            }
            
            // Get latest height
            val heightRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_HEIGHT)
                .setTimeRange(
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365),
                    System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
                )
                .setLimit(1)
                .build()
            
            val heightResponse = Tasks.await(
                Fitness.getHistoryClient(context, account).readData(heightRequest)
            )
            
            var latestHeight: Float? = null
            for (dataSet in heightResponse.dataSets) {
                dataSet.dataPoints.lastOrNull()?.let { dp ->
                    latestHeight = dp.getValue(Field.FIELD_HEIGHT).asFloat()
                }
            }
            
            // Update user profile if we have new data
            if (latestWeight != null || latestHeight != null) {
                val user = userDao.getUserById(userId)
                user?.let {
                    val updated = it.copy(
                        weight = latestWeight?.let { w -> (w * 2.20462).toFloat() } ?: it.weight, // Convert kg to lbs
                        height = latestHeight?.let { h -> (h * 39.3701).toInt() } ?: it.height // Convert meters to inches
                    )
                    userDao.updateUser(updated)
                }
                
                Log.d(TAG, "Updated user profile: weight=$latestWeight kg, height=$latestHeight m")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing user profile", e)
            // Profile data may not be available, don't throw
        }
    }
    
    private suspend fun syncRecentRuns(account: GoogleSignInAccount, userId: Long) {
        try {
            // Get sessions from the last 30 days
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(30)
            
            val sessionRequest = SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .read(DataType.TYPE_DISTANCE_DELTA)
                .read(DataType.TYPE_SPEED)
                .readSessionsFromAllApps()
                .build()
            
            val sessionResponse = Tasks.await(
                Fitness.getSessionsClient(context, account).readSession(sessionRequest)
            )
            
            for (session in sessionResponse.sessions) {
                // Check if this is a running session
                val activityType = session.activity
                if (activityType == FitnessActivities.RUNNING ||
                    activityType == FitnessActivities.RUNNING_JOGGING ||
                    activityType == FitnessActivities.RUNNING_TREADMILL) {
                    
                    // Extract session data
                    val startTimeMillis = session.getStartTime(TimeUnit.MILLISECONDS)
                    val endTimeMillis = session.getEndTime(TimeUnit.MILLISECONDS)
                    val durationMillis = endTimeMillis - startTimeMillis
                    
                    // Get distance and speed data for this session
                    var totalDistance = 0f
                    var avgSpeed = 0f
                    var speedCount = 0
                    
                    sessionResponse.getDataSet(session)?.forEach { dataSet ->
                        when (dataSet.dataType) {
                            DataType.TYPE_DISTANCE_DELTA -> {
                                dataSet.dataPoints.forEach { dp ->
                                    totalDistance += dp.getValue(Field.FIELD_DISTANCE).asFloat()
                                }
                            }
                            DataType.TYPE_SPEED -> {
                                dataSet.dataPoints.forEach { dp ->
                                    avgSpeed += dp.getValue(Field.FIELD_SPEED).asFloat()
                                    speedCount++
                                }
                            }
                        }
                    }
                    
                    if (speedCount > 0) {
                        avgSpeed /= speedCount
                    }
                    
                    // Check if we already have this session
                    val existingSession = runSessionDao.getSessionByGoogleFitId(session.identifier)
                    if (existingSession == null && totalDistance > 0) {
                        // Create new run session
                        val runSession = RunSessionEntity(
                            userId = userId,
                            googleFitSessionId = session.identifier,
                            startTime = startTimeMillis,
                            endTime = endTimeMillis,
                            duration = durationMillis,
                            distance = totalDistance,
                            avgSpeed = avgSpeed,
                            avgPace = if (avgSpeed > 0) 1000 / avgSpeed else null, // min/km
                            calories = null, // Will be updated separately
                            route = null,
                            notes = session.description,
                            syncedWithGoogleFit = true,
                            createdAt = System.currentTimeMillis()
                        )
                        
                        runSessionDao.insertRunSession(runSession)
                        Log.d(TAG, "Synced run session: ${session.name}, distance=$totalDistance m")
                    }
                }
            }
            
            Log.i(TAG, "Synced recent run sessions")
            
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing run sessions", e)
            // Run sessions may not be available, don't throw
        }
    }
    
    // ========== WRITE OPERATIONS ==========
    
    /**
     * Write a run session to Google Fit
     */
    suspend fun writeRunSession(runSession: RunSessionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
            if (account == null || !hasPermissions(account)) {
                return@withContext Result.failure(Exception("Not connected to Google Fit"))
            }
            
            // Create data source
            val dataSource = DataSource.Builder()
                .setAppPackageName(context)
                .setDataType(DataType.TYPE_DISTANCE_DELTA)
                .setStreamName("FITFOAI - Run Distance")
                .setType(DataSource.TYPE_RAW)
                .build()
            
            // Create distance data set
            val distanceDataSet = DataSet.builder(dataSource)
                .add(
                    DataPoint.builder(dataSource)
                        .setTimeInterval(
                            runSession.startTime,
                            runSession.endTime ?: System.currentTimeMillis(),
                            TimeUnit.MILLISECONDS
                        )
                        .setField(Field.FIELD_DISTANCE, runSession.distance)
                        .build()
                )
                .build()
            
            // Create session
            val session = Session.Builder()
                .setName("FITFOAI Run")
                .setDescription("Run tracked by FITFOAI")
                .setIdentifier("fitfoai_run_${runSession.id}")
                .setActivity(FitnessActivities.RUNNING)
                .setStartTime(runSession.startTime, TimeUnit.MILLISECONDS)
                .setEndTime(
                    runSession.endTime ?: System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            // Insert session with data
            val insertRequest = SessionInsertRequest.Builder()
                .setSession(session)
                .addDataSet(distanceDataSet)
                .build()
            
            Tasks.await(
                Fitness.getSessionsClient(context, account).insertSession(insertRequest)
            )
            
            // Update local session to mark as synced
            runSessionDao.updateGoogleFitSync(runSession.id, true, session.identifier)
            
            Log.i(TAG, "Successfully wrote run session to Google Fit")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error writing run session to Google Fit", e)
            Result.failure(e)
        }
    }
    
    // ========== BACKGROUND SYNC ==========
    
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<GoogleFitSyncWorker>(
            SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(SYNC_WORK_TAG)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "google_fit_periodic_sync",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWorkRequest
            )
        
        Log.i(TAG, "Scheduled periodic sync every $SYNC_INTERVAL_HOURS hours")
    }
    
    // ========== HELPER FUNCTIONS ==========
    
    private fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    private fun hasPermissions(account: GoogleSignInAccount): Boolean {
        return GoogleSignIn.hasPermissions(account, fitnessOptions)
    }
    
    private fun checkConnectionState() {
        val account = getSignedInAccount()
        _connectionState.value = when {
            account == null -> ConnectionState.DISCONNECTED
            !hasPermissions(account) -> ConnectionState.AWAITING_PERMISSIONS
            else -> ConnectionState.CONNECTED
        }
    }
    
    private suspend fun updateConnectionStatus(userId: Long, isConnected: Boolean) {
        val existingApp = connectedAppDao.getConnectedAppByType(userId, "GOOGLE_FIT")
        
        if (existingApp != null) {
            connectedAppDao.updateConnectionStatus(
                userId = userId,
                appType = "GOOGLE_FIT",
                isConnected = isConnected,
                lastSyncTime = if (isConnected) System.currentTimeMillis() else null
            )
        } else {
            connectedAppDao.insertConnectedApp(
                ConnectedAppEntity(
                    userId = userId,
                    appType = "GOOGLE_FIT",
                    appName = "Google Fit",
                    isConnected = isConnected,
                    lastSyncTime = if (isConnected) System.currentTimeMillis() else null
                )
            )
        }
    }
    
    // ========== DATA ACCESS ==========
    
    /**
     * Get fitness data flow for the current user
     */
    fun getFitnessDataFlow(): Flow<List<GoogleFitDailySummaryEntity>> = flow {
        val currentUser = userDao.getCurrentUser().first()
        if (currentUser != null) {
            emitAll(googleFitDao.getUserDailySummaries(currentUser.id))
        } else {
            emit(emptyList())
        }
    }
    
    /**
     * Get today's fitness data
     */
    suspend fun getTodaysFitnessData(): GoogleFitDailySummaryEntity? {
        val currentUser = userDao.getCurrentUser().first() ?: return null
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return googleFitDao.getDailySummaryForDate(currentUser.id, today)
    }
    
    /**
     * Get weekly fitness data
     */
    suspend fun getWeeklyFitnessData(): List<GoogleFitDailySummaryEntity> {
        val currentUser = userDao.getCurrentUser().first() ?: return emptyList()
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(7)
        val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val endTime = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        return googleFitDao.getDailySummariesForDateRange(currentUser.id, startTime, endTime)
    }
}

// Extension function for await on Tasks
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWith(Result.failure(it)) }
}