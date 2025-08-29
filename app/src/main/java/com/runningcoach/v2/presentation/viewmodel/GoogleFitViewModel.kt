package com.runningcoach.v2.presentation.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.data.manager.GoogleFitManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Google Fit integration
 * Provides a clean interface between UI and GoogleFitManager
 */
class GoogleFitViewModel(
    private val googleFitManager: GoogleFitManager
) : ViewModel() {
    
    // Connection state
    val connectionState = googleFitManager.connectionState
    val syncState = googleFitManager.syncState
    val lastSyncTime = googleFitManager.lastSyncTime
    val lastError = googleFitManager.lastError
    
    // UI State
    data class GoogleFitUiState(
        val isConnected: Boolean = false,
        val isConnecting: Boolean = false,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
        val todaySteps: Int = 0,
        val todayDistance: Float = 0f,
        val todayCalories: Int = 0,
        val weeklyData: List<DayData> = emptyList(),
        val errorMessage: String? = null
    )
    
    data class DayData(
        val date: String,
        val steps: Int,
        val distance: Float,
        val calories: Int
    )
    
    private val _uiState = MutableStateFlow(GoogleFitUiState())
    val uiState: StateFlow<GoogleFitUiState> = _uiState.asStateFlow()
    
    init {
        // Observe connection state
        viewModelScope.launch {
            connectionState.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        isConnected = state == GoogleFitManager.ConnectionState.CONNECTED,
                        isConnecting = state == GoogleFitManager.ConnectionState.CONNECTING ||
                                      state == GoogleFitManager.ConnectionState.AWAITING_PERMISSIONS
                    )
                }
                
                // Auto-sync when connected
                if (state == GoogleFitManager.ConnectionState.CONNECTED) {
                    syncData()
                }
            }
        }
        
        // Observe sync state
        viewModelScope.launch {
            syncState.collect { state ->
                _uiState.update { current ->
                    current.copy(isSyncing = state == GoogleFitManager.SyncState.SYNCING)
                }
            }
        }
        
        // Observe errors
        viewModelScope.launch {
            lastError.collect { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error?.message)
                }
            }
        }
        
        // Load initial data if connected
        if (connectionState.value == GoogleFitManager.ConnectionState.CONNECTED) {
            loadFitnessData()
        }
    }
    
    /**
     * Initiate Google Fit connection
     */
    fun connect(activity: Activity) {
        googleFitManager.connect(activity)
    }
    
    /**
     * Handle activity result from Google Fit sign-in/permissions
     */
    fun handleActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        googleFitManager.handleActivityResult(activity, requestCode, resultCode, data)
    }
    
    /**
     * Disconnect from Google Fit
     */
    fun disconnect() {
        viewModelScope.launch {
            googleFitManager.disconnect()
            _uiState.update { current ->
                current.copy(
                    isConnected = false,
                    todaySteps = 0,
                    todayDistance = 0f,
                    todayCalories = 0,
                    weeklyData = emptyList()
                )
            }
        }
    }
    
    /**
     * Manually trigger data sync
     */
    fun syncData() {
        viewModelScope.launch {
            val result = googleFitManager.performFullSync()
            if (result.isSuccess) {
                loadFitnessData()
            }
        }
    }
    
    /**
     * Load fitness data from database
     */
    private fun loadFitnessData() {
        viewModelScope.launch {
            try {
                // Get today's data
                val todayData = googleFitManager.getTodaysFitnessData()
                todayData?.let { summary ->
                    _uiState.update { current ->
                        current.copy(
                            todaySteps = summary.steps ?: 0,
                            todayDistance = summary.distance ?: 0f,
                            todayCalories = summary.calories ?: 0
                        )
                    }
                }
                
                // Get weekly data
                val weeklyData = googleFitManager.getWeeklyFitnessData()
                val dayDataList = weeklyData.map { summary ->
                    DayData(
                        date = java.time.Instant.ofEpochMilli(summary.date)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .toString(),
                        steps = summary.steps ?: 0,
                        distance = summary.distance ?: 0f,
                        calories = summary.calories ?: 0
                    )
                }
                
                _uiState.update { current ->
                    current.copy(weeklyData = dayDataList)
                }
                
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(errorMessage = "Failed to load fitness data: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { current ->
            current.copy(errorMessage = null)
        }
    }
    
    /**
     * Check if we need to request permissions
     */
    fun isPermissionRequestNeeded(): Boolean {
        return connectionState.value == GoogleFitManager.ConnectionState.AWAITING_PERMISSIONS
    }
}