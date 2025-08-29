package com.runningcoach.v2.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.presentation.theme.AppColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Race type enumeration with display names and typical distances
 */
enum class RaceType(val displayName: String, val distance: String, val minWeeks: Int) {
    FIVE_K("5K", "3.1 miles", 4),
    TEN_K("10K", "6.2 miles", 6),
    HALF_MARATHON("Half Marathon", "13.1 miles", 10),
    FULL_MARATHON("Marathon", "26.2 miles", 16)
}

/**
 * Experience level enumeration for training plan customization
 */
enum class ExperienceLevel(val displayName: String, val description: String) {
    BEGINNER("Beginner", "New to running or returning after a break"),
    INTERMEDIATE("Intermediate", "Running regularly, some race experience"),
    ADVANCED("Advanced", "Experienced runner with multiple races"),
    ELITE("Elite", "Competitive runner with performance goals")
}

/**
 * Data class representing training plan generation parameters
 */
data class PlanGenerationParams(
    val raceType: RaceType,
    val targetDate: LocalDate,
    val targetTime: String? = null,
    val experienceLevel: ExperienceLevel,
    val currentWeeklyMileage: String? = null
)

/**
 * Training plan generation dialog/bottom sheet component
 * Provides comprehensive settings for AI-powered plan generation
 *
 * @param isVisible Whether the dialog is currently visible
 * @param onDismiss Callback when dialog is dismissed
 * @param onGeneratePlan Callback when plan generation is requested
 * @param isGenerating Whether plan generation is in progress
 * @param modifier Modifier for the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanGenerationDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onGeneratePlan: (PlanGenerationParams) -> Unit,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Form state
    var selectedRaceType by remember { mutableStateOf(RaceType.FIVE_K) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var targetTime by remember { mutableStateOf("") }
    var selectedExperience by remember { mutableStateOf(ExperienceLevel.BEGINNER) }
    var currentMileage by remember { mutableStateOf("") }
    
    // UI state
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Validation
    val isFormValid by remember {
        derivedStateOf {
            selectedDate != null && 
            selectedDate!!.isAfter(LocalDate.now().plus(selectedRaceType.minWeeks.toLong(), ChronoUnit.WEEKS))
        }
    }
    
    // Date constraints
    val minDate = LocalDate.now().plus(selectedRaceType.minWeeks.toLong(), ChronoUnit.WEEKS)
    val maxDate = LocalDate.now().plus(6, ChronoUnit.MONTHS)
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier,
            containerColor = AppColors.Background,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .background(
                            AppColors.OnSurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = "Generate Training Plan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Create a personalized training plan tailored to your goals and experience level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Race Type Selection
                SectionHeader("Race Type")
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RaceType.values()) { raceType ->
                        RaceTypeChip(
                            raceType = raceType,
                            isSelected = selectedRaceType == raceType,
                            onClick = { 
                                selectedRaceType = raceType
                                // Reset date if it's too soon for the new race type
                                selectedDate?.let { date ->
                                    val newMinDate = LocalDate.now().plus(raceType.minWeeks.toLong(), ChronoUnit.WEEKS)
                                    if (date.isBefore(newMinDate)) {
                                        selectedDate = null
                                    }
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Target Date Selection
                SectionHeader("Target Date")
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = selectedDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                    onValueChange = { },
                    label = { Text("Race Date") },
                    placeholder = { Text("Select your race date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select date",
                            tint = AppColors.Primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    )
                )
                
                // Date validation message
                if (selectedDate != null && selectedDate!!.isBefore(minDate)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Race date must be at least ${selectedRaceType.minWeeks} weeks from now",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Error
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Target Time (Optional)
                SectionHeader("Target Time (Optional)")
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = targetTime,
                    onValueChange = { targetTime = it },
                    label = { Text("Goal Time") },
                    placeholder = { Text(getTargetTimePlaceholder(selectedRaceType)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Experience Level Selection
                SectionHeader("Experience Level")
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExperienceLevel.values().forEach { level ->
                        ExperienceLevelCard(
                            level = level,
                            isSelected = selectedExperience == level,
                            onClick = { selectedExperience = level }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Current Weekly Mileage (Optional)
                SectionHeader("Current Weekly Mileage (Optional)")
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = currentMileage,
                    onValueChange = { input -> currentMileage = input.filter { it.isDigit() } },
                    label = { Text("Miles per Week") },
                    placeholder = { Text("20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    )
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.OnSurface
                        ),
                        border = null
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            selectedDate?.let { date ->
                                val params = PlanGenerationParams(
                                    raceType = selectedRaceType,
                                    targetDate = date,
                                    targetTime = targetTime.takeIf { it.isNotBlank() },
                                    experienceLevel = selectedExperience,
                                    currentWeeklyMileage = currentMileage.takeIf { it.isNotBlank() }
                                )
                                onGeneratePlan(params)
                            }
                        },
                        enabled = isFormValid && !isGenerating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = AppColors.OnPrimary
                        )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AppColors.OnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Generate Plan")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            selectedDate = date
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = AppColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = AppColors.OnSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Race Date",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Section header component
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.OnSurface
    )
}

/**
 * Race type selection chip
 */
@Composable
private fun RaceTypeChip(
    raceType: RaceType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = raceType.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = raceType.distance,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) AppColors.OnPrimary else AppColors.OnSurfaceVariant
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.Primary,
            selectedLabelColor = AppColors.OnPrimary,
            containerColor = AppColors.Surface,
            labelColor = AppColors.OnSurface
        )
    )
}

/**
 * Experience level selection card
 */
@Composable
private fun ExperienceLevelCard(
    level: ExperienceLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) AppColors.Primary.copy(alpha = 0.1f) 
                else AppColors.Surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AppColors.Primary else AppColors.CardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = level.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) AppColors.Primary else AppColors.OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = level.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.OnSurfaceVariant
            )
        }
    }
}

/**
 * Get placeholder text for target time based on race type
 */
private fun getTargetTimePlaceholder(raceType: RaceType): String {
    return when (raceType) {
        RaceType.FIVE_K -> "25:00"
        RaceType.TEN_K -> "50:00"
        RaceType.HALF_MARATHON -> "1:45:00"
        RaceType.FULL_MARATHON -> "3:30:00"
    }
}