# HumbleCoders - Learning Platform App

A comprehensive Android learning platform built with Jetpack Compose that offers online courses, group study features, face verification, and peer-to-peer file sharing capabilities.

## 🚀 Features

### 📚 Course Management
- **Course Catalog**: Browse courses across multiple categories (App Dev, Web Dev, Python)
- **Course Details**: Detailed course information with sections, videos, and pricing
- **Video Player**: Integrated ExoPlayer for seamless video playback
- **Course Enrollment**: Secure payment integration with Razorpay
- **Progress Tracking**: Track learning progress and completion status

### 👥 Group Study & Collaboration
- **Group Study Sessions**: Join study groups for specific courses and sections
- **Real-time Video Sharing**: Share and watch course videos with study partners
- **P2P File Sharing**: Share course materials, notes, and resources directly between devices
- **WiFi Direct Support**: Direct device-to-device connections for offline sharing
- **Nearby Connections**: Use Google Nearby Connections API for seamless file transfers

### 🔐 Advanced Security & Verification
- **Face Recognition**: Luxand API integration for secure user verification
- **Attendance Tracking**: Automatic attendance verification using face recognition
- **Test Verification**: Secure test-taking with face verification
- **User Authentication**: Firebase Authentication with Google Sign-In support

### 🤖 AI-Powered Features
- **ChatGPT Integration**: AI-generated study schedules and learning recommendations
- **Smart Scheduling**: Personalized study plans based on user preferences
- **Learning Analytics**: Track progress and suggest improvements

### 📱 Modern UI/UX
- **Jetpack Compose**: Modern, declarative UI framework
- **Material Design 3**: Latest Material Design guidelines
- **Responsive Design**: Optimized for different screen sizes
- **Dark/Light Theme**: Adaptive theming support

## 🛠️ Technical Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Navigation**: Navigation Compose
- **State Management**: StateFlow/Flow

### Backend & Services
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth + Google Sign-In
- **File Storage**: Firebase Storage
- **Analytics**: Firebase Analytics

### Third-Party Integrations
- **Face Recognition**: Luxand Cloud API
- **Payment Processing**: Razorpay
- **AI Services**: OpenAI ChatGPT API
- **Video Playback**: ExoPlayer (Media3)
- **Image Loading**: Coil
- **Network**: Retrofit + OkHttp

### P2P & Connectivity
- **WiFi Direct**: Direct device-to-device communication
- **Nearby Connections**: Google Nearby Connections API
- **Bluetooth**: Bluetooth Low Energy support
- **File Transfer**: Custom file transfer protocols

## 📋 Prerequisites

### Development Environment
- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK API 24+ (Android 7.0)
- Target SDK 35 (Android 15)

