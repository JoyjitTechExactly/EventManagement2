# Event Management App

A modern Android application for managing events, built with Kotlin and Xml. The app provides a seamless experience for creating, viewing, and managing events with a clean and intuitive user interface.

## ✨ Features

- **User Authentication**: Secure login and registration
- **Event Management**: Create, view, edit, and delete events
- **Custom Toolbar**: Consistent UI with custom toolbar across the app
- **Modern UI**: Built with Material Design 3 for a polished look and feel
- **Real-time Updates**: Events are synchronized in real-time using Firebase
- **Responsive Design**: Works on various screen sizes and orientations

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Database**: Firebase Firestore
- **Authentication**: Firebase Authentication
- **UI**: Xml, Material Design 3
- **Navigation**: Navigation Component
- **Coroutines & Flow**: For asynchronous operations

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34 (Android 14)
- Kotlin 1.9.0 or later
- Gradle 8.0 or later

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/JoyjitTechExactly/EventManagement2.git
   cd EventManagement2
   ```

2. **Set up Firebase**
   - Go to the [Firebase Console](https://console.firebase.google.com/)
   - Create a new project
   - Add an Android app to your project
   - Package name: `com.example.eventmanagement2`
   - Download the `google-services.json` file
   - Place the file in the `app/` directory

3. **Enable Authentication** (in Firebase Console)
   - Go to Authentication > Sign-in method
   - Enable Email/Password authentication

4. **Set up Firestore Database**
   - Go to Firestore Database in Firebase Console
   - Create a new database in test mode (for development)
   - Create a collection named  "users", "events"

5. **Build and Run**
   - Open the project in Android Studio
   - Sync project with Gradle files
   - Run the app on an emulator or physical device

## 🔧 Configuration

### Firebase Configuration

The app requires a valid `google-services.json` file in the `app/` directory. This file contains your Firebase project configuration and should not be committed to version control.

### Environment Variables

Create a `local.properties` file in the root directory with the following content:

```properties
# Google Maps API Key (if using Maps)
MAPS_API_KEY=your_maps_api_key_here
```

## 🏗️ Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/eventmanagement2/
│   │   │   ├── data/           # Data layer (repositories, models, remote)
│   │   │   ├── di/             # Dependency injection modules
│   │   │   ├── ui/             # UI layer (screens, components, viewmodels)
│   │   │   │   ├── auth/       # Authentication screens
│   │   │   │   ├── events/     # Event management screens
│   │   │   │   └── dashboard/  # Dashboard and main screens
│   │   │   └── utils/          # Utility classes and extensions
│   │   └── res/                # Resources (layouts, drawables, strings, etc.)
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Contact

For any questions or feedback, please contact [joyjit.techexactly@gmail.com]
