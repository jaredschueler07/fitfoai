package com.runningcoach.v2.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runningcoach.v2.di.AppContainer
import com.runningcoach.v2.domain.model.Workout
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class PlaylistGenerationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val appContainer = AppContainer(applicationContext)
        val workoutParser = appContainer.workoutParser
        val playlistGenerator = appContainer.playlistGenerator
        val spotifyService = appContainer.spotifyService

        val workoutJson = inputData.getString("workout_json")
        val workout: Workout = Json.decodeFromString(workoutJson ?: return Result.failure())

        val workoutSegments = workoutParser.parseWorkout(workout)

        val userTracks = spotifyService.getAllUserTracks()

        val generatedPlaylistUris = playlistGenerator.generatePlaylist(workoutSegments, userTracks)

        val playlistId = spotifyService.createPlaylist(
            name = "FITFO AI: ${workout.name} (${workout.duration}min)",
            description = "Auto-generated for your workout by FITFO AI"
        )

        if (playlistId != null) {
            spotifyService.addTracksToPlaylist(playlistId, generatedPlaylistUris)
            return Result.success()
        } else {
            return Result.failure()
        }
    }
}