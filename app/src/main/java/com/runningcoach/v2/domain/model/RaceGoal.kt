package com.runningcoach.v2.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class RaceGoal(
    val id: String,
    val name: String,
    val distance: RaceDistance,
    val targetTime: String? = null,
    val raceDate: String? = null, // ISO string format
    val location: String? = null,
    val isCustom: Boolean = false
)

enum class RaceDistance(val displayName: String, val kilometers: Double) {
    FIVE_K("5K", 5.0),
    TEN_K("10K", 10.0),
    HALF_MARATHON("Half Marathon", 21.1),
    MARATHON("Marathon", 42.2),
    CUSTOM("Custom", 0.0)
}

// Popular races based on PRD requirements
object PopularRaces {
    val chicagoMarathon = RaceGoal(
        id = "chicago_marathon",
        name = "Chicago Marathon",
        distance = RaceDistance.MARATHON,
        raceDate = "2025-10-12",
        location = "Chicago, IL"
    )
    
    val nyMarathon = RaceGoal(
        id = "ny_marathon", 
        name = "New York Marathon",
        distance = RaceDistance.MARATHON,
        raceDate = "2025-11-02",
        location = "New York, NY"
    )
    
    val bostonMarathon = RaceGoal(
        id = "boston_marathon",
        name = "Boston Marathon", 
        distance = RaceDistance.MARATHON,
        raceDate = "2025-04-21",
        location = "Boston, MA"
    )
    
    val brooklynHalf = RaceGoal(
        id = "brooklyn_half",
        name = "Brooklyn Half Marathon",
        distance = RaceDistance.HALF_MARATHON,
        raceDate = "2025-05-17",
        location = "Brooklyn, NY"
    )
    
    val all = listOf(chicagoMarathon, nyMarathon, bostonMarathon, brooklynHalf)
}
