# ParentalControlApp - Implementation Plan

## Project Overview
Personal parental control Android app for 2 phones (child + parent). NOT for production/Play Store.

**Package Name:** `com.myparentalcontrol`

---

## Phase 1: Project Configuration ⏳

### 1.1 Update Version Catalog (gradle/libs.versions.toml)
- [ ] Update Kotlin version to 1.9.22
- [ ] Update Target SDK to 34
- [ ] Add Jetpack Compose BOM 2024.02.00
- [ ] Add Firebase BOM 32.7.2
- [ ] Add Hilt 2.50
- [ ] Add WebRTC (stream-webrtc-android:1.1.1)
- [ ] Add CameraX 1.3.1
- [ ] Add Room 2.6.1
- [ ] Add WorkManager 2.9.0
- [ ] Add Play Services Location 21.1.0
- [ ] Add Maps Compose 4.3.0
- [ ] Add Coroutines
- [ ] Add Lifecycle components
- [ ] Add Navigation Compose
- [ ] Add DataStore
- [ ] Add Accompanist (permissions)

### 1.2 Update Root build.gradle.kts
- [ ] Add Hilt plugin
- [ ] Add Google Services plugin
- [ ] Add KSP plugin for Room/Hilt

### 1.3 Update shared/build.gradle.kts
- [ ] Change namespace to com.myparentalcontrol.shared
- [ ] Update SDK versions
- [ ] Add all shared dependencies
- [ ] Enable Compose
- [ ] Add Hilt, Room, Firebase dependencies

### 1.4 Update app-child/build.gradle.kts
- [ ] Change applicationId to com.myparentalcontrol.child
- [ ] Change namespace to com.myparentalcontrol.child
- [ ] Update SDK versions
- [ ] Enable Compose
- [ ] Add Hilt, Firebase, WebRTC, CameraX dependencies
- [ ] Add dependency on :shared module

### 1.5 Update app-parent/build.gradle.kts
- [ ] Change applicationId to com.myparentalcontrol.parent
- [ ] Change namespace to com.myparentalcontrol.parent
- [ ] Update SDK versions
- [ ] Enable Compose
- [ ] Add Hilt, Firebase, WebRTC, Maps dependencies
- [ ] Add dependency on :shared module

### 1.6 Update AndroidManifest files
- [ ] Add all required permissions to app-child
- [ ] Add all required permissions to app-parent
- [ ] Configure foreground service types

---

## Phase 2: Shared Module Implementation ⏳

### 2.1 Domain Layer - Models
- [ ] Create DeviceInfo model
- [ ] Create AppUsageData model
- [ ] Create LocationData model
- [ ] Create CallLogEntry model
- [ ] Create SmsLogEntry model
- [ ] Create AppBlockRule model
- [ ] Create ScreenTimeRule model
- [ ] Create StreamingSession model
- [ ] Create RemoteCommand model
- [ ] Create DeviceStatus model

### 2.2 Domain Layer - Repository Interfaces
- [ ] Create DeviceRepository interface
- [ ] Create AppUsageRepository interface
- [ ] Create LocationRepository interface
- [ ] Create CallSmsRepository interface
- [ ] Create AppControlRepository interface
- [ ] Create StreamingRepository interface
- [ ] Create CommandRepository interface
- [ ] Create AuthRepository interface

### 2.3 Domain Layer - Use Cases
- [ ] Create GetDeviceStatusUseCase
- [ ] Create UpdateDeviceStatusUseCase
- [ ] Create GetAppUsageUseCase
- [ ] Create SyncAppUsageUseCase
- [ ] Create GetLocationUseCase
- [ ] Create UpdateLocationUseCase
- [ ] Create GetCallLogsUseCase
- [ ] Create GetSmsLogsUseCase
- [ ] Create BlockAppUseCase
- [ ] Create UnblockAppUseCase
- [ ] Create SetScreenTimeLimitUseCase
- [ ] Create StartStreamingUseCase
- [ ] Create StopStreamingUseCase
- [ ] Create SendCommandUseCase
- [ ] Create ReceiveCommandUseCase

### 2.4 Data Layer - Local (Room Database)
- [ ] Create AppDatabase
- [ ] Create AppUsageEntity
- [ ] Create AppUsageDao
- [ ] Create LocationEntity
- [ ] Create LocationDao
- [ ] Create CallLogEntity
- [ ] Create CallLogDao
- [ ] Create SmsLogEntity
- [ ] Create SmsLogDao
- [ ] Create BlockedAppEntity
- [ ] Create BlockedAppDao
- [ ] Create ScreenTimeRuleEntity
- [ ] Create ScreenTimeRuleDao

