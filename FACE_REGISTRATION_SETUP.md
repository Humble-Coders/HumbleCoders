# Face Registration Setup Guide

## Overview
This app now includes face registration functionality using Luxand face recognition API and CameraX for camera integration.

## Features Added

### 1. Face Registration Screen
- **Location**: `FaceRegistrationScreen.kt`
- **Features**:
  - Camera preview using CameraX
  - Face capture functionality
  - Progress tracking during registration
  - Error handling and user feedback
  - "Register Later" option

### 2. Luxand API Integration
- **API Service**: `LuxandApiService.kt`
- **Repository**: `FaceRegistrationRepository.kt`
- **Configuration**: `LuxandConfig.kt`
- **ViewModel**: `FaceRegistrationViewModel.kt`

### 3. Navigation Integration
- Added face registration screen to navigation flow
- **Automatically appears after successful user registration**
- Proper back navigation and completion handling
- Users cannot skip face registration during initial sign-up

## Setup Instructions

### 1. Get Luxand API Token
1. Visit [Luxand Cloud](https://cloud.luxand.co/)
2. Create an account or sign in
3. Navigate to your dashboard
4. Copy your API token

### 2. Configure API Token
1. Open `LuxandConfig.kt`
2. Replace `YOUR_LUXAND_API_TOKEN` with your actual token:
   ```kotlin
   const val API_TOKEN = "your_actual_token_here"
   ```

### 3. Test the Integration
1. Build and run the app
2. **Create a new account** (Sign Up)
3. **Face registration screen will automatically appear after successful registration**
4. Grant camera permission when prompted
5. Tap "Start Verification" to capture your face
6. The app will process and register your face
7. After completion, you'll be redirected to the dashboard

## API Endpoints Used

### Create Person
- **Endpoint**: `POST /persons`
- **Purpose**: Creates a new person in the Luxand database
- **Documentation**: [Create Person API](https://documenter.getpostman.com/view/1485228/UVeCR95R#0995fda1-aa9b-4491-89fb-8b17a7748ead)

### Add Face
- **Endpoint**: `POST /persons/{personId}/faces`
- **Purpose**: Adds face images to an existing person
- **Documentation**: [Add Face API](https://documenter.getpostman.com/view/1485228/UVeCR95R#5e280ffb-5b4f-4c4c-a3e3-7b06789bacf5)

## Dependencies Added

### CameraX
- `androidx.camera:camera-core:1.3.1`
- `androidx.camera:camera-camera2:1.3.1`
- `androidx.camera:camera-lifecycle:1.3.1`
- `androidx.camera:camera-view:1.3.1`
- `androidx.camera:camera-extensions:1.3.1`

### Network & API
- `com.squareup.retrofit2:retrofit:2.9.0`
- `com.squareup.retrofit2:converter-gson:2.9.0`
- `com.squareup.okhttp3:logging-interceptor:4.12.0`

### Image Processing
- `com.github.bumptech.glide:glide:4.16.0`

### Permissions
- `com.google.accompanist:accompanist-permissions:0.32.0`

## Permissions Required

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## UI Design

The face registration screen follows the design shown in the provided image:
- Clean, modern interface with light grey background
- Prominent "Face Registration" title
- Descriptive text explaining the purpose
- Central camera preview area with face scanning visualization
- "Start Verification" button with proper styling
- Security note about data storage
- "Register Later" option for users who want to skip

## Error Handling

The implementation includes comprehensive error handling for:
- Camera permission denial
- Camera initialization failures
- Image capture errors
- Network connectivity issues
- API response errors
- Face processing failures

## Security Considerations

- Face data is processed securely through Luxand's cloud API
- Images are temporarily stored locally during processing
- No face data is permanently stored on the device
- All network communications use HTTPS

## Testing

To test the face registration:
1. Ensure you have a valid Luxand API token
2. Run the app on a physical device (camera functionality requires real hardware)
3. Grant camera permissions when prompted
4. Test the face capture and registration flow
5. Verify that the registration completes successfully

## Troubleshooting

### Common Issues
1. **Camera not working**: Ensure camera permissions are granted
2. **API errors**: Verify your Luxand API token is correct
3. **Build errors**: Ensure all dependencies are properly added
4. **Face detection issues**: Ensure good lighting and clear face visibility

### Debug Logs
The app includes comprehensive logging for debugging:
- Camera initialization logs
- Image capture logs
- API request/response logs
- Error logs with detailed messages
