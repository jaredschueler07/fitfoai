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
        CoachPersonalityEntity::class,
        SpotifyTrackCacheEntity::class
    ],
    version = 6,
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
    abstract fun spotifyTrackCacheDao(): SpotifyTrackCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: FITFOAIDatabase? = null
        
        // ... (previous migrations)

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `spotify_track_cache` ADD COLUMN `durationMs` INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        fun getDatabase(context: Context): FITFOAIDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FITFOAIDatabase::class.java,
                    "fitfoai_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}