### 2.5 Data Layer - Remote (Firebase)
- [ ] Create FirebaseAuthService
- [ ] Create FirestoreService
- [ ] Create RealtimeDatabaseService
- [ ] Create FirebaseMessagingService
- [ ] Create FirebaseStorageService (optional)

### 2.6 Data Layer - WebRTC
- [ ] Create WebRTCClient
- [ ] Create SignalingClient (Firebase-based)
- [ ] Create PeerConnectionObserver
- [ ] Create VideoTrackManager
- [ ] Create AudioTrackManager

### 2.7 Data Layer - Repository Implementations
- [ ] Implement DeviceRepositoryImpl
- [ ] Implement AppUsageRepositoryImpl
- [ ] Implement LocationRepositoryImpl
- [ ] Implement CallSmsRepositoryImpl
- [ ] Implement AppControlRepositoryImpl
- [ ] Implement StreamingRepositoryImpl
- [ ] Implement CommandRepositoryImpl
- [ ] Implement AuthRepositoryImpl

### 2.8 Dependency Injection
- [ ] Create NetworkModule
- [ ] Create DatabaseModule
- [ ] Create FirebaseModule
- [ ] Create WebRTCModule
- [ ] Create RepositoryModule
- [ ] Create UseCaseModule

### 2.9 Utilities
- [ ] Create PermissionUtils
- [ ] Create DateTimeUtils
- [ ] Create NetworkUtils
- [ ] Create EncryptionUtils
- [ ] Create Constants

---

## Phase 3: Child App Implementation ⏳

### 3.1 App Setup
- [ ] Create ChildApplication class with Hilt
- [ ] Configure Firebase initialization
- [ ] Create MainActivity (Compose)
- [ ] Create Navigation graph

### 3.2 Foreground Service
- [ ] Create MonitoringService (foreground service)
- [ ] Create notification channel
- [ ] Create persistent notification
- [ ] Implement service lifecycle management

### 3.3 Boot Receiver
- [ ] Create BootCompletedReceiver
- [ ] Register in manifest
- [ ] Start MonitoringService on boot

### 3.4 App Usage Monitoring
- [ ] Create UsageStatsManager wrapper
- [ ] Create AppUsageCollector
- [ ] Implement periodic usage collection (WorkManager)
- [ ] Sync usage data to Firebase

### 3.5 App Blocking
- [ ] Create AppBlockingAccessibilityService
- [ ] Implement app detection
- [ ] Implement blocking overlay
- [ ] Sync blocked apps from Firebase

### 3.6 Screen Time Management
- [ ] Create ScreenTimeManager
- [ ] Implement daily limit tracking
- [ ] Implement bedtime schedule
- [ ] Create limit reached overlay

### 3.7 Location Tracking
- [ ] Create LocationTracker
- [ ] Implement FusedLocationProviderClient
- [ ] Create periodic location updates (WorkManager)
- [ ] Sync location to Firebase

### 3.8 Camera Streaming
- [ ] Create CameraStreamingService
- [ ] Implement CameraX integration
- [ ] Create WebRTC video track
- [ ] Handle camera switching (front/back)

### 3.9 Screen Mirroring
- [ ] Create ScreenMirroringService
- [ ] Implement MediaProjection
- [ ] Create WebRTC video track from screen
- [ ] Handle permission flow

### 3.10 Audio Listening
- [ ] Create AudioStreamingService
- [ ] Implement AudioRecord
- [ ] Create WebRTC audio track
- [ ] Handle microphone permissions

### 3.11 Call/SMS Monitoring
- [ ] Create CallLogObserver
- [ ] Create SmsObserver
- [ ] Implement ContentObserver
- [ ] Sync logs to Firebase

### 3.12 App Installation Monitoring
- [ ] Create PackageInstalledReceiver
- [ ] Detect new app installations
- [ ] Send notification to parent

### 3.13 Device Status
- [ ] Create DeviceStatusManager
- [ ] Monitor battery level
- [ ] Monitor online status
- [ ] Update last seen timestamp

### 3.14 Remote Command Handler
- [ ] Create CommandReceiver
- [ ] Listen for Firebase commands
- [ ] Execute commands (start stream, etc.)
- [ ] Send command responses

### 3.15 UI Screens (Minimal - Stealth Mode)
- [ ] Create SetupScreen (initial pairing)
- [ ] Create StatusScreen (minimal info)
- [ ] Create SettingsScreen (hidden)

---

## Phase 4: Parent App Implementation ⏳

### 4.1 App Setup
- [ ] Create ParentApplication class with Hilt
- [ ] Configure Firebase initialization
- [ ] Create MainActivity (Compose)
- [ ] Create Navigation graph

