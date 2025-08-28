package com.runningcoach.v2.presentation.components.maps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.runningcoach.v2.domain.model.RunMetrics
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun RunTrackingMap(
    currentLocation: LatLng?,
    routePoints: List<LatLng>,
    runMetrics: RunMetrics?,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()
    
    // Update camera position when current location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 17f),
                1000 // Animation duration in milliseconds
            )
        }
    }
    
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large
    ) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .height(300.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = currentLocation != null,
                mapStyleOptions = MapStyleOptions(
                    // Dark mode friendly map style
                    """
                    [
                        {
                            "featureType": "all",
                            "elementType": "geometry",
                            "stylers": [{"color": "#1e3a5f"}]
                        },
                        {
                            "featureType": "all",
                            "elementType": "labels.text.fill",
                            "stylers": [{"color": "#ffffff"}]
                        },
                        {
                            "featureType": "all",
                            "elementType": "labels.text.stroke",
                            "stylers": [{"color": "#1e3a5f"}]
                        },
                        {
                            "featureType": "road",
                            "elementType": "geometry",
                            "stylers": [{"color": "#4a7c97"}]
                        },
                        {
                            "featureType": "water",
                            "elementType": "geometry",
                            "stylers": [{"color": "#0066cc"}]
                        }
                    ]
                    """.trimIndent()
                )
            ),
            uiSettings = MapUiSettings(
                compassEnabled = true,
                myLocationButtonEnabled = false, // We'll use our own button
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // Current location marker
            currentLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Current Location",
                    snippet = runMetrics?.let { "Pace: ${it.getFormattedPace()}" },
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
            
            // Route polyline
            if (routePoints.size >= 2) {
                Polyline(
                    points = routePoints,
                    color = Color(0xFFFF6B6B), // Coral color to match the theme
                    width = 8f,
                    pattern = null,
                    geodesic = true
                )
                
                // Start marker (first point)
                if (routePoints.isNotEmpty()) {
                    Marker(
                        state = MarkerState(position = routePoints.first()),
                        title = "Start",
                        snippet = "Run started here",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
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
    val cameraPositionState = rememberCameraPositionState()
    
    // Update camera position when current location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 16f),
                1000
            )
        }
    }
    
    GoogleMap(
        modifier = modifier.height(200.dp),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = false
        ),
        uiSettings = MapUiSettings(
            compassEnabled = false,
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false
        )
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