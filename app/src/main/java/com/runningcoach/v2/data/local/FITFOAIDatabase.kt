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
        ConnectedAppEntity::class,
        VoiceLineEntity::class,
        CoachPersonalityEntity::class
    ],
    version = 7,
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
        
        fun getDatabase(context: Context): FITFOAIDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    FITFOAIDatabase::class.java,
                    "fitfoai_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(DatabaseCallback())
                
                // Add performance monitoring for debug builds
                if (BuildConfig.DEBUG) {
                    try {
                        // Initialize performance monitoring (debug builds only)
                        val performanceConfigClass = Class.forName("com.runningcoach.v2.config.PerformanceConfig")
                        val initializeMethod = performanceConfigClass.getMethod("initialize")
                        initializeMethod.invoke(null)
                    } catch (e: ClassNotFoundException) {
                        // PerformanceConfig not available (shouldn't happen in debug builds)
                    }
                }
                
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
