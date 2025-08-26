package com.runningcoach.v2.presentation.screen.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.domain.model.PopularRaces
import com.runningcoach.v2.domain.model.RaceGoal
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.PrimaryButton
import com.runningcoach.v2.presentation.components.SecondaryButton
import com.runningcoach.v2.presentation.components.icons.ChevronRightIcon
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun SetEventGoalScreen(
    onComplete: (RaceGoal?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRace by remember { mutableStateOf<RaceGoal?>(null) }
    var showCustomRaceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Set Your Event Goal",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnBackground,
            modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
        )
        
        Text(
            text = "Choose a race to train for, or skip to set general fitness goals",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Neutral400,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Popular Races Section
        Text(
            text = "Popular Races",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(PopularRaces.all) { race ->
                RaceSelectionCard(
                    race = race,
                    isSelected = selectedRace?.id == race.id,
                    onSelect = { selectedRace = race }
                )
            }
            
            // Custom Race Option
            item {
                AppCard(
                    backgroundColor = AppColors.CardBackground,
                    borderColor = AppColors.CardBorder,
                    onClick = { showCustomRaceDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Create Custom Race",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.OnSurface
                            )
                            
                            Text(
                                text = "Set your own race distance and date",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Neutral400
                            )
                        }
                        
                        ChevronRightIcon(tint = AppColors.Neutral500)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Bottom buttons
        Column {
            if (selectedRace != null) {
                PrimaryButton(
                    text = "Continue with ${selectedRace!!.name}",
                    onClick = { onComplete(selectedRace) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            SecondaryButton(
                text = "Skip - Set General Fitness Goals",
                onClick = { onComplete(null) }
            )
        }
    }
    
    // Custom Race Dialog (placeholder for now)
    if (showCustomRaceDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRaceDialog = false },
            title = { Text("Custom Race") },
            text = { Text("Custom race creation coming soon!") },
            confirmButton = {
                TextButton(
                    onClick = { showCustomRaceDialog = false }
                ) {
                    Text("OK", color = AppColors.Primary)
                }
            }
        )
    }
}

@Composable
private fun RaceSelectionCard(
    race: RaceGoal,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        backgroundColor = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else AppColors.CardBackground,
        borderColor = if (isSelected) AppColors.Primary else AppColors.CardBorder,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = race.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) AppColors.Primary else AppColors.OnSurface
                )
                
                Text(
                    text = "${race.distance.displayName} • ${race.location ?: "Various Locations"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) AppColors.Primary else AppColors.Neutral400,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                race.raceDate?.let { date ->
                    Text(
                        text = "Date: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) AppColors.Primary else AppColors.Neutral500,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            ChevronRightIcon(
                tint = if (isSelected) AppColors.Primary else AppColors.Neutral500
            )
        }
    }
}
