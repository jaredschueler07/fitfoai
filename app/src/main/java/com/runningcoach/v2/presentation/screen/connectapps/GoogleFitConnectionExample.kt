package com.runningcoach.v2.presentation.screen.connectapps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.data.manager.GoogleFitManager
import com.runningcoach.v2.presentation.viewmodel.GoogleFitViewModel
import kotlinx.coroutines.launch

/**
 * Example of how to use the new GoogleFitManager in UI
 * This shows a clean, simplified approach to Google Fit integration
 */
@Composable
fun GoogleFitConnectionExample(
    modifier: Modifier = Modifier,
    onConnected: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()
    
    // Get GoogleFitManager instance
    val googleFitManager = remember { GoogleFitManager.getInstance(context) }
    
    // Create ViewModel
    val viewModel = remember { GoogleFitViewModel(googleFitManager) }
    
    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    // Activity result launcher for handling Google Fit responses
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        activity?.let { act ->
            viewModel.handleActivityResult(
                act,
                result.data?.extras?.getInt("requestCode") ?: 0,
                result.resultCode,
                result.data
            )
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Connection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isConnected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Google Fit",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Connection status
                Text(
                    text = when (connectionState) {
                        GoogleFitManager.ConnectionState.CONNECTED -> "Connected ✓"
                        GoogleFitManager.ConnectionState.CONNECTING -> "Connecting..."
                        GoogleFitManager.ConnectionState.AWAITING_PERMISSIONS -> "Awaiting permissions..."
                        GoogleFitManager.ConnectionState.ERROR -> "Connection error"
                        else -> "Not connected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (connectionState) {
                        GoogleFitManager.ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                        GoogleFitManager.ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                // Show today's stats if connected
                if (uiState.isConnected) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.todaySteps}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Steps",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%.1f".format(uiState.todayDistance / 1000),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "km",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.todayCalories}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Cal",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action button
                Button(
                    onClick = {
                        when (connectionState) {
                            GoogleFitManager.ConnectionState.CONNECTED -> {
                                // Sync data
                                viewModel.syncData()
                            }
                            GoogleFitManager.ConnectionState.DISCONNECTED,
                            GoogleFitManager.ConnectionState.ERROR -> {
                                // Connect
                                activity?.let { act ->
                                    viewModel.connect(act)
                                }
                            }
                            else -> {
                                // Already connecting, do nothing
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isConnecting && !uiState.isSyncing
                ) {
                    Text(
                        text = when {
                            uiState.isSyncing -> "Syncing..."
                            uiState.isConnecting -> "Connecting..."
                            uiState.isConnected -> "Sync Now"
                            else -> "Connect Google Fit"
                        }
                    )
                }
                
                // Disconnect button if connected
                if (uiState.isConnected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.disconnect()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
        
        // Error message
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
        
        // Weekly data preview if connected
        if (uiState.isConnected && uiState.weeklyData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Last 7 Days",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.weeklyData.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day.date,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${day.steps} steps",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Call onConnected when connection is successful
    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) {
            onConnected()
        }
    }
}