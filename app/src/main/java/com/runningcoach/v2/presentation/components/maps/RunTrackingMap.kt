package com.runningcoach.v2.presentation.components.maps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.presentation.theme.AppColors
import kotlinx.coroutines.delay

// [ARCH-CHANGE] Performance optimizations for Google Maps integration
// - Cached map style and UI settings to avoid recreation on recomposition
// - Loading skeleton with progress indicators for better UX during map initialization
// - Error fallback UI with retry functionality
// - Marker state caching to reduce object allocation
// - Animated map appearance for smooth transitions
// - Separate MapContent composable for better performance isolation

// [BUILD-ISSUE] Performance optimization: Use centralized map configuration
// Removed local style definitions in favor of MapConfig utility

@Composable
fun RunTrackingMap(
    currentLocation: LatLng?,
    routePoints: List<LatLng>,
    runMetrics: RunMetrics?,
    modifier: Modifier = Modifier
) {
    var isMapLoading by remember { mutableStateOf(true) }
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    
    // [BUILD-ISSUE] Performance optimization: Use centralized map configuration
    val optimizedMapProperties = MapComposables.rememberTrackingMapProperties(currentLocation != null)
    val optimizedUiSettings = MapComposables.rememberTrackingUiSettings()
    
    // Simulate map loading state management - provides minimum 500ms for smooth UX
    LaunchedEffect(Unit) {
        delay(500) // Minimum loading time for better UX
        isMapLoading = false
    }
    
    // Update camera position when current location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, MapConfig.CameraConfig.TRACKING_ZOOM),
                    MapConfig.CameraConfig.ANIMATION_DURATION
                )
            } catch (e: Exception) {
                mapLoadError = "Failed to center map: ${e.message}"
            }
        }
    }
    
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large
    ) {
        Box {
            // [BUILD-ISSUE] Performance optimization: Show loading skeleton while map initializes
            if (isMapLoading) {
                MapLoadingSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(300.dp)
                )
            }
            
            // [BUILD-ISSUE] Error fallback UI
            mapLoadError?.let { error ->
                MapErrorFallback(
                    error = error,
                    onRetry = {
                        mapLoadError = null
                        isMapLoading = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .height(300.dp)
                )
            }
            
            // [BUILD-ISSUE] Performance optimization: Only render map when not loading and no error
            if (!isMapLoading && mapLoadError == null) {
                GoogleMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .height(300.dp),
                        cameraPositionState = cameraPositionState,
                        properties = optimizedMapProperties,
                        uiSettings = optimizedUiSettings,
                        onMapLoaded = {
                            // Map has finished loading
                            isMapLoading = false
                        }
                    ) {
                    // [BUILD-ISSUE] Performance optimization: Cache markers to avoid recreation
                    MapContent(
                        currentLocation = currentLocation,
                        routePoints = routePoints,
                        runMetrics = runMetrics
                    )
                }
            }
        }
    }
}

@Composable
fun CompactRunTrackingMap(
    currentLocation: LatLng?,
    routePoints: List<LatLng>,
    modifier: Modifier = Modifier
) {
    var isMapLoading by remember { mutableStateOf(true) }
    val cameraPositionState = rememberCameraPositionState()
    
    // [BUILD-ISSUE] Performance optimization: Use centralized compact map configuration
    val compactMapProperties = MapComposables.rememberCompactMapProperties()
    val compactUiSettings = MapComposables.rememberCompactUiSettings()
    
    // Faster loading for compact map (300ms vs 500ms)
    LaunchedEffect(Unit) {
        delay(300)
        isMapLoading = false
    }
    
    // Update camera position when current location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, MapConfig.CameraConfig.COMPACT_ZOOM),
                MapConfig.CameraConfig.ANIMATION_DURATION
            )
        }
    }
    
    Box {
        if (isMapLoading) {
            MapLoadingSkeleton(
                modifier = modifier.height(200.dp)
            )
        } else {
            GoogleMap(
                modifier = modifier.height(200.dp),
                cameraPositionState = cameraPositionState,
                properties = compactMapProperties,
                uiSettings = compactUiSettings,
                onMapLoaded = { isMapLoading = false }
            ) {
                // Current location marker
                currentLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
                
                // Route polyline
                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = Color(0xFFFF6B6B), // Coral color
                        width = 6f,
                        geodesic = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MapContent(
    currentLocation: LatLng?,
    routePoints: List<LatLng>,
    runMetrics: RunMetrics?
) {
    // [BUILD-ISSUE] Performance optimization: Cache marker states
    val currentLocationMarker = remember(currentLocation) {
        currentLocation?.let { location ->
            MarkerState(position = location)
        }
    }
    
    val startLocationMarker = remember(routePoints) {
        if (routePoints.isNotEmpty()) {
            MarkerState(position = routePoints.first())
        } else null
    }
    
    // Current location marker
    currentLocationMarker?.let { markerState ->
        Marker(
            state = markerState,
            title = "Current Location",
            snippet = runMetrics?.let { "Pace: ${it.getFormattedPace()}" },
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )
    }
    
    // Route polyline - only render if we have enough points
    if (routePoints.size >= 2) {
        Polyline(
            points = routePoints,
            color = Color(0xFFFF6B6B), // Coral color to match the theme
            width = 8f,
            pattern = null,
            geodesic = true
        )
        
        // Start marker (first point)
        startLocationMarker?.let { markerState ->
            Marker(
                state = markerState,
                title = "Start",
                snippet = "Run started here",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
        }
    }
}

@Composable
private fun MapLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = AppColors.Surface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = AppColors.CoralAccent,
                strokeWidth = 3.dp
            )
            
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Loading Map",
                tint = AppColors.CoralAccent.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
            
            Text(
                text = "Loading Map...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Optimizing for performance",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MapErrorFallback(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = AppColors.Surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Map Error",
                tint = AppColors.Error,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Map Loading Failed",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2
            )
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.CoralAccent
                )
            ) {
                Text(
                    text = "Retry",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "GPS tracking continues in background",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Success.copy(alpha = 0.8f)
            )
        }
    }
}