### 4.2 Authentication
- [ ] Create LoginScreen
- [ ] Create RegisterScreen
- [ ] Implement Firebase Auth
- [ ] Create device pairing flow

### 4.3 Dashboard
- [ ] Create DashboardScreen
- [ ] Create DashboardViewModel
- [ ] Display child device status
- [ ] Display quick stats (screen time, location)
- [ ] Display recent activity

### 4.4 Location Tracking
- [ ] Create LocationScreen
- [ ] Integrate Google Maps Compose
- [ ] Display child location marker
- [ ] Display location history
- [ ] Implement real-time location updates

### 4.5 App Usage Reports
- [ ] Create AppUsageScreen
- [ ] Create usage charts (daily/weekly)
- [ ] Display app usage breakdown
- [ ] Show most used apps

### 4.6 App Control Panel
- [ ] Create AppControlScreen
- [ ] Display installed apps list
- [ ] Implement block/unblock toggle
- [ ] Set per-app time limits

### 4.7 Screen Time Settings
- [ ] Create ScreenTimeScreen
- [ ] Set daily screen time limits
- [ ] Configure bedtime schedule
- [ ] Set app category limits

### 4.8 Live Camera Viewer
- [ ] Create CameraViewerScreen
- [ ] Implement WebRTC video receiver
- [ ] Add camera switch button
- [ ] Handle connection states

### 4.9 Screen Mirror Viewer
- [ ] Create ScreenMirrorScreen
- [ ] Implement WebRTC video receiver
- [ ] Handle screen orientation
- [ ] Display connection status

### 4.10 Audio Listener
- [ ] Create AudioListenerScreen
- [ ] Implement WebRTC audio receiver
- [ ] Add mute/unmute controls
- [ ] Display audio level indicator

### 4.11 Call/SMS History
- [ ] Create CallHistoryScreen
- [ ] Create SmsHistoryScreen
- [ ] Display call logs with details
- [ ] Display SMS messages
- [ ] Add search/filter functionality

### 4.12 Notifications
- [ ] Create NotificationService
- [ ] Handle FCM messages
- [ ] Display push notifications
- [ ] Create NotificationHistoryScreen

### 4.13 Remote Commands
- [ ] Create CommandSender
- [ ] Implement command buttons in UI
- [ ] Handle command responses
- [ ] Display command status

### 4.14 Settings
- [ ] Create SettingsScreen
- [ ] Configure notification preferences
- [ ] Manage paired devices
- [ ] Account settings

---

## Phase 5: Testing & Polish ⏳

### 5.1 Unit Tests
- [ ] Test Use Cases
- [ ] Test Repositories
- [ ] Test ViewModels

### 5.2 Integration Tests
- [ ] Test Firebase integration
- [ ] Test WebRTC connection
- [ ] Test Room database

### 5.3 UI Tests
- [ ] Test navigation flows
- [ ] Test critical user journeys

### 5.4 Manual Testing
- [ ] Test on child device
- [ ] Test on parent device
- [ ] Test real-time features

### 5.5 Bug Fixes & Optimization
- [ ] Fix identified bugs
- [ ] Optimize battery usage
- [ ] Optimize network usage

---

## Firebase Structure

```
firestore/
├── users/
│   └── {userId}/
│       ├── profile
│       └── settings
├── devices/
│   └── {deviceId}/
│       ├── info
│       ├── status
│       └── settings
├── appUsage/
│   └── {deviceId}/
│       └── {date}/
│           └── {appPackage}
├── locations/
│   └── {deviceId}/
│       └── {timestamp}
├── callLogs/
│   └── {deviceId}/
│       └── {callId}
├── smsLogs/
│   └── {deviceId}/
│       └── {smsId}
├── blockedApps/
│   └── {deviceId}/
│       └── {appPackage}
└── screenTimeRules/
    └── {deviceId}/
        └── rules

realtime-database/
├── presence/
│   └── {deviceId}: online/offline
├── commands/
│   └── {deviceId}/
│       └── {commandId}
└── signaling/
    └── {sessionId}/
        ├── offer
        ├── answer
        └── candidates/
```

---

## Current Progress

**Phase 1:** ⏳ In Progress
- [x] Project structure created (3 modules)
- [x] Basic build.gradle.kts files
- [ ] Version catalog updates (NEXT)
- [ ] Package name updates
- [ ] Dependencies configuration

**Phase 2:** ⏳ Not Started
**Phase 3:** ⏳ Not Started
**Phase 4:** ⏳ Not Started
**Phase 5:** ⏳ Not Started

---

## Notes

- This is for personal use only, not Play Store
- Target devices: Android 10+ (API 29+)
- Min SDK: 26 (Android 8.0)
- Focus on functionality over polish
- Security: Basic Firebase Auth, not enterprise-grade
