package com.runningcoach.v2.domain.model

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null, // in m/s
    val bearing: Float? = null, // in degrees
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return """
            {
                "latitude": $latitude,
                "longitude": $longitude,
                "altitude": ${altitude ?: "null"},
                "accuracy": ${accuracy ?: "null"},
                "speed": ${speed ?: "null"},
                "bearing": ${bearing ?: "null"},
                "timestamp": $timestamp
            }
        """.trimIndent()
    }
    
    companion object {
        fun fromJson(json: String): LocationData? {
            return try {
                // Simple JSON parsing - in production, use proper JSON library
                val regex = """"(\w+)":\s*([^,}]+)""".toRegex()
                val matches = regex.findAll(json)
                val map = matches.associate { 
                    it.groupValues[1] to it.groupValues[2].trim().removeSurrounding("\"")
                }
                
                LocationData(
                    latitude = map["latitude"]?.toDoubleOrNull() ?: 0.0,
                    longitude = map["longitude"]?.toDoubleOrNull() ?: 0.0,
                    altitude = map["altitude"]?.toDoubleOrNull(),
                    accuracy = map["accuracy"]?.toFloatOrNull(),
                    speed = map["speed"]?.toFloatOrNull(),
                    bearing = map["bearing"]?.toFloatOrNull(),
                    timestamp = map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
