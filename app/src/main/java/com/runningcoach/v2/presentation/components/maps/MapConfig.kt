package com.runningcoach.v2.presentation.components.maps

import androidx.compose.runtime.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

// [ARCH-CHANGE] Map configuration optimization utilities
// - Centralized map configuration management
// - Pre-computed settings for different map types
// - Cached style configurations to avoid JSON parsing overhead

object MapConfig {
    
    // [BUILD-ISSUE] Performance optimization: Pre-computed map styles for different contexts
    private val DEFAULT_MAP_STYLE = """
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
    
    // [BUILD-ISSUE] Performance optimization: Lightweight style for compact maps
    private val COMPACT_MAP_STYLE = """
        [
            {
                "featureType": "all",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "all",
                "elementType": "geometry",
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
    
    // Cached map style instances
    private var cachedDefaultStyle: MapStyleOptions? = null
    private var cachedCompactStyle: MapStyleOptions? = null
    
    /**
     * Get optimized map style for full tracking map
     * [BUILD-ISSUE] Performance optimization: Cache to avoid JSON parsing on every composition
     */
    fun getDefaultMapStyle(): MapStyleOptions {
        if (cachedDefaultStyle == null) {
            cachedDefaultStyle = MapStyleOptions(DEFAULT_MAP_STYLE)
        }
        return cachedDefaultStyle!!
    }
    
    /**
     * Get optimized map style for compact preview maps
     * [BUILD-ISSUE] Performance optimization: Simplified style with hidden labels for better performance
     */
    fun getCompactMapStyle(): MapStyleOptions {
        if (cachedCompactStyle == null) {
            cachedCompactStyle = MapStyleOptions(COMPACT_MAP_STYLE)
        }
        return cachedCompactStyle!!
    }
    
    /**
     * Get default UI settings for tracking maps
     * [BUILD-ISSUE] Performance optimization: Pre-configured settings to avoid recreation
     */
    fun getDefaultUiSettings() = MapUiSettings(
        compassEnabled = true,
        myLocationButtonEnabled = false,
        zoomControlsEnabled = false,
        mapToolbarEnabled = false,
        // Enable gestures for interactive maps
        scrollGesturesEnabled = true,
        zoomGesturesEnabled = true,
        rotationGesturesEnabled = true,
        tiltGesturesEnabled = false // Disable for performance
    )
    
    /**
     * Get minimal UI settings for compact maps
     * [BUILD-ISSUE] Performance optimization: Disable all interactions for preview maps
     */
    fun getCompactUiSettings() = MapUiSettings(
        compassEnabled = false,
        myLocationButtonEnabled = false,
        zoomControlsEnabled = false,
        mapToolbarEnabled = false,
        // Disable all gestures for compact maps
        scrollGesturesEnabled = false,
        zoomGesturesEnabled = false,
        rotationGesturesEnabled = false,
        tiltGesturesEnabled = false
    )
    
    /**
     * Get optimized map properties for tracking maps
     * [BUILD-ISSUE] Performance optimization: Pre-configured properties with location awareness
     */
    fun getTrackingMapProperties(hasLocation: Boolean) = MapProperties(
        mapType = MapType.NORMAL,
        isMyLocationEnabled = hasLocation,
        mapStyleOptions = getDefaultMapStyle(),
        // Performance optimizations
        isTrafficEnabled = false,
        isIndoorEnabled = false,
        minZoomPreference = 10f,
        maxZoomPreference = 20f
    )
    
    /**
     * Get optimized map properties for compact maps
     * [BUILD-ISSUE] Performance optimization: Minimal settings for preview maps
     */
    fun getCompactMapProperties() = MapProperties(
        mapType = MapType.NORMAL,
        isMyLocationEnabled = false,
        mapStyleOptions = getCompactMapStyle(),
        // Minimal features for better performance
        isTrafficEnabled = false,
        isIndoorEnabled = false,
        minZoomPreference = 12f,
        maxZoomPreference = 18f
    )
    
    /**
     * Camera configuration for different map contexts
     * [BUILD-ISSUE] Performance optimization: Pre-defined zoom levels for different use cases
     */
    object CameraConfig {
        const val TRACKING_ZOOM = 17f
        const val COMPACT_ZOOM = 15f
        const val ROUTE_OVERVIEW_ZOOM = 14f
        const val ANIMATION_DURATION = 800 // Reduced from 1000ms for snappier feel
    }
}

/**
 * Composable utilities for optimized map creation
 */
object MapComposables {
    
    /**
     * Create optimized tracking map properties
     * [BUILD-ISSUE] Performance optimization: Memoized properties with location dependency
     */
    @Composable
    fun rememberTrackingMapProperties(hasLocation: Boolean): MapProperties {
        return remember(hasLocation) {
            MapConfig.getTrackingMapProperties(hasLocation)
        }
    }
    
    /**
     * Create optimized compact map properties
     * [BUILD-ISSUE] Performance optimization: Fully cached properties for preview maps
     */
    @Composable
    fun rememberCompactMapProperties(): MapProperties {
        return remember {
            MapConfig.getCompactMapProperties()
        }
    }
    
    /**
     * Create cached UI settings for tracking maps
     */
    @Composable
    fun rememberTrackingUiSettings(): MapUiSettings {
        return remember {
            MapConfig.getDefaultUiSettings()
        }
    }
    
    /**
     * Create cached UI settings for compact maps
     */
    @Composable
    fun rememberCompactUiSettings(): MapUiSettings {
        return remember {
            MapConfig.getCompactUiSettings()
        }
    }
}