### Required API Keys
1. **Firebase Project**: Set up Firebase project and add `google-services.json`
2. **Luxand API**: Get API token from [Luxand Cloud](https://cloud.luxand.co/)
3. **OpenAI API**: Get API key from [OpenAI Platform](https://platform.openai.com/)
4. **Razorpay**: Set up Razorpay account for payments

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone <repository-url>
cd HumbleCoders
```

### 2. Configure API Keys

#### Firebase Setup
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add Android app with package name: `com.humblecoders.humblecoders`
3. Download `google-services.json` and place it in `app/` directory

#### Luxand API Configuration
1. Open `app/src/main/java/com/humblecoders/humblecoders/LuxandConfig.kt`
2. Replace `YOUR_LUXAND_API_TOKEN` with your actual token:
```kotlin
const val API_TOKEN = "your_actual_token_here"
```

#### OpenAI API Configuration
1. Open `app/src/main/java/com/humblecoders/humblecoders/ChatGPTService.kt`
2. Replace the API key with your actual OpenAI API key:
```kotlin
private val apiKey = "your_openai_api_key_here"
```

#### Razorpay Configuration
1. Open `app/src/main/java/com/humblecoders/humblecoders/RazorpayPaymentService.kt`
2. Replace the test key with your actual Razorpay key:
```kotlin
private const val RAZORPAY_KEY_ID = "your_razorpay_key_here"
```

### 3. Build and Run
1. Open the project in Android Studio
2. Sync project with Gradle files
3. Build and run on a physical device (camera features require real hardware)

## 📱 App Structure

### Main Screens
- **Welcome Screen**: App introduction and authentication options
- **Sign In/Sign Up**: User authentication with email/password and Google Sign-In
- **Face Registration**: Biometric registration using Luxand API
- **Skills & Interests**: User preference collection for personalized experience
- **Dashboard**: Main app interface with course recommendations
- **Course Detail**: Detailed course information and enrollment
- **Group Study**: Collaborative learning sessions
- **Profile**: User profile management

### Key Components
- **Authentication**: Firebase Auth with Google Sign-In
- **Course Management**: Course catalog, details, and enrollment
- **Video Player**: ExoPlayer integration for course videos
- **Face Verification**: Luxand API for secure verification
- **P2P Sharing**: WiFi Direct and Nearby Connections
- **Payment Processing**: Razorpay integration
- **AI Integration**: ChatGPT for study schedules

## 🔧 Configuration

### Permissions
The app requires several permissions for full functionality:

```xml
<!-- Camera for face recognition -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Internet for API calls -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Storage for file downloads -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- WiFi Direct for P2P sharing -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

<!-- Location for Nearby Connections -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Bluetooth for device discovery -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

### Dependencies
Key dependencies include:
- Jetpack Compose BOM
- Firebase BOM
- CameraX for camera functionality
- ExoPlayer for video playback
- Retrofit for network calls
- Coil for image loading
- Razorpay for payments
- Google Play Services

## 🎯 Key Features Deep Dive

### Face Recognition System
- **Registration**: Users register their face during onboarding
- **Verification**: Face verification for attendance and test taking
- **Security**: Secure cloud processing with Luxand API
- **Fallback**: Option to skip face registration if needed

### Group Study Features
- **Session Creation**: Create study groups for specific courses
- **Video Synchronization**: Watch course videos together
- **File Sharing**: Share notes, resources, and materials
- **Real-time Communication**: Chat and collaboration tools

### P2P File Sharing
- **WiFi Direct**: Direct device-to-device connections
- **Nearby Connections**: Google's proximity-based sharing
- **File Types**: Support for videos, documents, and images
- **Progress Tracking**: Real-time transfer progress

### AI-Powered Learning
- **Study Schedules**: Personalized learning plans
- **Progress Analysis**: AI-driven learning insights
- **Recommendations**: Course and content suggestions
- **Adaptive Learning**: Dynamic content adjustment

## 🧪 Testing

### Test on Physical Device
- Camera features require real hardware
- WiFi Direct needs multiple devices
- Location services need actual GPS

### Test Scenarios
1. **User Registration**: Complete sign-up flow with face registration
2. **Course Enrollment**: Browse and enroll in courses
3. **Group Study**: Create and join study sessions
4. **File Sharing**: Test P2P file transfer
5. **Payment Flow**: Test course payment process

## 🚨 Troubleshooting

### Common Issues
1. **Camera not working**: Ensure camera permissions are granted
2. **API errors**: Verify all API keys are correctly configured
3. **Build errors**: Ensure all dependencies are properly added
4. **Face detection issues**: Ensure good lighting and clear face visibility
5. **P2P connection issues**: Check WiFi Direct and Bluetooth permissions

### Debug Logs
The app includes comprehensive logging for debugging:
- Camera initialization logs
- API request/response logs
- File transfer progress logs
- Error logs with detailed messages

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📞 Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the API documentation for third-party services

## 🔮 Future Enhancements

- [ ] Offline course downloads
- [ ] Live streaming for courses
- [ ] Advanced analytics dashboard
- [ ] Multi-language support
- [ ] AR/VR course content
- [ ] Advanced AI tutoring features

---

**Note**: This app requires physical devices for testing camera and P2P features. Some features may not work properly in emulators.
