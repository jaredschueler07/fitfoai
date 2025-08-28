package com.runningcoach.v2.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.runningcoach.v2.data.local.dao.*
import com.runningcoach.v2.data.local.entity.*
import com.runningcoach.v2.data.local.converter.Converters

@Database(
    entities = [
        UserEntity::class,
        RunSessionEntity::class,
        TrainingPlanEntity::class,
        AIConversationEntity::class,
        GoogleFitDailySummaryEntity::class,
        ConnectedAppEntity::class,
        VoiceLineEntity::class,
        CoachPersonalityEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FITFOAIDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun runSessionDao(): RunSessionDao
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun aiConversationDao(): AIConversationDao
    abstract fun googleFitDailySummaryDao(): GoogleFitDailySummaryDao
    abstract fun connectedAppDao(): ConnectedAppDao
    abstract fun voiceLineDao(): VoiceLineDao
    abstract fun coachPersonalityDao(): CoachPersonalityDao
    
    companion object {
        @Volatile
        private var INSTANCE: FITFOAIDatabase? = null
        
        /**
         * Migration from version 3 to 4: Add voice coaching entities
         * Adds VoiceLineEntity and CoachPersonalityEntity tables
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create VoiceLineEntity table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `voice_lines` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `coachId` TEXT NOT NULL,
                        `contextType` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `audioFilePath` TEXT,
                        `isPreloaded` INTEGER NOT NULL DEFAULT 0,
                        `lastUsed` INTEGER NOT NULL DEFAULT 0,
                        `useCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """)
                
                // Create CoachPersonalityEntity table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `coach_personalities` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `voiceId` TEXT NOT NULL,
                        `motivationalStyle` TEXT NOT NULL,
                        `coachingFrequency` TEXT NOT NULL,
                        `personalityTraits` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """)
                
                // Create indices for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_voice_lines_coachId` ON `voice_lines` (`coachId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_voice_lines_contextType` ON `voice_lines` (`contextType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_coach_personalities_isActive` ON `coach_personalities` (`isActive`)")
            }
        }
        
        fun getDatabase(context: Context): FITFOAIDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FITFOAIDatabase::class.java,
                    "fitfoai_database"
                )
                .addMigrations(MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
