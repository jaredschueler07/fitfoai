package com.runningcoach.v2.presentation.screen.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.presentation.components.PrimaryButton
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top spacer to push content down
        Spacer(modifier = Modifier.height(80.dp))
        
        // Hero section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero image placeholder (circular like wireframe)
            Box(
                modifier = Modifier
                    .size(192.dp)
                    .clip(CircleShape)
                    .background(AppColors.Neutral700),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder for runner image
                Text(
                    text = "🏃‍♀️",
                    style = MaterialTheme.typography.displayLarge,
                    color = AppColors.OnSurface
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App title
            Text(
                text = "RunningCoach",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "Your Personal Running Companion",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Neutral400,
                textAlign = TextAlign.Center
            )
        }
        
        // Bottom section with CTA
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Start your fitness journey today!",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.Neutral300,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            PrimaryButton(
                text = "Get Started",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
