package com.runningcoach.v2.presentation.screen.runtracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.data.service.SpotifyService
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.domain.usecase.StartRunSessionUseCase
import com.runningcoach.v2.domain.usecase.TrackRunSessionUseCase
import com.runningcoach.v2.domain.usecase.EndRunSessionUseCase
import com.runningcoach.v2.domain.usecase.TrackingData
import com.runningcoach.v2.data.service.VoiceCoachingManager
import com.spotify.protocol.types.ListItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RunTrackingViewModel(
    private val startRunSessionUseCase: StartRunSessionUseCase,
    private val trackRunSessionUseCase: TrackRunSessionUseCase,
    private val endRunSessionUseCase: EndRunSessionUseCase,
    private val voiceCoachingManager: VoiceCoachingManager,
    private val spotifyService: SpotifyService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunTrackingUiState())
    val uiState: StateFlow<RunTrackingUiState> = _uiState.asStateFlow()

    private val _playlists = MutableStateFlow<List<ListItem>>(emptyList())
    val playlists: StateFlow<List<ListItem>> = _playlists.asStateFlow()

    private val _showPlaylistDialog = MutableStateFlow(false)
    val showPlaylistDialog: StateFlow<Boolean> = _showPlaylistDialog.asStateFlow()

    private var currentSessionId: Long? = null
    private val defaultUserId = 1L // TODO: Get from user session management

    // ... (rest of the ViewModel)

    fun onSelectPlaylistClicked() {
        fetchPlaylists()
        _showPlaylistDialog.value = true
    }

    fun onPlaylistSelected(playlist: ListItem) {
        spotifyService.playPlaylist(playlist.uri)
        _showPlaylistDialog.value = false
    }

    fun onDismissPlaylistDialog() {
        _showPlaylistDialog.value = false
    }

    private fun fetchPlaylists() {
        viewModelScope.launch {
            // This is a simplified representation. In a real app,
            // the SpotifyService would have a method to fetch playlists.
            // For now, we'll use a placeholder list.
            _playlists.value = listOf(
                ListItem("1", "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M", null, "Running Hits", "", false, true),
                ListItem("2", "spotify:playlist:37i9dQZF1DX0hWmn8dI09w", null, "Cardio", "", false, true)
            )
        }
    }
}

data class RunTrackingUiState(
    // ... (existing properties)
    val playlists: List<ListItem> = emptyList(),
    val showPlaylistDialog: Boolean = false
) {
    // ... (existing getters)
}

// ... (rest of the existing enums and data classes)