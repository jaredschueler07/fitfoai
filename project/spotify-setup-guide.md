# 🎵 Spotify API Setup Guide

## 📋 Overview

This guide will help you set up Spotify API integration for the RunningCoach app, including OAuth authentication and redirect URI configuration.

## 🔑 Step 1: Create Spotify App

1. **Go to Spotify Developer Dashboard**
   - Visit: https://developer.spotify.com/dashboard
   - Sign in with your Spotify account

2. **Create New App**
   - Click "Create App"
   - Fill in the required information:
     - **App name**: `RunningCoach V2`
     - **App description**: `AI-powered running coach with music integration`
     - **Website**: `https://your-website.com` (optional)
     - **Redirect URI**: `com.runningcoach.v2://spotify-callback`
     - **API/SDKs**: Check "Web API"
   - Accept the terms and click "Save"

3. **Get Your Credentials**
   - After creating the app, you'll see:
     - **Client ID**: Copy this value
     - **Client Secret**: Click "Show Client Secret" and copy this value

## 🔗 Step 2: Configure Redirect URI

### For Android App
The redirect URI should be in the format: `com.runningcoach.v2://spotify-callback`

This URI scheme is already configured in the AndroidManifest.xml:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="com.runningcoach.v2" />
</intent-filter>
```

### Alternative Redirect URIs
You can also use these formats:
- `com.runningcoach.v2://auth`
- `com.runningcoach.v2://callback`
- `com.runningcoach.v2://oauth`

## 📝 Step 3: Update API Keys

Add your Spotify credentials to `app/src/main/assets/api_keys.properties`:

```properties
# Spotify API Configuration
SPOTIFY_CLIENT_ID=your_spotify_client_id_here
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret_here
SPOTIFY_REDIRECT_URI=com.runningcoach.v2://spotify-callback
```

## 🔧 Step 4: Configure Scopes

The app requires the following Spotify scopes:

- `user-read-private` - Read user's private information
- `user-read-email` - Read user's email address
- `playlist-read-private` - Read user's private playlists
- `playlist-read-collaborative` - Read collaborative playlists
- `playlist-modify-public` - Modify public playlists
- `playlist-modify-private` - Modify private playlists
- `user-library-read` - Read user's library
- `user-top-read` - Read user's top tracks and artists
- `user-read-recently-played` - Read recently played tracks

These scopes are automatically configured in the `SpotifyConfig` class.

## 🧪 Step 5: Test the Integration

### Test OAuth Flow
1. Build and run the app
2. Navigate to the Spotify integration section
3. Tap "Connect Spotify"
4. You should be redirected to Spotify's authorization page
5. After authorization, you should be redirected back to the app

### Test API Calls
The app will test the following API endpoints:
- Get user profile
- Get user's playlists
- Get user's top tracks
- Create workout playlists

## 🔒 Step 6: Security Considerations

### Client Secret Security
- Never expose the client secret in client-side code
- For production apps, use a backend server to handle OAuth
- Consider using Spotify's PKCE flow for enhanced security

### Token Management
- Store access tokens securely using EncryptedSharedPreferences
- Implement token refresh logic
- Handle token expiration gracefully

## 🚀 Step 7: Production Deployment

### Update Redirect URIs
For production, you may want to add additional redirect URIs:
- `https://your-domain.com/spotify-callback` (for web fallback)
- `com.runningcoach.v2://spotify-callback` (for Android app)

### App Store Requirements
- Ensure your app complies with Spotify's branding guidelines
- Test the integration thoroughly before submission
- Provide clear privacy policy regarding data usage

## 🐛 Troubleshooting

### Common Issues

#### 1. "Invalid redirect URI" Error
**Problem**: Spotify returns "Invalid redirect URI" error
**Solution**: 
- Double-check the redirect URI in Spotify Dashboard
- Ensure it matches exactly: `com.runningcoach.v2://spotify-callback`
- Check for extra spaces or typos

#### 2. App Not Opening After Authorization
**Problem**: After Spotify authorization, the app doesn't open
**Solution**:
- Verify the intent filter in AndroidManifest.xml
- Check that the scheme matches your redirect URI
- Test with a simple intent filter first

#### 3. "Invalid client" Error
**Problem**: API calls return "Invalid client" error
**Solution**:
- Verify Client ID and Client Secret are correct
- Check that the app is properly registered in Spotify Dashboard
- Ensure the app is not in development mode if testing with production tokens

#### 4. Scope Permission Issues
**Problem**: Some features don't work due to missing permissions
**Solution**:
- Verify all required scopes are included in the authorization request
- Check that the user granted all requested permissions
- Re-authorize the app if scopes were changed

### Debug Steps
1. **Check Logs**: Look for Spotify-related error messages
2. **Verify Configuration**: Double-check all API keys and URIs
3. **Test OAuth Flow**: Use a simple test to verify the redirect works
4. **Check Network**: Ensure the device has internet connectivity
5. **Clear App Data**: Sometimes cached tokens can cause issues

## 📚 Additional Resources

- [Spotify Web API Documentation](https://developer.spotify.com/documentation/web-api/)
- [Spotify OAuth Guide](https://developer.spotify.com/documentation/general/guides/authorization-guide/)
- [Android Deep Linking](https://developer.android.com/training/app-links/deep-linking)
- [Spotify Branding Guidelines](https://developer.spotify.com/documentation/design)

## 🔄 Next Steps

After setting up Spotify integration:

1. **Test Music Integration**: Verify playlist creation and playback
2. **Implement Workout Playlists**: Create AI-generated workout playlists
3. **Add Music-Coaching Sync**: Synchronize music with coaching messages
4. **Optimize Performance**: Cache playlists and optimize API calls
5. **Add Offline Support**: Download playlists for offline use

---

**Note**: Keep your API keys secure and never commit them to version control. The `api_keys.properties` file is already added to `.gitignore` for security.
