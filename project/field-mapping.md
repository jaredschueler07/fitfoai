# 🗂️ RunningCoach App - Field Mapping & Data Models

## 📋 Overview

This document provides comprehensive field mappings for all data models, database schemas, and API integrations in the RunningCoach app. It ensures consistency across the entire data layer and serves as a reference for developers.

## 🗄️ Database Schema Mapping

### Users Table
```sql
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    age INTEGER NOT NULL,
    gender TEXT NOT NULL,
    weight REAL NOT NULL,
    height REAL NOT NULL,
    experience_level TEXT NOT NULL,
    preferred_units TEXT NOT NULL,
    timezone TEXT NOT NULL,
    location TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

#### Field Mappings
| Database Field | Kotlin Property | Type | Description | Constraints |
|----------------|-----------------|------|-------------|-------------|
| `id` | `id` | `String` | Unique user identifier | Primary Key, UUID |
| `name` | `name` | `String` | User's full name | Not null, max 100 chars |
| `age` | `age` | `Int` | User's age in years | 13-120 |
| `gender` | `gender` | `Gender` | User's gender identity | Enum values |
| `weight` | `weight` | `Float` | Weight in kg | 20.0-300.0 |
| `height` | `height` | `Float` | Height in cm | 100.0-250.0 |
| `experience_level` | `experienceLevel` | `ExperienceLevel` | Running experience | Enum values |
| `preferred_units` | `preferredUnits` | `Units` | Distance units preference | MILES/KILOMETERS |
| `timezone` | `timezone` | `String` | User's timezone | IANA timezone format |
| `location` | `location` | `String` | User's location | City, State/Country |
| `created_at` | `createdAt` | `Instant` | Account creation timestamp | Unix timestamp |
| `updated_at` | `updatedAt` | `Instant` | Last update timestamp | Unix timestamp |

### Training Plans Table
```sql
CREATE TABLE training_plans (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    goal_distance TEXT NOT NULL,
    goal_time INTEGER NOT NULL,
    goal_date INTEGER NOT NULL,
    goal_priority TEXT NOT NULL,
    start_date INTEGER NOT NULL,
    end_date INTEGER NOT NULL,
    status TEXT NOT NULL,
    adaptations TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### Field Mappings
| Database Field | Kotlin Property | Type | Description | Constraints |
|----------------|-----------------|------|-------------|-------------|
| `id` | `id` | `String` | Unique plan identifier | Primary Key, UUID |
| `user_id` | `userId` | `String` | Associated user ID | Foreign Key |
| `goal_distance` | `goal.distance` | `RaceDistance` | Target race distance | Enum values |
| `goal_time` | `goal.targetTime` | `Duration` | Target completion time | Milliseconds |
| `goal_date` | `goal.raceDate` | `LocalDate` | Race date | Unix timestamp |
| `goal_priority` | `goal.priority` | `GoalPriority` | Goal importance level | Enum values |
| `start_date` | `startDate` | `LocalDate` | Training start date | Unix timestamp |
| `end_date` | `endDate` | `LocalDate` | Training end date | Unix timestamp |
| `status` | `status` | `PlanStatus` | Current plan status | Enum values |
| `adaptations` | `adaptations` | `List<PlanAdaptation>` | Plan modifications | JSON array |
| `created_at` | `createdAt` | `Instant` | Plan creation timestamp | Unix timestamp |
| `updated_at` | `updatedAt` | `Instant` | Last update timestamp | Unix timestamp |

### Training Weeks Table
```sql
CREATE TABLE training_weeks (
    id TEXT PRIMARY KEY,
    plan_id TEXT NOT NULL,
    week_number INTEGER NOT NULL,
    total_distance REAL NOT NULL,
    total_time INTEGER NOT NULL,
    intensity TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES training_plans(id)
);
```

#### Field Mappings
| Database Field | Kotlin Property | Type | Description | Constraints |
|----------------|-----------------|------|-------------|-------------|
| `id` | `id` | `String` | Unique week identifier | Primary Key, UUID |
| `plan_id` | `planId` | `String` | Associated plan ID | Foreign Key |
| `week_number` | `weekNumber` | `Int` | Week number in plan | 1-52 |
| `total_distance` | `totalDistance` | `Float` | Total distance for week | Miles/KM |
| `total_time` | `totalTime` | `Duration` | Total time for week | Milliseconds |
| `intensity` | `intensity` | `TrainingIntensity` | Week intensity level | Enum values |
| `created_at` | `createdAt` | `Instant` | Creation timestamp | Unix timestamp |

### Training Runs Table
```sql
CREATE TABLE training_runs (
    id TEXT PRIMARY KEY,
    week_id TEXT NOT NULL,
    day_of_week TEXT NOT NULL,
    run_type TEXT NOT NULL,
    distance REAL NOT NULL,
    duration INTEGER NOT NULL,
    pace_minutes REAL NOT NULL,
    pace_units TEXT NOT NULL,
    description TEXT,
    coaching_notes TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (week_id) REFERENCES training_weeks(id)
);
```

#### Field Mappings
| Database Field | Kotlin Property | Type | Description | Constraints |
|----------------|-----------------|------|-------------|-------------|
| `id` | `id` | `String` | Unique run identifier | Primary Key, UUID |
| `week_id` | `weekId` | `String` | Associated week ID | Foreign Key |
| `day_of_week` | `dayOfWeek` | `DayOfWeek` | Day of the week | Enum values |
| `run_type` | `type` | `RunType` | Type of training run | Enum values |
| `distance` | `distance` | `Float` | Planned distance | Miles/KM |
| `duration` | `duration` | `Duration` | Planned duration | Milliseconds |
| `pace_minutes` | `pace.minutesPerUnit` | `Float` | Target pace | Minutes per unit |
| `pace_units` | `pace.units` | `Units` | Pace units | MILES/KILOMETERS |
| `description` | `description` | `String` | Run description | Max 500 chars |
| `coaching_notes` | `coachingNotes` | `String` | Coaching instructions | Max 1000 chars |
| `created_at` | `createdAt` | `Instant` | Creation timestamp | Unix timestamp |

### Runs Table
```sql
CREATE TABLE runs (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    plan_id TEXT,
    start_time INTEGER NOT NULL,
    end_time INTEGER,
    distance REAL NOT NULL,
    duration INTEGER NOT NULL,
    average_pace_minutes REAL NOT NULL,
    average_pace_units TEXT NOT NULL,
    calories INTEGER,
    route TEXT,
    metrics TEXT,
    coaching_events TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (plan_id) REFERENCES training_plans(id)
);
```

#### Field Mappings
| Database Field | Kotlin Property | Type | Description | Constraints |
|----------------|-----------------|------|-------------|-------------|
| `id` | `id` | `String` | Unique run identifier | Primary Key, UUID |
| `user_id` | `userId` | `String` | Associated user ID | Foreign Key |
| `plan_id` | `planId` | `String?` | Associated plan ID | Optional Foreign Key |
| `start_time` | `startTime` | `Instant` | Run start timestamp | Unix timestamp |
| `end_time` | `endTime` | `Instant?` | Run end timestamp | Optional |
| `distance` | `distance` | `Float` | Actual distance covered | Miles/KM |
| `duration` | `duration` | `Duration` | Actual duration | Milliseconds |
| `average_pace_minutes` | `averagePace.minutesPerUnit` | `Float` | Average pace | Minutes per unit |
| `average_pace_units` | `averagePace.units` | `Units` | Pace units | MILES/KILOMETERS |
| `calories` | `calories` | `Int?` | Calories burned | Optional |
| `route` | `route` | `List<Location>` | GPS route data | JSON array |
| `metrics` | `metrics` | `RunMetrics` | Run performance metrics | JSON object |
| `coaching_events` | `coachingEvents` | `List<CoachingEvent>` | Coaching interactions | JSON array |
| `created_at` | `createdAt` | `Instant` | Creation timestamp | Unix timestamp |
| `updated_at` | `updatedAt` | `Instant` | Last update timestamp | Unix timestamp |

### Coaches Table
```sql
CREATE TABLE coaches (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    personality_type TEXT NOT NULL,
    voice_id TEXT,
    color_theme TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL
);
```

#### Field Mappings
| Database Field | Kotlin Property | Type | Description | Constraints |
|----------------|-----------------|------|-------------|-------------|
| `id` | `id` | `String` | Unique coach identifier | Primary Key, UUID |
| `name` | `name` | `String` | Coach name | Not null, max 50 chars |
| `personality_type` | `personalityType` | `CoachPersonality` | Coach personality | Enum values |
| `voice_id` | `voiceId` | `String?` | TTS voice identifier | Optional |
| `color_theme` | `colorTheme` | `String` | UI color theme | Hex color code |
| `description` | `description` | `String?` | Coach description | Optional, max 200 chars |
| `is_active` | `isActive` | `Boolean` | Coach availability | Default true |
| `created_at` | `createdAt` | `Instant` | Creation timestamp | Unix timestamp |

## 🔄 API Field Mappings

### Google Gemini API

#### Plan Generation Request
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Generate a training plan for a user with the following profile..."
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "topK": 40,
    "topP": 0.95,
    "maxOutputTokens": 2048
  }
}
```

#### Field Mappings
| API Field | Kotlin Property | Type | Description | Example |
|-----------|-----------------|------|-------------|---------|
| `contents[0].parts[0].text` | `prompt` | `String` | AI prompt text | Generated from user profile |
| `generationConfig.temperature` | `temperature` | `Float` | Response creativity | 0.7 |
| `generationConfig.topK` | `topK` | `Int` | Token selection limit | 40 |
| `generationConfig.topP` | `topP` | `Float` | Nucleus sampling | 0.95 |
| `generationConfig.maxOutputTokens` | `maxTokens` | `Int` | Response length limit | 2048 |

#### Plan Generation Response
```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "{\"plan\": {...}}"
          }
        ]
      }
    }
  ]
}
```

#### Field Mappings
| API Field | Kotlin Property | Type | Description | Example |
|-----------|-----------------|------|-------------|---------|
| `candidates[0].content.parts[0].text` | `responseText` | `String` | AI response | JSON plan data |
| `candidates[0].finishReason` | `finishReason` | `String` | Completion reason | "STOP" |

### ElevenLabs TTS API

#### Voice Synthesis Request
```json
{
  "text": "Great job! You're maintaining a perfect pace.",
  "model_id": "eleven_monolingual_v1",
  "voice_settings": {
    "stability": 0.5,
    "similarity_boost": 0.75,
    "style": 0.0,
    "use_speaker_boost": true
  }
}
```

#### Field Mappings
| API Field | Kotlin Property | Type | Description | Example |
|-----------|-----------------|------|-------------|---------|
| `text` | `text` | `String` | Text to synthesize | Coaching message |
| `model_id` | `modelId` | `String` | TTS model | "eleven_monolingual_v1" |
| `voice_settings.stability` | `settings.stability` | `Float` | Voice stability | 0.5 |
| `voice_settings.similarity_boost` | `settings.similarityBoost` | `Float` | Voice similarity | 0.75 |
| `voice_settings.style` | `settings.style` | `Float` | Voice style | 0.0 |
| `voice_settings.use_speaker_boost` | `settings.useSpeakerBoost` | `Boolean` | Speaker boost | true |

#### Voice Synthesis Response
```json
{
  "audio": "base64_encoded_audio_data",
  "generation_id": "abc123"
}
```

#### Field Mappings
| API Field | Kotlin Property | Type | Description | Example |
|-----------|-----------------|------|-------------|---------|
| `audio` | `audioBytes` | `ByteArray` | Audio data | Base64 decoded |
| `generation_id` | `generationId` | `String` | Generation ID | "abc123" |

## 📊 Data Type Mappings

### Enum Mappings

#### Gender
```kotlin
enum class Gender {
    MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
}
```
| Database Value | Kotlin Value | Description |
|----------------|---------------|-------------|
| `"MALE"` | `Gender.MALE` | Male |
| `"FEMALE"` | `Gender.FEMALE` | Female |
| `"OTHER"` | `Gender.OTHER` | Other |
| `"PREFER_NOT_TO_SAY"` | `Gender.PREFER_NOT_TO_SAY` | Prefer not to say |

#### Experience Level
```kotlin
enum class ExperienceLevel {
    BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
}
```
| Database Value | Kotlin Value | Description |
|----------------|---------------|-------------|
| `"BEGINNER"` | `ExperienceLevel.BEGINNER` | New to running |
| `"INTERMEDIATE"` | `ExperienceLevel.INTERMEDIATE` | Some running experience |
| `"ADVANCED"` | `ExperienceLevel.ADVANCED` | Experienced runner |
| `"EXPERT"` | `ExperienceLevel.EXPERT` | Elite runner |

#### Race Distance
```kotlin
enum class RaceDistance {
    FIVE_K, TEN_K, HALF_MARATHON, MARATHON
}
```
| Database Value | Kotlin Value | Description |
|----------------|---------------|-------------|
| `"FIVE_K"` | `RaceDistance.FIVE_K` | 5 kilometers |
| `"TEN_K"` | `RaceDistance.TEN_K` | 10 kilometers |
| `"HALF_MARATHON"` | `RaceDistance.HALF_MARATHON` | 21.1 kilometers |
| `"MARATHON"` | `RaceDistance.MARATHON` | 42.2 kilometers |

#### Run Type
```kotlin
enum class RunType {
    EASY_RUN, TEMPO_RUN, LONG_RUN, SPEED_WORK, REST, CROSS_TRAINING
}
```
| Database Value | Kotlin Value | Description |
|----------------|---------------|-------------|
| `"EASY_RUN"` | `RunType.EASY_RUN` | Easy pace run |
| `"TEMPO_RUN"` | `RunType.TEMPO_RUN` | Tempo/threshold run |
| `"LONG_RUN"` | `RunType.LONG_RUN` | Long distance run |
| `"SPEED_WORK"` | `RunType.SPEED_WORK` | Speed intervals |
| `"REST"` | `RunType.REST` | Rest day |
| `"CROSS_TRAINING"` | `RunType.CROSS_TRAINING` | Cross training |

### Complex Type Mappings

#### Location Data
```kotlin
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float,
    val timestamp: Instant,
    val speed: Float?,
    val bearing: Float?
)
```
| JSON Field | Kotlin Property | Type | Description |
|------------|-----------------|------|-------------|
| `lat` | `latitude` | `Double` | Latitude coordinate |
| `lng` | `longitude` | `Double` | Longitude coordinate |
| `alt` | `altitude` | `Double?` | Altitude (optional) |
| `acc` | `accuracy` | `Float` | GPS accuracy |
| `ts` | `timestamp` | `Instant` | Timestamp |
| `spd` | `speed` | `Float?` | Speed (optional) |
| `brg` | `bearing` | `Float?` | Bearing (optional) |

#### Run Metrics
```kotlin
data class RunMetrics(
    val currentPace: Pace,
    val averagePace: Pace,
    val bestPace: Pace,
    val elevation: Float,
    val heartRate: Int?,
    val cadence: Int?,
    val strideLength: Float?
)
```
| JSON Field | Kotlin Property | Type | Description |
|------------|-----------------|------|-------------|
| `current_pace` | `currentPace` | `Pace` | Current pace |
| `average_pace` | `averagePace` | `Pace` | Average pace |
| `best_pace` | `bestPace` | `Pace` | Best pace |
| `elevation` | `elevation` | `Float` | Elevation gain |
| `heart_rate` | `heartRate` | `Int?` | Heart rate (optional) |
| `cadence` | `cadence` | `Int?` | Running cadence (optional) |
| `stride_length` | `strideLength` | `Float?` | Stride length (optional) |

## 🔄 Data Conversion Utilities

### Type Converters
```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }
    
    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
    
    @TypeConverter
    fun fromGender(gender: Gender): String {
        return gender.name
    }
    
    @TypeConverter
    fun toGender(value: String): Gender {
        return Gender.valueOf(value)
    }
    
    @TypeConverter
    fun fromLocationList(locations: List<Location>): String {
        return Json.encodeToString(locations)
    }
    
    @TypeConverter
    fun toLocationList(value: String): List<Location> {
        return Json.decodeFromString(value)
    }
}
```

### Mapper Functions
```kotlin
object DataMappers {
    fun mapUserToEntity(user: User): UserEntity {
        return UserEntity(
            id = user.id,
            name = user.name,
            age = user.age,
            gender = user.gender.name,
            weight = user.weight,
            height = user.height,
            experienceLevel = user.experienceLevel.name,
            preferredUnits = user.preferredUnits.name,
            timezone = user.timezone,
            location = user.location,
            createdAt = user.createdAt.toEpochMilli(),
            updatedAt = user.updatedAt.toEpochMilli()
        )
    }
    
