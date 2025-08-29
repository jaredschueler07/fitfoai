package com.runningcoach.v2.presentation.screen.apitesting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.data.service.GoogleFitService
import com.runningcoach.v2.presentation.components.PrimaryButton
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleFitTestScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var testResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Not tested") }
    
    val googleFitService = remember { GoogleFitService(context) }
    
    // Observe connection status
    val isConnected by googleFitService.isConnected.collectAsState()
    val serviceConnectionStatus by googleFitService.connectionStatus.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("← Back", color = AppColors.Primary)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Text(
            text = "Google Fit API Test",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        // Connection Status Card
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            backgroundColor = if (isConnected) AppColors.Primary.copy(alpha = 0.1f) else AppColors.CardBackground,
            borderColor = if (isConnected) AppColors.Primary else AppColors.CardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Connection Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connected: $isConnected",
                    color = if (isConnected) AppColors.Primary else MaterialTheme.colorScheme.error
                )
                Text(text = "Status: $serviceConnectionStatus")
            }
        }
        
        // Test Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(
                text = "Check Connection",
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            googleFitService.checkConnectionStatus()
                            connectionStatus = "Connection check completed"
                        } catch (e: Exception) {
                            connectionStatus = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            
            PrimaryButton(
                text = "Test APIs",
                onClick = {
                    scope.launch {
                        isLoading = true
                        val results = mutableListOf<String>()
                        
                        try {
                            results.add("=== GOOGLE FIT API TEST RESULTS ===")
                            results.add("")
                            
                            // Test connection first
                            googleFitService.checkConnectionStatus()
                            results.add("✅ Connection Status: ${googleFitService.connectionStatus.value}")
                            results.add("✅ Is Connected: ${googleFitService.isConnected.value}")
                            results.add("")
                            
                            if (googleFitService.isConnected.value) {
                                // Test user profile data
                                results.add("--- User Profile Data ---")
                                val profileResult = googleFitService.getUserProfileData()
                                if (profileResult.isSuccess) {
                                    val profile = profileResult.getOrNull()
                                    results.add("✅ Name: ${profile?.name ?: "Not available"}")
                                    results.add("✅ Email: ${profile?.email ?: "Not available"}")
                                    results.add("✅ Weight: ${profile?.weightImperial ?: "Not available"}")
                                    results.add("✅ Height: ${profile?.heightImperial ?: "Not available"}")
                                } else {
                                    results.add("❌ Profile data failed: ${profileResult.exceptionOrNull()?.message}")
                                }
                                results.add("")
                                
                                // Test fitness data
                                results.add("--- Fitness Data ---")
                                val stepsResult = googleFitService.getDailySteps()
                                if (stepsResult.isSuccess) {
                                    results.add("✅ Daily Steps: ${stepsResult.getOrNull()} steps")
                                } else {
                                    results.add("❌ Steps failed: ${stepsResult.exceptionOrNull()?.message}")
                                }
                                
                                val weightResult = googleFitService.getLatestWeight()
                                if (weightResult.isSuccess) {
                                    results.add("✅ Latest Weight: ${weightResult.getOrNull()} kg")
                                } else {
                                    results.add("❌ Weight failed: ${weightResult.exceptionOrNull()?.message}")
                                }
                                
                                val heightResult = googleFitService.getLatestHeight()
                                if (heightResult.isSuccess) {
                                    results.add("✅ Latest Height: ${heightResult.getOrNull()} m")
                                } else {
                                    results.add("❌ Height failed: ${heightResult.exceptionOrNull()?.message}")
                                }
                                results.add("")
                                
                                // Test comprehensive data
                                results.add("--- Comprehensive Fitness Data ---")
                                val comprehensiveResult = googleFitService.getComprehensiveFitnessData()
                                if (comprehensiveResult.isSuccess) {
                                    val data = comprehensiveResult.getOrNull()
                                    results.add("✅ Steps: ${data?.steps}")
                                    results.add("✅ Distance: ${String.format("%.2f", data?.distance?.div(1000) ?: 0f)} km")
                                    results.add("✅ Calories: ${data?.calories}")
                                    results.add("✅ Heart Rate: ${data?.heartRate ?: "N/A"} BPM")
                                } else {
                                    results.add("❌ Comprehensive data failed: ${comprehensiveResult.exceptionOrNull()?.message}")
                                }
                                
                            } else {
                                results.add("❌ Not connected to Google Fit")
                                results.add("Please connect to Google Fit first from the Connect Apps screen")
                            }
                            
                        } catch (e: Exception) {
                            results.add("❌ Test failed with exception: ${e.message}")
                            results.add("Stack trace: ${e.stackTraceToString()}")
                        }
                        
                        testResults = results
                        isLoading = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        }
        
        // Test Results
        if (testResults.isNotEmpty()) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = AppColors.CardBackground
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(testResults) { result ->
                        Text(
                            text = result,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = when {
                                result.startsWith("✅") -> AppColors.Primary
                                result.startsWith("❌") -> MaterialTheme.colorScheme.error
                                result.startsWith("---") -> MaterialTheme.colorScheme.primary
                                result.startsWith("===") -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}