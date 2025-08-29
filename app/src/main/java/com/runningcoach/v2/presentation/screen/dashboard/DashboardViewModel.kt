package com.runningcoach.v2.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.data.local.entity.GoogleFitDailySummaryEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import com.runningcoach.v2.data.repository.GoogleFitRepository
import com.runningcoach.v2.data.repository.UserRepository
import com.runningcoach.v2.domain.model.CompletedWorkout
import com.runningcoach.v2.domain.model.SampleTrainingData
import com.runningcoach.v2.domain.model.Workout
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val user: UserEntity? = null,
    val userName: String = "Runner",
    val todaysWorkout: Workout = SampleTrainingData.todaysWorkout,
    val upcomingWorkouts: List<Workout> = SampleTrainingData.upcomingWorkouts,
    val pastWorkouts: List<CompletedWorkout> = SampleTrainingData.pastWorkouts,
    val fitnessData: GoogleFitDailySummaryEntity? = null,
    val weeklyActivity: List<Int> = listOf(30, 0, 45, 25, 35, 60, 0), // Minutes per day
    val isLoadingUser: Boolean = true,
    val isLoadingFitnessData: Boolean = false,
    val errorMessage: String? = null,
    val trainingPlanName: String = "Marathon Training"
)

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val googleFitRepository: GoogleFitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadFitnessData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingUser = true, errorMessage = null) }
                
                userRepository.getCurrentUserFlow().collect { user ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            user = user,
                            userName = user?.name ?: "Runner",
                            isLoadingUser = false,
                            // Update training plan name based on user's goals
                            trainingPlanName = getUserTrainingPlanName(user)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingUser = false, 
                        errorMessage = "Failed to load user profile: ${e.message}"
                    ) 
                }
            }
        }
    }

    private fun loadFitnessData() {
        viewModelScope.launch {
            if (googleFitRepository.isGoogleFitConnected()) {
                try {
                    _uiState.update { it.copy(isLoadingFitnessData = true) }
                    
                    // Try to get cached data first
                    val cachedData = googleFitRepository.getTodaysFitnessData()
                    if (cachedData != null) {
                        _uiState.update { it.copy(fitnessData = cachedData) }
                    }
                    
                    // Sync fresh data from Google Fit
                    val syncResult = googleFitRepository.syncTodaysFitnessData()
                    if (syncResult.isSuccess) {
                        val freshData = syncResult.getOrNull()
                        _uiState.update { 
                            it.copy(
                                fitnessData = freshData,
                                isLoadingFitnessData = false
                            ) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoadingFitnessData = false,
                                errorMessage = "Failed to sync fitness data"
                            ) 
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            isLoadingFitnessData = false,
                            errorMessage = "Error loading fitness data: ${e.message}"
                        ) 
                    }
                }
            }
        }
    }

    private fun getUserTrainingPlanName(user: UserEntity?): String {
        return user?.runningGoals?.firstOrNull()?.let { goal ->
            when (goal.lowercase()) {
                "marathon" -> "Marathon Training"
                "half_marathon" -> "Half Marathon Training"
                "5k" -> "5K Training Plan"
                "10k" -> "10K Training Plan"
                "weight_loss" -> "Weight Loss Running Plan"
                "general_fitness" -> "General Fitness Plan"
                else -> "Training Plan"
            }
        } ?: "Training Plan"
    }

    fun refreshFitnessData() {
        loadFitnessData()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun retryDataLoad() {
        loadUserData()
        loadFitnessData()
    }

    class Factory(
        private val userRepository: UserRepository,
        private val googleFitRepository: GoogleFitRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(userRepository, googleFitRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}