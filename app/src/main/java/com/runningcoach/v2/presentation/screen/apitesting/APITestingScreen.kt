package com.runningcoach.v2.presentation.screen.apitesting

import androidx.compose.foundation.background
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.BuildConfig
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.PrimaryButton
import com.runningcoach.v2.presentation.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class APITestResult(
    val name: String,
    val status: APIStatus,
    val message: String,
    val details: String = ""
)

enum class APIStatus {
    PENDING, TESTING, SUCCESS, FAILED, NOT_CONFIGURED
}

@Composable
fun APITestingScreen(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var testResults by remember { mutableStateOf<List<APITestResult>>(emptyList()) }
    var isTestingAll by remember { mutableStateOf(false) }
    
    // Create shared HTTP client and services
    val httpClient = remember {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json()
            }
        }
    }
    
    val geminiService = remember { com.runningcoach.v2.data.service.GeminiService(httpClient) }
    val elevenLabsService = remember { com.runningcoach.v2.data.service.ElevenLabsService(httpClient, context) }
    val googleFitService = remember { com.runningcoach.v2.data.service.GoogleFitService(context) }
    
    val apiTests = listOf(
        "Google Gemini AI",
        "ElevenLabs TTS",
        "Google Maps",
        "Spotify Web API",
        "Google Fit API"
    )
    
    LaunchedEffect(Unit) {
        // Initialize test results
        testResults = apiTests.map { apiName ->
            APITestResult(
                name = apiName,
                status = APIStatus.PENDING,
                message = "Ready to test"
            )
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "API Connection Testing",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnBackground,
            modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
        )
        
        Text(
            text = "Test all API connections to ensure proper configuration",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Neutral400,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Test All Button
        PrimaryButton(
            text = if (isTestingAll) "Testing..." else "Test All APIs",
            onClick = {
                if (!isTestingAll) {
                        scope.launch {
        testAllAPIs(geminiService, elevenLabsService, googleFitService) { results ->
            testResults = results
        }
    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTestingAll
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // API Configuration Check
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "API Key Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                APIKeyStatusItem("Gemini API Key", BuildConfig.GEMINI_API_KEY)
                APIKeyStatusItem("ElevenLabs API Key", BuildConfig.ELEVENLABS_API_KEY)
                APIKeyStatusItem("Google Maps API Key", BuildConfig.GOOGLE_MAPS_API_KEY)
                APIKeyStatusItem("Spotify Client ID", BuildConfig.SPOTIFY_CLIENT_ID)
                APIKeyStatusItem("Spotify Client Secret", BuildConfig.SPOTIFY_CLIENT_SECRET)
                APIKeyStatusItem("Google Fit Client ID", BuildConfig.GOOGLE_FIT_CLIENT_ID)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test Results
        Text(
            text = "Test Results",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(testResults) { result ->
                APITestResultCard(
                    result = result,
                    onTestIndividual = { apiName ->
                        scope.launch {
                            testIndividualAPI(apiName, geminiService, elevenLabsService, googleFitService) { updatedResult ->
                                testResults = testResults.map { existing ->
                                    if (existing.name == apiName) updatedResult else existing
                                }
                            }
                        }
                    }
                )
            }
        }
    }
    
    // Update isTestingAll based on test results
    LaunchedEffect(testResults) {
        isTestingAll = testResults.any { it.status == APIStatus.TESTING }
    }
}

@Composable
private fun APIKeyStatusItem(name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.OnSurface,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (value.isNotBlank()) Color.Green else Color.Red,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (value.isNotBlank()) "Configured" else "Missing",
                style = MaterialTheme.typography.bodySmall,
                color = if (value.isNotBlank()) Color.Green else Color.Red
            )
        }
    }
}

@Composable
private fun APITestResultCard(
    result: APITestResult,
    onTestIndividual: (String) -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(result.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Neutral400
                    )
                }
                
                if (result.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Neutral500
                    )
                }
            }
            
            if (result.status != APIStatus.TESTING) {
                Button(
                    onClick = { onTestIndividual(result.name) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary.copy(alpha = 0.1f),
                        contentColor = AppColors.Primary
                    )
                ) {
                    Text("Test", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AppColors.Primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(status: APIStatus) {
    val color = when (status) {
        APIStatus.PENDING -> AppColors.Neutral500
        APIStatus.TESTING -> AppColors.Primary
        APIStatus.SUCCESS -> Color.Green
        APIStatus.FAILED -> Color.Red
        APIStatus.NOT_CONFIGURED -> Color(0xFFFF9800) // Orange
    }
    
    Text(
        text = "●",
        color = color,
        style = MaterialTheme.typography.titleMedium
    )
}

private suspend fun testAllAPIs(
    geminiService: com.runningcoach.v2.data.service.GeminiService,
    elevenLabsService: com.runningcoach.v2.data.service.ElevenLabsService,
    googleFitService: com.runningcoach.v2.data.service.GoogleFitService,
    onResults: (List<APITestResult>) -> Unit
) {
    val apiNames = listOf(
        "Google Gemini AI",
        "ElevenLabs TTS", 
        "Google Maps",
        "Spotify Web API",
        "Google Fit API"
    )
    
    val initialResults = apiNames.map { name ->
        APITestResult(name, APIStatus.TESTING, "Testing connection...")
    }
    onResults(initialResults)
    
    val finalResults = mutableListOf<APITestResult>()
    
    for (apiName in apiNames) {
        val result = performAPITest(apiName, geminiService, elevenLabsService, googleFitService)
        finalResults.add(result)
        
        // Update results incrementally
        val currentResults = apiNames.map { name ->
            finalResults.find { it.name == name } ?: APITestResult(
                name = name,
                status = if (finalResults.any { it.name == name }) APIStatus.PENDING else APIStatus.TESTING,
                message = if (finalResults.any { it.name == name }) "Ready to test" else "Testing connection..."
            )
        }
        onResults(currentResults)
        
        delay(500) // Small delay between tests
    }
}

private suspend fun testIndividualAPI(
    apiName: String, 
    geminiService: com.runningcoach.v2.data.service.GeminiService,
    elevenLabsService: com.runningcoach.v2.data.service.ElevenLabsService,
    googleFitService: com.runningcoach.v2.data.service.GoogleFitService,
    onResult: (APITestResult) -> Unit
) {
    onResult(APITestResult(apiName, APIStatus.TESTING, "Testing connection..."))
    delay(100) // Small delay for UI update
    
    val result = performAPITest(apiName, geminiService, elevenLabsService, googleFitService)
    onResult(result)
}

private suspend fun performAPITest(
    apiName: String,
    geminiService: com.runningcoach.v2.data.service.GeminiService,
    elevenLabsService: com.runningcoach.v2.data.service.ElevenLabsService,
    googleFitService: com.runningcoach.v2.data.service.GoogleFitService
): APITestResult {
    return try {
        when (apiName) {
            "Google Gemini AI" -> testGeminiAPI(geminiService)
            "ElevenLabs TTS" -> testElevenLabsAPI(elevenLabsService)
            "Google Maps" -> testGoogleMapsAPI()
            "Spotify Web API" -> testSpotifyAPI()
            "Google Fit API" -> testGoogleFitAPI(googleFitService)
            else -> APITestResult(
                name = apiName,
                status = APIStatus.FAILED,
                message = "Unknown API"
            )
        }
    } catch (e: Exception) {
        APITestResult(
            name = apiName,
            status = APIStatus.FAILED,
            message = "Test failed",
            details = e.localizedMessage ?: "Unknown error"
        )
    }
}

private suspend fun testGeminiAPI(geminiService: com.runningcoach.v2.data.service.GeminiService): APITestResult {
    return if (BuildConfig.GEMINI_API_KEY.isBlank()) {
        APITestResult(
            name = "Google Gemini AI",
            status = APIStatus.NOT_CONFIGURED,
            message = "API key not configured"
        )
    } else {
        try {
            // Use the shared GeminiService from the composable
            val result = geminiService.testConnection()
            
            if (result.isSuccess) {
                APITestResult(
                    name = "Google Gemini AI",
                    status = APIStatus.SUCCESS,
                    message = "Connection successful",
                    details = result.getOrNull() ?: "API working"
                )
            } else {
                APITestResult(
                    name = "Google Gemini AI",
                    status = APIStatus.FAILED,
                    message = "Connection failed",
                    details = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            APITestResult(
                name = "Google Gemini AI",
                status = APIStatus.FAILED,
                message = "Test failed",
                details = e.localizedMessage ?: "Unknown error"
            )
        }
    }
}

private suspend fun testElevenLabsAPI(elevenLabsService: com.runningcoach.v2.data.service.ElevenLabsService): APITestResult {
    return if (BuildConfig.ELEVENLABS_API_KEY.isBlank()) {
        APITestResult(
            name = "ElevenLabs TTS",
            status = APIStatus.NOT_CONFIGURED,
            message = "API key not configured"
        )
    } else {
        try {
            // Use the shared ElevenLabsService
            val result = elevenLabsService.testConnection()
            
            if (result.isSuccess) {
                APITestResult(
                    name = "ElevenLabs TTS",
                    status = APIStatus.SUCCESS,
                    message = "Connection successful",
                    details = result.getOrNull() ?: "API working"
                )
            } else {
                APITestResult(
                    name = "ElevenLabs TTS",
                    status = APIStatus.FAILED,
                    message = "Connection failed",
                    details = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            APITestResult(
                name = "ElevenLabs TTS",
                status = APIStatus.FAILED,
                message = "Test failed",
                details = e.localizedMessage ?: "Unknown error"
            )
        }
    }
}

private suspend fun testGoogleMapsAPI(): APITestResult {
    delay(600)
    
    return if (BuildConfig.GOOGLE_MAPS_API_KEY.isBlank()) {
        APITestResult(
            name = "Google Maps",
            status = APIStatus.NOT_CONFIGURED,
            message = "API key not configured"
        )
    } else {
        APITestResult(
            name = "Google Maps",
            status = APIStatus.SUCCESS,
            message = "API key configured",
            details = "Key: ${BuildConfig.GOOGLE_MAPS_API_KEY.take(10)}..."
        )
    }
}

private suspend fun testSpotifyAPI(): APITestResult {
    delay(900)
    
    return if (BuildConfig.SPOTIFY_CLIENT_ID.isBlank() || BuildConfig.SPOTIFY_CLIENT_SECRET.isBlank()) {
        APITestResult(
            name = "Spotify Web API",
            status = APIStatus.NOT_CONFIGURED,
            message = "Client credentials not configured"
        )
    } else {
        APITestResult(
            name = "Spotify Web API",
            status = APIStatus.SUCCESS,
            message = "Client credentials configured",
            details = "Client ID: ${BuildConfig.SPOTIFY_CLIENT_ID.take(10)}..."
        )
    }
}

private suspend fun testGoogleFitAPI(googleFitService: com.runningcoach.v2.data.service.GoogleFitService): APITestResult {
    return try {
        // Check if Google Fit is connected
        val isConnected = googleFitService.isConnected.value
        
        if (isConnected) {
            // Try to get daily steps to test the API
            val stepsResult = googleFitService.getDailySteps()
            
            if (stepsResult.isSuccess) {
                val steps = stepsResult.getOrNull() ?: 0
                APITestResult(
                    name = "Google Fit API",
                    status = APIStatus.SUCCESS,
                    message = "Connected and working",
                    details = "Today's steps: $steps"
                )
            } else {
                APITestResult(
                    name = "Google Fit API",
                    status = APIStatus.FAILED,
                    message = "Connected but data access failed",
                    details = stepsResult.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                )
            }
        } else {
            APITestResult(
                name = "Google Fit API",
                status = APIStatus.FAILED,
                message = "Not connected to Google Fit",
                details = "Please connect your Google Fit account first"
            )
        }
    } catch (e: Exception) {
        APITestResult(
            name = "Google Fit API",
            status = APIStatus.FAILED,
            message = "Test failed",
            details = e.localizedMessage ?: "Unknown error"
        )
    }
}
