package com.runningcoach.v2.domain.model

data class User(
    val id: String,
    val name: String,
    val age: Int? = null,
    val height: String? = null,
    val weight: String? = null,
    val fitnessLevel: FitnessLevel = FitnessLevel.BEGINNER,
    val runningGoals: List<RunningGoal> = emptyList(),
    val connectedApps: List<ConnectedApp> = emptyList(),
    val selectedCoach: Coach? = null
)

enum class FitnessLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

data class ConnectedApp(
    val id: String,
    val name: String,
    val type: AppType,
    val isConnected: Boolean = false,
    val lastSyncTime: Long? = null
)

enum class AppType {
    GOOGLE_FIT,
    SPOTIFY
}

enum class RunningGoal {
    GENERAL_FITNESS,
    WEIGHT_LOSS,
    ENDURANCE,
    SPEED,
    RACE_TRAINING
}
