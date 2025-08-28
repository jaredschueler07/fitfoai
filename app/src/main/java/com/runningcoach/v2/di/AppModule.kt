package com.runningcoach.v2.di

import android.content.Context
import androidx.room.Room
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.repository.RunSessionRepositoryImpl
import com.runningcoach.v2.data.service.LocationService
import com.runningcoach.v2.domain.repository.RunSessionRepository
import com.runningcoach.v2.domain.usecase.StartRunSessionUseCase
import com.runningcoach.v2.domain.usecase.TrackRunSessionUseCase
import com.runningcoach.v2.domain.usecase.EndRunSessionUseCase
import com.runningcoach.v2.domain.usecase.GetRunSessionsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency injection container.
 * Provides all dependencies for the application without Hilt.
 * [TECH-DEBT] Replace with Hilt once KSP compatibility issues are resolved.
 */
class AppContainer(private val context: Context) {

    private val database: FITFOAIDatabase by lazy {
        Room.databaseBuilder(
            context,
            FITFOAIDatabase::class.java,
            "fitfoai_database"
        ).build()
    }

    private val runSessionDao: RunSessionDao by lazy {
        database.runSessionDao()
    }

    private val locationService: LocationService by lazy {
        LocationService(context)
    }

    private val applicationScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    val runSessionRepository: RunSessionRepository by lazy {
        RunSessionRepositoryImpl(runSessionDao, locationService, applicationScope)
    }

    val startRunSessionUseCase: StartRunSessionUseCase by lazy {
        StartRunSessionUseCase(runSessionRepository)
    }

    val trackRunSessionUseCase: TrackRunSessionUseCase by lazy {
        TrackRunSessionUseCase(runSessionRepository)
    }

    val endRunSessionUseCase: EndRunSessionUseCase by lazy {
        EndRunSessionUseCase(runSessionRepository)
    }

    val getRunSessionsUseCase: GetRunSessionsUseCase by lazy {
        GetRunSessionsUseCase(runSessionRepository)
    }
}