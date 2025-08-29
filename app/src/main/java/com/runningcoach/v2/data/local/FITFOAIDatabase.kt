package com.runningcoach.v2.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.runningcoach.v2.BuildConfig
import com.runningcoach.v2.data.local.dao.*
import com.runningcoach.v2.data.local.entity.*
import com.runningcoach.v2.data.local.converter.Converters

@Database(
    entities = [
        UserEntity::class,
        RunSessionEntity::class,
        TrainingPlanEntity::class,
        WorkoutEntity::class,
        AIConversationEntity::class,
        GoogleFitDailySummaryEntity::class,
        HealthConnectDailySummaryEntity::class,
        ConnectedAppEntity::class,
        VoiceLineEntity::class,
        CoachPersonalityEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FITFOAIDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun runSessionDao(): RunSessionDao
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun aiConversationDao(): AIConversationDao
    abstract fun googleFitDailySummaryDao(): GoogleFitDailySummaryDao
    abstract fun healthConnectDailySummaryDao(): HealthConnectDailySummaryDao
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
        
        /**
         * Migration from version 4 to 5: Add source field to RunSessionEntity
         * Adds source column to track data origin (FITFOAI vs GOOGLE_FIT)
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add source column with default value FITFOAI
                database.execSQL("ALTER TABLE `run_sessions` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'FITFOAI'")
                
                // Backfill existing data based on Google Fit sync status
                // Set source to GOOGLE_FIT where googleFitSessionId is not null OR syncedWithGoogleFit is true
                database.execSQL("""
                    UPDATE `run_sessions` 
                    SET `source` = 'GOOGLE_FIT' 
                    WHERE `googleFitSessionId` IS NOT NULL OR `syncedWithGoogleFit` = 1
                """)
                
                // Create index on source column for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_run_sessions_source` ON `run_sessions` (`source`)")
            }
        }
        
        /**
         * Migration from version 5 to 6: Add WorkoutEntity table
         * Creates workouts table for training plan workout management
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create WorkoutEntity table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workouts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `planId` INTEGER NOT NULL,
                        `scheduledDate` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `targetDistance` REAL,
                        `targetDuration` INTEGER,
                        `targetPace` REAL,
                        `intensity` INTEGER NOT NULL,
                        `actualRunSessionId` INTEGER,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`planId`) REFERENCES `training_plans`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`actualRunSessionId`) REFERENCES `run_sessions`(`id`) ON DELETE SET NULL
                    )
                """)
                
                // Create indices for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_planId` ON `workouts` (`planId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_scheduledDate` ON `workouts` (`scheduledDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_actualRunSessionId` ON `workouts` (`actualRunSessionId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_status` ON `workouts` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_planId_scheduledDate` ON `workouts` (`planId`, `scheduledDate`)")
            }
        }
        
        /**
         * Migration from version 6 to 7: Enhance TrainingPlanEntity
         * Adds AI generation fields and enhanced race preparation features
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to training_plans table
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `raceDate` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `planStartDate` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `planEndDate` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `targetDistance` REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `targetTime` INTEGER")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `modelVersion` TEXT NOT NULL DEFAULT 'v1.0'")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `generationPrompt` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `weeklyMileageProgression` TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE `training_plans` ADD COLUMN `baselineStats` TEXT NOT NULL DEFAULT '{}'")
                
                // Migrate existing data: set raceDate from targetDate where available
                database.execSQL("""
                    UPDATE `training_plans` 
                    SET `raceDate` = COALESCE(`targetDate`, `createdAt`),
                        `planStartDate` = `createdAt`,
                        `planEndDate` = COALESCE(`targetDate`, `createdAt` + (12 * 7 * 24 * 60 * 60 * 1000))
                    WHERE `raceDate` = 0
                """)
                
                // Set default target distance for existing plans (assume 5K = 5000m if not specified)
                database.execSQL("""
                    UPDATE `training_plans` 
                    SET `targetDistance` = 5000.0 
                    WHERE `targetDistance` = 0.0
                """)
                
                // Create new indices for enhanced queries
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_training_plans_raceDate` ON `training_plans` (`raceDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_training_plans_planStartDate_planEndDate` ON `training_plans` (`planStartDate`, `planEndDate`)")
                
                // Add foreign key constraint (if not already exists from Room schema)
                // Note: SQLite doesn't support adding foreign keys via ALTER TABLE, 
                // so we rely on Room's schema validation for new inserts
            }
        }
        
        /**
         * Migration from version 7 to 8: Add Health Connect support
         * Adds Health Connect daily summaries table and Health Connect sync fields to run_sessions
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create HealthConnectDailySummaryEntity table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `health_connect_daily_summaries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `steps` INTEGER NOT NULL DEFAULT 0,
                        `distance` REAL NOT NULL DEFAULT 0.0,
                        `calories` INTEGER NOT NULL DEFAULT 0,
                        `activeMinutes` INTEGER NOT NULL DEFAULT 0,
                        `avgHeartRate` REAL,
                        `maxHeartRate` REAL,
                        `minHeartRate` REAL,
                        `floorsClimbed` INTEGER,
                        `exerciseMinutes` INTEGER NOT NULL DEFAULT 0,
                        `sleepMinutes` INTEGER,
                        `sleepQuality` INTEGER,
                        `waterIntakeMl` INTEGER,
                        `weight` REAL,
                        `lastSynced` INTEGER NOT NULL,
                        `dataSource` TEXT NOT NULL DEFAULT 'HEALTH_CONNECT',
                        `recordIds` TEXT,
                        `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                        `syncError` TEXT,
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE
                    )
                """)
                
                // Add Health Connect sync fields to run_sessions
                database.execSQL("ALTER TABLE `run_sessions` ADD COLUMN `healthConnectSessionId` TEXT")
                database.execSQL("ALTER TABLE `run_sessions` ADD COLUMN `syncedWithHealthConnect` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `run_sessions` ADD COLUMN `healthConnectLastSyncTime` INTEGER")
                database.execSQL("ALTER TABLE `run_sessions` ADD COLUMN `healthConnectSyncError` TEXT")
                database.execSQL("ALTER TABLE `run_sessions` ADD COLUMN `migratedToHealthConnect` INTEGER NOT NULL DEFAULT 0")
                
                // Create indices for Health Connect daily summaries
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_health_connect_daily_summaries_userId` ON `health_connect_daily_summaries` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_health_connect_daily_summaries_date` ON `health_connect_daily_summaries` (`date`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_health_connect_daily_summaries_userId_date` ON `health_connect_daily_summaries` (`userId`, `date`)")
                
                // Create indices for new run_sessions fields
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_run_sessions_healthConnectSessionId` ON `run_sessions` (`healthConnectSessionId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_run_sessions_syncedWithHealthConnect` ON `run_sessions` (`syncedWithHealthConnect`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_run_sessions_migratedToHealthConnect` ON `run_sessions` (`migratedToHealthConnect`)")
            }
        }
        
        fun getDatabase(context: Context): FITFOAIDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    FITFOAIDatabase::class.java,
                    "fitfoai_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration() // Allow destructive migration if schema mismatch
                .addCallback(DatabaseCallback())
                
                // Performance monitoring is handled by DatabaseCallback
                
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
