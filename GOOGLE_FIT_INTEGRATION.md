# Google Fit Integration Guide

This document describes the Google Fit integration implemented in the FITFO AI Android app.

## Overview

The app now uses Google Play Services Fitness API to read fitness data from Google Fit, including:
- Step count (daily and weekly)
- Heart rate data
- Weight and height measurements
- Distance and calories (estimated)

## Implementation Details

### 1. Dependencies

The following dependencies are already included in `app/build.gradle.kts`:
```kotlin
implementation(libs.google.play.services.fitness)
implementation(libs.google.play.services.auth)
```

### 2. Permissions

Required permissions added to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.BODY_SENSORS" />
```

### 3. Service Implementation

The main Google Fit service is located at:
`app/src/main/java/com/runningcoach/v2/data/service/GoogleFitService.kt`

Key features:
- **Authentication**: Uses Google Sign-In with Fitness scopes
- **Data Reading**: Implements methods to read various fitness data types
- **Error Handling**: Comprehensive error handling and logging
- **State Management**: Uses StateFlow for connection status

### 4. Data Types Supported

The service requests access to the following Google Fit data types:
- `TYPE_STEP_COUNT_DELTA` - Step count data
- `TYPE_HEART_RATE_BPM` - Heart rate measurements
- `TYPE_WEIGHT` - Weight measurements
- `TYPE_HEIGHT` - Height measurements
- `TYPE_DISTANCE_DELTA` - Distance data
- `TYPE_CALORIES_EXPENDED` - Calorie data

### 5. Key Methods

#### Connection Management
- `initiateConnection()`: Starts the Google Sign-In flow
- `checkConnectionStatus()`: Checks if user is connected and has permissions
- `requestPermissions(activity)`: Requests fitness permissions
- `handleActivityResult()`: Handles permission request results
- `disconnect()`: Signs out and disconnects

#### Data Reading
- `getDailySteps()`: Gets today's step count
- `getWeeklySteps()`: Gets step data for the past week
- `getHeartRateData(hoursBack)`: Gets heart rate data for specified time period
- `getLatestWeight()`: Gets the most recent weight measurement
- `getLatestHeight()`: Gets the most recent height measurement
- `getComprehensiveFitnessData()`: Gets all available fitness data

### 6. Integration Points

#### ConnectAppsScreen
- Shows Google Fit connection status
- Handles connection/disconnection
- Displays connection feedback

#### DashboardScreen
- Displays today's fitness data when connected
- Shows steps, distance, calories, heart rate, and weight
- Handles loading states and error cases

#### APITestingScreen
- Tests Google Fit connection and data access
- Provides detailed feedback on connection status
- Shows actual data when available

#### MainActivity
- Handles Google Fit activity results
- Manages OAuth callback flow

## Usage Examples

### Connecting to Google Fit
```kotlin
val googleFitService = GoogleFitService(context)
val intent = googleFitService.initiateConnection()
startActivity(intent)
```

### Reading Daily Steps
```kotlin
val stepsResult = googleFitService.getDailySteps()
if (stepsResult.isSuccess) {
    val steps = stepsResult.getOrNull() ?: 0
    println("Today's steps: $steps")
}
```

### Getting Comprehensive Data
```kotlin
val fitnessData = googleFitService.getComprehensiveFitnessData()
if (fitnessData.isSuccess) {
    val data = fitnessData.getOrNull()
    println("Steps: ${data?.steps}")
    println("Heart Rate: ${data?.heartRate} BPM")
    println("Weight: ${data?.weight} kg")
}
```

## Error Handling

The service includes comprehensive error handling:
- Network errors
- Permission denials
- Data not available
- Authentication failures

All methods return `Result<T>` to handle success/failure cases gracefully.

## Testing

### Manual Testing
1. Connect Google Fit account in ConnectAppsScreen
2. Check API Testing screen for connection status
3. View fitness data in Dashboard screen
4. Test data reading with various time ranges

### API Testing Screen
The API Testing screen provides:
- Connection status verification
- Actual data reading tests
- Detailed error reporting
- Step count verification

## Troubleshooting

### Common Issues

1. **"Not connected to Google Fit"**
   - Ensure user has signed in with Google account
   - Check that fitness permissions were granted
   - Verify Google Play Services is up to date

2. **"No fitness data available"**
   - Check if user has recorded fitness data in Google Fit
   - Verify data sync between device and Google Fit
   - Check if data exists for the requested time range

3. **Permission denied errors**
   - Request permissions again using `requestPermissions()`
   - Check Android runtime permissions (ACTIVITY_RECOGNITION, BODY_SENSORS)
   - Verify Google Fit app permissions

### Debug Logging

The service includes comprehensive logging with tag "GoogleFitService":
```kotlin
Log.i(TAG, "Total steps today: $totalSteps")
Log.e(TAG, "Error reading daily steps", e)
```

## Future Enhancements

Potential improvements for the Google Fit integration:
1. Real-time data streaming
2. Background data collection
3. Data writing (recording workouts)
4. Goal setting and tracking
5. Integration with Health Connect (future migration)
6. More detailed analytics and insights

## Migration to Health Connect

Note: Google is deprecating Google Fit APIs in 2026. When ready to migrate:
1. Health Connect is the recommended replacement
2. Available on Android 13+ (API 33+)
3. Provides similar functionality with better privacy controls
4. Migration guide will be provided when needed

## References

- [Google Fit Android API Documentation](https://developers.google.com/fit/android/get-started)
- [Google Fit Data Types](https://developers.google.com/fit/android/data-types)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Health Connect Documentation](https://developer.android.com/health-connect)
