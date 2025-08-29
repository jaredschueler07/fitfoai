package com.runningcoach.v2.presentation.components.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Icon components based on wireframe reference
@Composable
fun HomeIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Home,
        contentDescription = "Home",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun ChatIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Person, // Using Person as placeholder for chat
        contentDescription = "Chat",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun ChartIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Settings, // Using Settings as placeholder for chart
        contentDescription = "Chart",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun ProfileIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Profile",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun PlusIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Plus",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun GoogleFitIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Settings, // Using Settings as placeholder
        contentDescription = "Google Fit",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun SpotifyIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Settings, // Using Settings as placeholder
        contentDescription = "Spotify",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.KeyboardArrowRight,
        contentDescription = "Chevron Right",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun SendIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Send,
        contentDescription = "Send",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun SettingsIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun CloseIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Close",
        modifier = modifier,
        tint = tint
    )
}
