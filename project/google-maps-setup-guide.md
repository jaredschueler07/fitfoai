# 🗺️ Google Maps API Setup Guide

## 📋 Overview

This guide will help you set up Google Maps API for the RunningCoach app, including location services, route tracking, and navigation features.

## 🗺️ Google Maps API Setup

### Step 1: Create Google Cloud Project

1. **Go to Google Cloud Console**
   - Visit: https://console.cloud.google.com/
   - Sign in with your Google account

2. **Create New Project**
   - Click "Select a project" → "New Project"
   - **Project name**: `RunningCoach V2`
   - **Project ID**: `runningcoach-v2-xxxxx` (auto-generated)
   - Click "Create"

### Step 2: Enable Required APIs

In your Google Cloud project, enable these APIs:

1. **Maps SDK for Android**
   - Go to **APIs & Services** → **Library**
   - Search for "Maps SDK for Android"
   - Click "Enable"

2. **Directions API**
   - Search for "Directions API"
   - Click "Enable"

3. **Elevation API**
   - Search for "Elevation API"
   - Click "Enable"

4. **Geocoding API**
   - Search for "Geocoding API"
   - Click "Enable"

5. **Places API** (for race locations)
   - Search for "Places API"
   - Click "Enable"

### Step 3: Create API Key

1. **Go to APIs & Services** → **Credentials**
2. **Click "Create Credentials"** → **API Key**
3. **Copy the API key** (starts with `AIza...`)

### Step 4: Restrict API Key (Security)

1. **Click on your API key** to edit it
2. **Application restrictions**: Select "Android apps"
3. **Add your app's package name**: `com.runningcoach.v2`
4. **Add your SHA-1 fingerprint**: `AC:5B:1A:E4:68:D0:81:3F:1D:4C:1E:D7:EC:CE:BC:B5:7F:B2:DF:24`
5. **API restrictions**: Select only these APIs:
   - Maps SDK for Android
   - Directions API
   - Elevation API
   - Geocoding API
   - Places API

### Step 5: Update API Keys File

Add your Google Maps API key to `app/src/main/assets/api_keys.properties`:

```properties
GOOGLE_MAPS_API_KEY=your_actual_google_maps_api_key_here
```

## 💰 Billing Setup

### Enable Billing

1. **Go to Billing** in Google Cloud Console
2. **Link a billing account** to your project
3. **Set up billing alerts** to avoid unexpected charges

### Cost Estimates

**Google Maps API** (typical monthly usage for RunningCoach):
- Maps SDK: ~$5-20/month
- Directions API: ~$10-50/month
- Geocoding API: ~$5-15/month
- Elevation API: ~$5-10/month
- **Total**: ~$25-95/month

**Free Tier**:
- $200 monthly credit
- Usually covers most small to medium apps

## 🔧 Integration Steps

### 1. Update API Keys

Add your Google Maps API key to the properties file:

```properties
# Google Maps API - For route visualization and location services
GOOGLE_MAPS_API_KEY=AIzaSyYourActualKeyHere
```

### 2. Test Google Maps Integration

The app will test these features:
- **Route tracking**: GPS coordinates to route polyline
- **Elevation data**: Get elevation for route points
- **Address resolution**: Convert coordinates to addresses
- **Race locations**: Find nearby races and locations

## 🧪 Testing

### Test Google Maps Features

1. **Build and run the app**
2. **Start a run** to test GPS tracking
3. **Check route visualization** on the map
4. **Verify elevation data** in run statistics
5. **Test address resolution** for run locations

## 🔒 Security Best Practices

### API Key Security

1. **Never commit API keys** to version control
2. **Use API key restrictions** (Android apps + SHA-1)
3. **Monitor API usage** in Google Cloud Console
4. **Set up billing alerts** to prevent overages

## 🐛 Troubleshooting

### Common Issues

#### 1. "API key not valid" Error
**Problem**: Google Maps API returns "API key not valid"
**Solution**:
- Verify API key is correct
- Check API key restrictions (package name, SHA-1)
- Ensure required APIs are enabled
- Check billing is enabled

#### 2. "Quota exceeded" Error
**Problem**: API calls fail due to quota limits
**Solution**:
- Check current usage in Google Cloud Console
- Increase quota limits if needed
- Implement request caching
- Add retry logic with exponential backoff

#### 3. Maps Not Loading
**Problem**: Google Maps doesn't display
**Solution**:
- Check internet connectivity
- Verify API key is valid
- Check Maps SDK is enabled
- Ensure app has location permissions

### Debug Steps

1. **Check Logs**: Look for Google API error messages
2. **Verify Configuration**: Double-check API keys and permissions
3. **Test API Calls**: Use Google Cloud Console to test APIs directly
4. **Check Billing**: Ensure billing is enabled and account is active
5. **Monitor Usage**: Check API usage in Google Cloud Console

## 📚 Additional Resources

- [Google Maps Platform Documentation](https://developers.google.com/maps/documentation)
- [Android Maps SDK Guide](https://developers.google.com/maps/documentation/android-sdk/overview)
- [Google Cloud Pricing](https://cloud.google.com/maps-platform/pricing)
- [API Key Best Practices](https://developers.google.com/maps/api-security-best-practices)

## 🔄 Next Steps

After setting up Google Maps:

1. **Test Basic Integration**: Verify maps and location services work
2. **Implement Route Tracking**: Add GPS to route conversion
3. **Add Elevation Data**: Include elevation in run statistics
4. **Optimize Performance**: Cache API responses and optimize requests
5. **Monitor Usage**: Set up alerts and monitor API usage

---

**Note**: Keep your API keys secure and never commit them to version control. The `api_keys.properties` file is already added to `.gitignore` for security.