    fun mapEntityToUser(entity: UserEntity): User {
        return User(
            id = entity.id,
            name = entity.name,
            age = entity.age,
            gender = Gender.valueOf(entity.gender),
            weight = entity.weight,
            height = entity.height,
            experienceLevel = ExperienceLevel.valueOf(entity.experienceLevel),
            preferredUnits = Units.valueOf(entity.preferredUnits),
            timezone = entity.timezone,
            location = entity.location,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt)
        )
    }
}
```

## 📝 Validation Rules

### User Profile Validation
```kotlin
object UserValidation {
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Name cannot be empty")
            name.length > 100 -> ValidationResult.Error("Name too long")
            !name.matches(Regex("^[a-zA-Z\\s'-]+$")) -> ValidationResult.Error("Invalid name format")
            else -> ValidationResult.Success
        }
    }
    
    fun validateAge(age: Int): ValidationResult {
        return when {
            age < 13 -> ValidationResult.Error("Must be at least 13 years old")
            age > 120 -> ValidationResult.Error("Invalid age")
            else -> ValidationResult.Success
        }
    }
    
    fun validateWeight(weight: Float): ValidationResult {
        return when {
            weight < 20.0f -> ValidationResult.Error("Weight too low")
            weight > 300.0f -> ValidationResult.Error("Weight too high")
            else -> ValidationResult.Success
        }
    }
}
```

### Training Plan Validation
```kotlin
object PlanValidation {
    fun validateGoalDate(goalDate: LocalDate): ValidationResult {
        val today = LocalDate.now()
        return when {
            goalDate.isBefore(today) -> ValidationResult.Error("Goal date cannot be in the past")
            goalDate.isAfter(today.plusYears(2)) -> ValidationResult.Error("Goal date too far in future")
            else -> ValidationResult.Success
        }
    }
    
    fun validateTrainingDuration(startDate: LocalDate, endDate: LocalDate): ValidationResult {
        val duration = ChronoUnit.WEEKS.between(startDate, endDate)
        return when {
            duration < 4 -> ValidationResult.Error("Training plan too short")
            duration > 52 -> ValidationResult.Error("Training plan too long")
            else -> ValidationResult.Success
        }
    }
}
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
