package com.runningcoach.v2.data.service

import com.runningcoach.v2.data.local.entity.SpotifyTrackCacheEntity
import com.runningcoach.v2.domain.model.WorkoutSegment
import kotlin.math.abs

class PlaylistGenerator {

    fun generatePlaylist(
        segments: List<WorkoutSegment>,
        userTracks: List<SpotifyTrackCacheEntity>
    ): List<String> {
        val playlist = mutableListOf<String>()
        val availableTracks = userTracks.toMutableList()

        segments.forEach { segment ->
            val segmentDurationMillis = segment.durationMinutes * 60 * 1000
            var currentSegmentTracks = mutableListOf<SpotifyTrackCacheEntity>()
            var currentDuration = 0

            val suitableTracks = availableTracks
                .filter { it.bpm in segment.minBPM..segment.maxBPM }
                .shuffled()

            for (track in suitableTracks) {
                if (currentDuration + track.durationMs <= segmentDurationMillis + 30000) { // Allow 30s leeway
                    currentSegmentTracks.add(track)
                    currentDuration += track.durationMs
                    availableTracks.remove(track)
                }
            }

            // Simple greedy approach, can be improved with more complex algorithms
            playlist.addAll(currentSegmentTracks.map { it.uri })
        }
        return playlist
    }
}