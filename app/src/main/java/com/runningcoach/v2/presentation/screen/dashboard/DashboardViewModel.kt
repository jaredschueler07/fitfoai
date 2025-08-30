package com.runningcoach.v2.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.data.local.entity.HealthConnectDailySummaryEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import com.runningcoach.v2.data.repository.HealthConnectRepository
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
    val fitnessData: HealthConnectDailySummaryEntity? = null,
    val weeklyActivity: List<Int> = listOf(30, 0, 45, 25, 35, 60, 0), // Minutes per day
    val isLoadingUser: Boolean = true,
    val isLoadingFitnessData: Boolean = false,
    val errorMessage: String? = null,
    val trainingPlanName: String = "Marathon Training"
)

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val healthConnectRepository: HealthConnectRepository
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
            if (healthConnectRepository.isHealthConnectConnected()) {
                try {
                    _uiState.update { it.copy(isLoadingFitnessData = true) }
                    
                    // Load today's cached Health Connect data
                    val today = healthConnectRepository.getTodaysHealthData()
                    if (today != null) _uiState.update { it.copy(fitnessData = today) }
                    _uiState.update { it.copy(isLoadingFitnessData = false) }
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
        private val healthConnectRepository: HealthConnectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(userRepository, healthConnectRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}