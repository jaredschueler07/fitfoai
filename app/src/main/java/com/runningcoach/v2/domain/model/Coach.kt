package com.runningcoach.v2.domain.model

data class Coach(
    val id: String,
    val name: String,
    val style: String,
    val description: String,
    val personality: CoachPersonality
)

enum class CoachPersonality {
    BENNETT,  // Enthusiastic & philosophical
    MARIANA,  // Supportive & encouraging
    BECS,     // Motivational & energetic
    GOGGINS   // Intense & challenging
}

// Sample coaches based on wireframe
object SampleCoaches {
    val bennett = Coach(
        id = "bennett",
        name = "Coach Bennett",
        style = "Enthusiastic & philosophical",
        description = "Motivational coach who believes in your potential",
        personality = CoachPersonality.BENNETT
    )
    
    val mariana = Coach(
        id = "mariana", 
        name = "Coach Mariana",
        style = "Supportive & encouraging",
        description = "Gentle guidance with consistent motivation",
        personality = CoachPersonality.MARIANA
    )
    
    val becs = Coach(
        id = "becs",
        name = "Coach Becs", 
        style = "Motivational & energetic",
        description = "High-energy coaching with positive vibes",
        personality = CoachPersonality.BECS
    )
    
    val goggins = Coach(
        id = "goggins",
        name = "Coach Goggins",
        style = "Intense & challenging", 
        description = "Push your limits and embrace the grind",
        personality = CoachPersonality.GOGGINS
    )
    
    val all = listOf(bennett, mariana, becs, goggins)
}
