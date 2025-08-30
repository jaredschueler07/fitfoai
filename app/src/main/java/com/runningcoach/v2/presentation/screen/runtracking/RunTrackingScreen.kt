package com.runningcoach.v2.presentation.screen.runtracking

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.presentation.theme.AppColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runningcoach.v2.data.service.SpotifyService
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.PlaylistSelectionDialog
import com.runningcoach.v2.presentation.components.icons.*
import com.runningcoach.v2.presentation.components.maps.RunTrackingMap
import com.google.android.gms.maps.model.LatLng

@Composable
fun RunTrackingScreen(
    viewModel: RunTrackingViewModel,
    spotifyService: SpotifyService,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spotifyConnected by spotifyService.isConnected.collectAsState()
    val currentTrack by spotifyService.currentTrack.collectAsState()
    val showPlaylistDialog by viewModel.showPlaylistDialog.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(
            playlists = playlists,
            onPlaylistSelected = { viewModel.onPlaylistSelected(it) },
            onDismiss = { viewModel.onDismissPlaylistDialog() }
        )
    }

    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission && !uiState.permissionRequested) {
            viewModel.requestLocationPermissions()
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.GradientStart,
                        AppColors.GradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (Header)

            if (spotifyConnected) {
                SpotifyControlCard(
                    currentTrack = currentTrack?.name,
                    onPlayPause = { if (currentTrack != null) spotifyService.pause() else spotifyService.resume() },
                    onSelectPlaylist = { viewModel.onSelectPlaylistClicked() }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ... (Rest of the screen)
        }
    }
}

// ... (Rest of the composables)