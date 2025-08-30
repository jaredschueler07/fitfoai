package com.runningcoach.v2.presentation.screen.connectapps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ConnectAppsScreen(viewModel: ConnectAppsViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Connect your favorite apps")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.onConnectSpotifyClick() }) {
            Text(text = "Connect to Spotify")
        }
        // Add other app connection buttons here
    }
}