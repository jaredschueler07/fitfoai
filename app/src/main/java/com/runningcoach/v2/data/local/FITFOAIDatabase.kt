package com.runningcoach.v2.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.runningcoach.v2.data.local.dao.*
import com.runningcoach.v2.data.local.entity.*
import com.runningcoach.v2.data.local.converter.Converters

@Database(
    entities = [
        UserEntity::class,
        RunSessionEntity::class,
        TrainingPlanEntity::class,
        AIConversationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FITFOAIDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun runSessionDao(): RunSessionDao
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun aiConversationDao(): AIConversationDao
    
    companion object {
        @Volatile
        private var INSTANCE: FITFOAIDatabase? = null
        
        fun getDatabase(context: Context): FITFOAIDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FITFOAIDatabase::class.java,
                    "fitfoai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
