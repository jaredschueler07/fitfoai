package com.runningcoach.v2.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.presentation.components.icons.GoogleFitIcon
import com.runningcoach.v2.presentation.theme.AppColors

/**
 * Data source enumeration for the SourceBadge component
 */
enum class AppDataSource {
    FITFOAI,
    GOOGLE_FIT
}

/**
 * Badge variant enumeration for styling
 */
enum class BadgeVariant {
    FILLED,
    OUTLINED
}

/**
 * Reusable badge component to show data source (FITFOAI vs Google Fit)
 * Following Material 3 design guidelines with athletic blue theme
 *
 * @param source The data source to display
 * @param variant Badge styling variant (filled or outlined)
 * @param showIcon Whether to show the source icon
 * @param modifier Modifier for the badge
 */
@Composable
fun SourceBadge(
    source: AppDataSource,
    variant: BadgeVariant = BadgeVariant.FILLED,
    showIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sourceInfo = when (source) {
        AppDataSource.FITFOAI -> SourceInfo(
            label = "FITFOAI",
            backgroundColor = if (variant == BadgeVariant.FILLED) AppColors.Primary else Color.Transparent,
            textColor = if (variant == BadgeVariant.FILLED) AppColors.OnPrimary else AppColors.Primary,
            borderColor = AppColors.Primary,
            contentDesc = "Data from FITFOAI"
        )
        AppDataSource.GOOGLE_FIT -> SourceInfo(
            label = "Google Fit",
            backgroundColor = if (variant == BadgeVariant.FILLED) GoogleFitColors.Primary else Color.Transparent,
            textColor = if (variant == BadgeVariant.FILLED) Color.White else GoogleFitColors.Primary,
            borderColor = GoogleFitColors.Primary,
            contentDesc = "Data from Google Fit"
        )
    }

    Surface(
        modifier = modifier
            .height(20.dp)
            .semantics { contentDescription = sourceInfo.contentDesc },
        shape = RoundedCornerShape(4.dp),
        color = sourceInfo.backgroundColor,
        border = if (variant == BadgeVariant.OUTLINED) 
            BorderStroke(1.dp, sourceInfo.borderColor) else null
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon && source == AppDataSource.GOOGLE_FIT) {
                GoogleFitIcon(
                    modifier = Modifier.size(12.dp),
                    tint = sourceInfo.textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                text = sourceInfo.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = sourceInfo.textColor
            )
        }
    }
}

/**
 * Compact version of SourceBadge for use in list items and tight spaces
 */
@Composable
fun CompactSourceBadge(
    source: AppDataSource,
    modifier: Modifier = Modifier
) {
    SourceBadge(
        source = source,
        variant = BadgeVariant.FILLED,
        showIcon = false,
        modifier = modifier
    )
}

/**
 * Data class to hold source styling information
 */
private data class SourceInfo(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val contentDesc: String
)

/**
 * Google Fit brand colors for authentic representation
 */
private object GoogleFitColors {
    val Primary = Color(0xFF4285F4)    // Google Blue
    val Secondary = Color(0xFF34A853)  // Google Green
}