package com.runningcoach.v2.data.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GoogleFitService(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleFitService"
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow("Not connected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private var googleSignInClient: GoogleSignInClient? = null
    private var fitnessOptions: FitnessOptions? = null
    
    // Fitness data models
    data class FitnessData(
        val steps: Int = 0,
        val distance: Float = 0f, // in meters
        val calories: Int = 0,
        val activeMinutes: Int = 0,
        val heartRate: Float? = null,
        val weight: Float? = null, // in kg
        val height: Float? = null // in meters
    )
    
    data class UserProfileData(
        val name: String? = null,
        val email: String? = null,
        val weight: Float? = null, // in kg
        val height: Float? = null, // in meters
        val weightImperial: String? = null, // formatted as "150 lbs"
        val heightImperial: String? = null // formatted as "5'8\""
    )
    
    data class DailyStepsData(
        val date: String,
        val steps: Int
    )
    
    data class HeartRateData(
        val timestamp: Long,
        val bpm: Float
    )
    
    init {
        setupGoogleFit()
    }
    
    private fun setupGoogleFit() {
        // Build FitnessOptions with required data types
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .build()
        
        // Build GoogleSignInOptions
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .addExtension(fitnessOptions!!)
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        // Check if already signed in
        checkConnectionStatus()
    }
    
    fun initiateConnection(): Intent {
        return try {
            _connectionStatus.value = "Connecting to Google Fit..."
            Log.i(TAG, "Initiating Google Fit connection")
            
            // Ensure GoogleSignInClient is initialized
            if (googleSignInClient == null) {
                Log.e(TAG, "GoogleSignInClient is null, reinitializing...")
                setupGoogleFit()
            }
            
            // Get the current signed-in account
            val account = getCurrentAccount()
            Log.i(TAG, "Current account: ${account?.email ?: "null"}")
            
            if (account != null && hasPermissions(account)) {
                // Already connected
                _isConnected.value = true
                _connectionStatus.value = "Connected to Google Fit"
                Log.i(TAG, "Already connected to Google Fit")
                return Intent() // Return empty intent since we're already connected
            }
            
            // Request permissions
            val signInIntent = googleSignInClient?.signInIntent
            if (signInIntent == null) {
                Log.e(TAG, "SignIn intent is null")
                _connectionStatus.value = "Error: Unable to create sign-in intent"
                return Intent()
            }
            
            Log.i(TAG, "Returning sign-in intent")
            signInIntent
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating Google Fit connection", e)
            _connectionStatus.value = "Error: ${e.message}"
            _isConnected.value = false
            Intent()
        }
    }
    
    fun checkConnectionStatus() {
        try {
            Log.i(TAG, "Checking Google Fit connection status")
            val account = getCurrentAccount()
            val hasPermissions = account != null && hasPermissions(account)
            
            _isConnected.value = hasPermissions
            _connectionStatus.value = if (hasPermissions) "Connected to Google Fit" else "Not connected"
            
            Log.i(TAG, "Connection status: ${_connectionStatus.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connection status", e)
            _isConnected.value = false
            _connectionStatus.value = "Error checking connection"
        }
    }
    
    private fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    private fun hasPermissions(account: GoogleSignInAccount): Boolean {
        return try {
            val options = fitnessOptions ?: return false
            GoogleSignIn.hasPermissions(account, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }
    
    fun requestPermissions(activity: Activity) {
        val account = getCurrentAccount() ?: return
        val options = fitnessOptions ?: return
        
        if (!hasPermissions(account)) {
            GoogleSignIn.requestPermissions(
                activity,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                options
            )
        }
    }
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                _isConnected.value = true
                _connectionStatus.value = "Connected to Google Fit"
                Log.i(TAG, "Google Fit permissions granted")
            } else {
                _isConnected.value = false
                _connectionStatus.value = "Google Fit permissions denied"
                Log.w(TAG, "Google Fit permissions were NOT granted")
            }
        }
    }
    
    suspend fun handleGoogleSignInResult() {
        // This method is called after successful Google Sign-In to check Fitness API permissions
        try {
            val account = getCurrentAccount()
            if (account != null) {
                if (!hasPermissions(account)) {
                    requestFitnessPermissions(account)
                }
            }
            checkConnectionStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connection after Google Sign-In", e)
            _isConnected.value = false
            _connectionStatus.value = "Error checking connection"
        }
    }

    private suspend fun requestFitnessPermissions(account: GoogleSignInAccount): Boolean = suspendCoroutine { continuation ->
        val fitnessOptions = fitnessOptions ?: return@suspendCoroutine continuation.resume(false)
        Fitness.getConfigClient(context, account)
            .readDataType(DataType.TYPE_STEP_COUNT_DELTA.name)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }
    
    suspend fun getDailySteps(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for daily steps")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                // Get steps for today
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startTime = cal.timeInMillis
                val endTime = System.currentTimeMillis()

                val readRequest = DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()

                val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                var totalSteps = 0
                
                for (bucket in response.buckets) {
                    val dataSets = bucket.dataSets
                    for (dataSet in dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            val steps = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                            totalSteps += steps
                            Log.d(TAG, "Steps data point: $steps")
                        }
                    }
                }

                Log.i(TAG, "Retrieved daily steps from Google Fit: $totalSteps")
                Result.success(totalSteps)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading daily steps from Google Fit", e)
                // Fallback to direct data read if aggregation fails
                try {
                    val account = getCurrentAccount() ?: return@withContext Result.failure(e)
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val startTime = cal.timeInMillis
                    val endTime = System.currentTimeMillis()

                    val readRequest = DataReadRequest.Builder()
                        .read(DataType.TYPE_STEP_COUNT_DELTA)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build()

                    val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                    var totalSteps = 0
                    
                    for (dataSet in response.dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            val steps = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                            totalSteps += steps
                        }
                    }

                    Log.i(TAG, "Retrieved daily steps via fallback method: $totalSteps")
                    Result.success(totalSteps)
                    
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Fallback method also failed for daily steps", fallbackException)
                    Result.failure(fallbackException)
                }
            }
        }
    }
    
    suspend fun getWeeklySteps(): Result<List<DailyStepsData>> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for weekly steps")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                // Get steps for the last 7 days
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -6) // Go back 6 days to get 7 days total including today
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startTime = cal.timeInMillis
                
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                val endTime = endCal.timeInMillis

                val readRequest = DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()

                val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                val weeklyData = mutableListOf<DailyStepsData>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                for (bucket in response.buckets) {
                    var dailySteps = 0
                    val bucketStartTime = bucket.getStartTime(TimeUnit.MILLISECONDS)
                    val dateString = dateFormat.format(Date(bucketStartTime))
                    
                    for (dataSet in bucket.dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            val steps = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                            dailySteps += steps
                        }
                    }
                    
                    weeklyData.add(DailyStepsData(dateString, dailySteps))
                    Log.d(TAG, "Daily steps for $dateString: $dailySteps")
                }

                Log.i(TAG, "Retrieved weekly steps data: ${weeklyData.size} days")
                Result.success(weeklyData.sortedBy { it.date })
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading weekly steps from Google Fit", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getHeartRateData(hoursBack: Int = 24): Result<List<HeartRateData>> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for heart rate data")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                val endTime = System.currentTimeMillis()
                val startTime = endTime - TimeUnit.HOURS.toMillis(hoursBack.toLong())

                val readRequest = DataReadRequest.Builder()
                    .read(DataType.TYPE_HEART_RATE_BPM)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()

                val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                val heartRateData = mutableListOf<HeartRateData>()
                
                for (dataSet in response.dataSets) {
                    for (dataPoint in dataSet.dataPoints) {
                        val bpm = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                        val timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                        
                        heartRateData.add(HeartRateData(timestamp, bpm))
                        Log.d(TAG, "Heart rate data point: $bpm bpm at ${Date(timestamp)}")
                    }
                }

                Log.i(TAG, "Retrieved heart rate data: ${heartRateData.size} measurements")
                Result.success(heartRateData.sortedByDescending { it.timestamp })
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading heart rate data from Google Fit", e)
                // Return empty list instead of failure for heart rate as it's not always available
                Result.success(emptyList())
            }
        }
    }
    
    suspend fun getLatestWeight(): Result<Float?> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for weight data")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                // Get weight data from the last 30 days to find the most recent entry
                val endTime = System.currentTimeMillis()
                val startTime = endTime - TimeUnit.DAYS.toMillis(30)

                val readRequest = DataReadRequest.Builder()
                    .read(DataType.TYPE_WEIGHT)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()

                val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                var latestWeight: Float? = null
                var latestTimestamp = 0L
                
                for (dataSet in response.dataSets) {
                    for (dataPoint in dataSet.dataPoints) {
                        val weight = dataPoint.getValue(Field.FIELD_WEIGHT).asFloat()
                        val timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                        
                        if (timestamp > latestTimestamp) {
                            latestWeight = weight
                            latestTimestamp = timestamp
                            Log.d(TAG, "Found weight data: $weight kg at ${Date(timestamp)}")
                        }
                    }
                }

                if (latestWeight != null) {
                    Log.i(TAG, "Retrieved latest weight from Google Fit: $latestWeight kg")
                } else {
                    Log.i(TAG, "No weight data found in Google Fit")
                }
                
                Result.success(latestWeight)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading weight data from Google Fit", e)
                // Return null instead of failure for weight as it's optional data
                Result.success(null)
            }
        }
    }
    
    suspend fun getLatestHeight(): Result<Float?> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for height data")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                // Get height data from the last 365 days to find the most recent entry
                // Height doesn't change often, so we look back further
                val endTime = System.currentTimeMillis()
                val startTime = endTime - TimeUnit.DAYS.toMillis(365)

                val readRequest = DataReadRequest.Builder()
                    .read(DataType.TYPE_HEIGHT)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()

                val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                var latestHeight: Float? = null
                var latestTimestamp = 0L
                
                for (dataSet in response.dataSets) {
                    for (dataPoint in dataSet.dataPoints) {
                        val height = dataPoint.getValue(Field.FIELD_HEIGHT).asFloat()
                        val timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                        
                        if (timestamp > latestTimestamp) {
                            latestHeight = height
                            latestTimestamp = timestamp
                            Log.d(TAG, "Found height data: $height m at ${Date(timestamp)}")
                        }
                    }
                }

                if (latestHeight != null) {
                    Log.i(TAG, "Retrieved latest height from Google Fit: $latestHeight m")
                } else {
                    Log.i(TAG, "No height data found in Google Fit")
                }
                
                Result.success(latestHeight)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading height data from Google Fit", e)
                // Return null instead of failure for height as it's optional data
                Result.success(null)
            }
        }
    }
    
    suspend fun getUserProfileData(): Result<UserProfileData> {
        return try {
            Log.i(TAG, "Retrieving user profile data from Google Fit")
            
            val account = getCurrentAccount()
            if (account == null || !hasPermissions(account)) {
                Log.w(TAG, "No Google account or permissions available")
                return Result.failure(Exception("Google Fit not aconnected or no permissions"))
            }
            
            // Get user's basic info from Google account
            val name = account.displayName
            val email = account.email
            
            // Get fitness data
            val weightResult = getLatestWeight()
            val heightResult = getLatestHeight()
            
            val weight = weightResult.getOrNull()
            val height = heightResult.getOrNull()
            
            // Convert to imperial units
            val weightImperial = weight?.let { 
                val pounds = (it * 2.20462).toInt()
                "$pounds lbs"
            }
            
            val heightImperial = height?.let { heightMeters ->
                val totalInches = (heightMeters * 39.3701).toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                "$feet'$inches\""
            }
            
            val profileData = UserProfileData(
                name = name,
                email = email,
                weight = weight,
                height = height,
                weightImperial = weightImperial,
                heightImperial = heightImperial
            )
            
            Log.i(TAG, "Retrieved user profile: name=$name, weight=$weightImperial, height=$heightImperial")
            Result.success(profileData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile data", e)
            Result.failure(e)
        }
    }
    
    suspend fun getComprehensiveFitnessData(): Result<FitnessData> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for comprehensive fitness data")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                // Get data for today
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startTime = cal.timeInMillis
                val endTime = System.currentTimeMillis()

                // Get fitness data using dedicated methods with proper error handling
                val steps = getDailySteps().getOrNull() ?: 0
                val distance = getDailyDistance().getOrNull() ?: (steps * 0.7f) // Fallback to estimate
                val calories = getDailyCalories().getOrNull() ?: ((steps * 0.04f).toInt()) // Fallback to estimate

                // Get weight and height (use our existing methods)
                val weight = getLatestWeight().getOrNull()
                val height = getLatestHeight().getOrNull()
                
                // Get heart rate data for the last hour
                val heartRateData = getHeartRateData(1).getOrNull() ?: emptyList()
                val latestHeartRate = heartRateData.firstOrNull()?.bpm // Most recent first

                val fitnessData = FitnessData(
                    steps = steps,
                    distance = distance,
                    calories = calories,
                    activeMinutes = 0, // Would need separate calculation with activity recognition
                    heartRate = latestHeartRate,
                    weight = weight,
                    height = height
                )
                
                Log.i(TAG, "Retrieved comprehensive fitness data - Steps: $steps, Distance: ${distance}m, Calories: $calories")
                Result.success(fitnessData)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting comprehensive fitness data from Google Fit", e)
                Result.failure(e)
            }
        }
    }
    
    fun disconnect() {
        googleSignInClient?.signOut()
        _isConnected.value = false
        _connectionStatus.value = "Not connected"
        Log.i(TAG, "Disconnected from Google Fit")
    }
    
    fun testConnection(): Boolean {
        checkConnectionStatus()
        return _isConnected.value
    }
    
    // Helper method to get distance data separately
    suspend fun getDailyDistance(): Result<Float> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for distance data")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startTime = cal.timeInMillis
                val endTime = System.currentTimeMillis()

                val readRequest = DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()

                val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                var totalDistance = 0f
                
                for (bucket in response.buckets) {
                    for (dataSet in bucket.dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            totalDistance += dataPoint.getValue(Field.FIELD_DISTANCE).asFloat()
                        }
                    }
                }

                Log.i(TAG, "Retrieved daily distance from Google Fit: $totalDistance meters")
                Result.success(totalDistance)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading distance data from Google Fit", e)
                // Fallback calculation based on steps
                val stepsResult = getDailySteps()
                if (stepsResult.isSuccess) {
                    val estimatedDistance = (stepsResult.getOrNull() ?: 0) * 0.7f
                    Log.i(TAG, "Using estimated distance based on steps: $estimatedDistance meters")
                    Result.success(estimatedDistance)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    // Helper method to get calories data separately
    suspend fun getDailyCalories(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccount()
                if (account == null || !hasPermissions(account)) {
                    Log.w(TAG, "No Google account or permissions available for calories data")
                    return@withContext Result.failure(Exception("Google Fit not connected or no permissions"))
                }

                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startTime = cal.timeInMillis
                val endTime = System.currentTimeMillis()

                val readRequest = DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()

                val response = Tasks.await(Fitness.getHistoryClient(context, account).readData(readRequest))
                var totalCalories = 0
                
                for (bucket in response.buckets) {
                    for (dataSet in bucket.dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            totalCalories += dataPoint.getValue(Field.FIELD_CALORIES).asFloat().toInt()
                        }
                    }
                }

                Log.i(TAG, "Retrieved daily calories from Google Fit: $totalCalories")
                Result.success(totalCalories)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading calories data from Google Fit", e)
                // Fallback calculation based on steps
                val stepsResult = getDailySteps()
                if (stepsResult.isSuccess) {
                    val estimatedCalories = ((stepsResult.getOrNull() ?: 0) * 0.04f).toInt()
                    Log.i(TAG, "Using estimated calories based on steps: $estimatedCalories")
                    Result.success(estimatedCalories)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    // Enhanced logging method for debugging API responses
    private fun logDataPointDetails(dataPoint: DataPoint, dataTypeName: String) {
        val startTime = Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS))
        val endTime = Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS))
        Log.d(TAG, "$dataTypeName data point: Start=$startTime, End=$endTime, Values=${dataPoint.dataType.fields.map { field -> 
            "${field.name}: ${dataPoint.getValue(field)}" 
        }.joinToString(", ")}")
    }
}