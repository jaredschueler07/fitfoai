package com.runningcoach.v2.presentation.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.util.Log
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.repository.UserRepository
import com.runningcoach.v2.data.service.GoogleFitService
import com.runningcoach.v2.domain.model.FitnessLevel
import com.runningcoach.v2.domain.model.RunningGoal
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.PrimaryButton
import com.runningcoach.v2.presentation.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizeProfileScreen(
    onComplete: (ProfileData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Repository setup
    val database = remember { FITFOAIDatabase.getDatabase(context) }
    val userRepository = remember { UserRepository(database) }
    val googleFitService = remember { GoogleFitService(context) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedFitnessLevel by remember { mutableStateOf(FitnessLevel.BEGINNER) }
    var selectedRunningGoals by remember { mutableStateOf(setOf<RunningGoal>()) }
    
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoadingFromGoogleFit by remember { mutableStateOf(false) }
    var autoFillAttempted by remember { mutableStateOf(false) }

    // Google Fit connection state
    val isGoogleFitConnected by googleFitService.isConnected.collectAsState()

    // Auto-fill from Google Fit when connected
    LaunchedEffect(isGoogleFitConnected) {
        if (isGoogleFitConnected && !autoFillAttempted) {
            autoFillAttempted = true
            isLoadingFromGoogleFit = true
            try {
                val profileResult = googleFitService.getUserProfileData()
                if (profileResult.isSuccess) {
                    val profileData = profileResult.getOrNull()
                    profileData?.let { data ->
                        data.name?.let { userName -> if (name.isBlank()) name = userName }
                        data.heightImperial?.let { if (height.isBlank()) height = it }
                        data.weightImperial?.let { w ->
                            if (weight.isBlank()) weight = w.filter { it.isDigit() }
                        }
                    }
                } else {
                    Log.w("PersonalizeProfile", "Failed to get Google Fit profile data: ${profileResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("PersonalizeProfile", "Error auto-filling from Google Fit", e)
            } finally {
                isLoadingFromGoogleFit = false
            }
        }
    }

    val scrollState = rememberScrollState()
    
    // Form validation - name is required, others can be filled manually if Google Fit fails
    val isFormValid = name.isNotBlank() && age.isNotBlank() && 
                     height.isNotBlank() && weight.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "Personalize Your Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnBackground,
            modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
        )
        
        Text(
            text = "Help us customize your training experience",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Neutral400,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Google Fit auto-fill status
        if (isGoogleFitConnected && isLoadingFromGoogleFit) {
            AppCard(
                modifier = Modifier.padding(bottom = 16.dp),
                backgroundColor = AppColors.Primary.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AppColors.Primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Loading profile data from Google Fit...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.Primary
                    )
                }
            }
        } else if (isGoogleFitConnected && autoFillAttempted) {
            AppCard(
                modifier = Modifier.padding(bottom = 16.dp),
                backgroundColor = AppColors.Primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (name.isNotBlank()) {
                        "✓ Profile auto-filled from Google Fit"
                    } else {
                        "Google Fit connected - manual entry required"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Basic Info Section
        AppCard(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    focusedLabelColor = AppColors.Primary,
                    cursorColor = AppColors.Primary
                )
            )
            
            // Age Field
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    focusedLabelColor = AppColors.Primary,
                    cursorColor = AppColors.Primary
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Height Dropdown (4'10" to 7'0")
                var heightExpanded by remember { mutableStateOf(false) }
                val heightOptions = remember {
                    buildList {
                        for (ft in 4..7) {
                            val startIn = if (ft == 4) 10 else 0
                            val endIn = if (ft == 7) 0 else 11
                            for (inch in startIn..endIn) {
                                add("${ft}'${inch}\"")
                            }
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = heightExpanded,
                    onExpandedChange = { heightExpanded = !heightExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Height") },
                        placeholder = { Text("Select height") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = heightExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            focusedLabelColor = AppColors.Primary,
                            cursorColor = AppColors.Primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = heightExpanded,
                        onDismissRequest = { heightExpanded = false }
                    ) {
                        heightOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    height = option
                                    heightExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Weight Field
                OutlinedTextField(
                    value = weight,
                    onValueChange = { input -> weight = input.filter { it.isDigit() } },
                    label = { Text("Weight (lbs)") },
                    placeholder = { Text("150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    )
                )
            }
        }

        // Fitness Level Section
        AppCard(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "Fitness Level",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            FitnessLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedFitnessLevel == level,
                        onClick = { selectedFitnessLevel = level },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AppColors.Primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = level.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = getFitnessLevelDescription(level),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Neutral400
                        )
                    }
                }
            }
        }

        // Running Goals Section
        AppCard(
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text(
                text = "Running Goals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Select all that apply",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Neutral400,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            RunningGoal.entries.forEach { goal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selectedRunningGoals.contains(goal),
                        onCheckedChange = { checked ->
                            selectedRunningGoals = if (checked) {
                                selectedRunningGoals + goal
                            } else {
                                selectedRunningGoals - goal
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AppColors.Primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getRunningGoalDisplayName(goal),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.OnSurface,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            AppCard(
                modifier = Modifier.padding(bottom = 16.dp),
                backgroundColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Continue Button
        PrimaryButton(
            text = if (isSaving) "Saving..." else "Continue",
            onClick = {
                scope.launch {
                    isSaving = true
                    errorMessage = null
                    
                    val profileData = ProfileData(
                        name = name,
                        age = age.toIntOrNull() ?: 0,
                        height = height,
                        weight = weight,
                        fitnessLevel = selectedFitnessLevel,
                        runningGoals = selectedRunningGoals.toList()
                    )
                    
                    // Save profile to database
                    val saveResult = userRepository.saveUserProfile(profileData)
                    
                    if (saveResult.isSuccess) {
                        // Mark profile as completed
                        val profileCompleteResult = userRepository.markProfileCompleted()
                        if (profileCompleteResult.isFailure) {
                            Log.w("PersonalizeProfile", "Failed to mark profile as completed: ${profileCompleteResult.exceptionOrNull()?.message}")
                        }
                        
                        // Profile saved successfully, proceed to next screen
                        onComplete(profileData)
                    } else {
                        // Show error message
                        errorMessage = "Failed to save profile: ${saveResult.exceptionOrNull()?.message}"
                    }
                    
                    isSaving = false
                }
            },
            enabled = isFormValid && !isSaving
        )
    }
}

data class ProfileData(
    val name: String,
    val age: Int,
    val height: String,
    val weight: String,
    val fitnessLevel: FitnessLevel,
    val runningGoals: List<RunningGoal>
)

private fun getFitnessLevelDescription(level: FitnessLevel): String {
    return when (level) {
        FitnessLevel.BEGINNER -> "New to running or returning after a break"
        FitnessLevel.INTERMEDIATE -> "Regular runner, comfortable with 3-5 miles"
        FitnessLevel.ADVANCED -> "Experienced runner, comfortable with 10+ miles"
        FitnessLevel.EXPERT -> "Competitive runner with extensive training experience"
    }
}

private fun getRunningGoalDisplayName(goal: RunningGoal): String {
    return when (goal) {
        RunningGoal.GENERAL_FITNESS -> "General Fitness"
        RunningGoal.WEIGHT_LOSS -> "Weight Loss"
        RunningGoal.ENDURANCE -> "Build Endurance"
        RunningGoal.SPEED -> "Improve Speed"
        RunningGoal.RACE_TRAINING -> "Race Training"
    }
}
