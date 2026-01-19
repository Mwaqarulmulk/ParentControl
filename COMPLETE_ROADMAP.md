# Parental Control App - Complete Feature Roadmap

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        FIREBASE CLOUD                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │   Firestore     │  │ Realtime DB     │  │ Firebase        │         │
│  │ (Persistent)    │  │ (Real-time)     │  │ Storage         │         │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘         │
│           │                    │                    │                   │
└───────────┼────────────────────┼────────────────────┼───────────────────┘
            │                    │                    │
    ┌───────┴───────┐    ┌───────┴───────┐    ┌───────┴───────┐
    │               │    │               │    │               │
┌───┴───────────────┴────┴───────────────┴────┴───────────────┴───┐
│                      PARENT APP                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │
│  │ Dashboard   │  │ Device      │  │ Stream      │               │
│  │ Screen      │  │ Screen      │  │ Viewer      │               │
│  └─────────────┘  └─────────────┘  └─────────────┘               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │
│  │ Location    │  │ Snapshots   │  │ Recordings  │               │
│  │ Screen      │  │ Screen      │  │ Screen      │               │
│  └─────────────┘  └─────────────┘  └─────────────┘               │
│  ┌─────────────────────────────────────────────────┐             │
│  │  CommandRepository → Sends commands via RTDB    │             │
│  └─────────────────────────────────────────────────┘             │
└──────────────────────────────────────────────────────────────────┘
            │                    │                    │
            │   ← Commands →     │   ← WebRTC →       │   ← Data →
            │                    │                    │
┌──────────────────────────────────────────────────────────────────┐
│                      CHILD APP                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  MonitoringService (Foreground - Main Coordinator)          │ │
│  │    ├── CommandListenerService (Listens for commands)        │ │
│  │    ├── LocationTrackingService (GPS tracking)               │ │
│  │    └── StreamingService (WebRTC video/audio streaming)      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Standalone Services (Special permissions required)         │ │
│  │    ├── NotificationListenerService (System service)         │ │
│  │    └── AppBlockingAccessibilityService (System service)     │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  On-demand Services (Started by commands)                   │ │
│  │    ├── SnapshotService (Camera/Screen capture)              │ │
│  │    └── RecordingService (Video/Audio recording)             │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

## Feature List & Status

### 1. Device Pairing ✅ WORKING
- Child generates 6-digit code
- Parent enters code to pair
- Creates parent-child relationship in Firestore

### 2. Location Tracking 🔧 NEEDS FIX
**Flow:**
1. Child: LocationTrackingService → Gets GPS → Saves to RTDB + Firestore
2. Parent: LiveLocationScreen → Listens to RTDB → Shows on map

**Current Issues:**
- LocationTrackingService not starting properly after pairing
- Parent app not listening to Realtime Database for instant updates

### 3. Live Camera Streaming 🔧 NEEDS FIX
**Flow:**
1. Parent: Clicks "Remote Camera" → Sends command via RTDB
2. Child: CommandListenerService → Starts StreamingService
3. Child: StreamingService → WebRTC → Creates offer → Signaling via RTDB
4. Parent: StreamViewerScreen → Receives offer → Creates answer → Video displays

**Current Issues:**
- StreamingService not being started with correct device ID
- WebRTC signaling path issues
- Parent's StreamViewerScreen needs proper initialization

### 4. Screen Mirroring 🔧 NEEDS FIX
**Flow:**
1. Parent: Clicks "Screen Mirror" → Sends command
2. Child: Needs MediaProjection permission (user must grant)
3. Child: ScreenCaptureManager → Captures screen → Streams via WebRTC

**Current Issues:**
- MediaProjection permission flow incomplete
- Screen capture not initializing properly

### 5. Audio Streaming (One-Way Audio) 🔧 NEEDS FIX
**Flow:**
1. Parent: Clicks "One-Way Audio" → Sends command
2. Child: MicrophoneManager → Captures audio → Streams via WebRTC

**Current Issues:**
- Audio-only stream not properly configured

### 6. Notification Capture 🔧 NEEDS FIX
**Flow:**
1. Child: NotificationListenerService captures all app notifications
2. Child: Saves to Realtime Database + Firestore
3. Parent: Receives real-time notification updates

**Current Issues:**
- Service needs proper activation (user must enable in settings)
- Real-time sync needs verification

### 7. Snapshots (Camera/Screen) 🔧 NEEDS FIX
**Flow:**
1. Parent: Clicks snapshot button → Sends command
2. Child: SnapshotService captures image → Uploads to Firebase Storage
3. Child: Saves metadata to Realtime Database
4. Parent: SnapshotsScreen receives real-time updates → Shows images

**Current Issues:**
- SnapshotService not being started by commands properly
- Need to ensure camera initialization works

### 8. Recordings (Video/Audio) 🔧 NEEDS FIX
**Flow:**
1. Parent: Starts recording → Sends command with duration
2. Child: RecordingService records → Uploads to Storage
3. Parent: Can view completed recordings

**Current Issues:**
- RecordingService needs proper initialization

## Firebase Data Structure

```
Firestore:
├── users/
│   └── {parentId}/
│       └── children/
│           └── {deviceId}/ (paired devices)
├── devices/
│   └── {deviceId}/ (device info, status, location)
├── notifications/
│   └── {deviceId}/
│       └── history/ (notification history)
└── locations/
    └── {deviceId}/
        └── history/ (location history)

Realtime Database:
├── commands/
│   └── {deviceId}/
│       └── {commandId}/ (pending commands)
├── locations/
│   └── {deviceId}/
│       └── current/ (real-time location)
├── devices/
│   └── {deviceId}/
│       └── status/ (real-time online/battery status)
├── notifications/
│   └── {deviceId}/
│       ├── latest/ (most recent notification)
│       └── history/ (for real-time sync)
├── snapshots/
│   └── {deviceId}/ (snapshot metadata)
├── signaling/
│   └── {deviceId}/
│       ├── offer/
│       ├── answer/
│       ├── childIceCandidates/
│       └── parentIceCandidates/
└── streamRequests/
    └── {deviceId}/ (active stream requests)
```

## Required Permissions (Child App)

1. **Camera** - For camera streaming/snapshots
2. **Microphone** - For audio streaming/recording
3. **Location (Fine + Background)** - For GPS tracking
4. **Notification Listener** - Special settings permission
5. **Accessibility Service** - For app blocking (optional)
6. **Draw Over Other Apps** - For overlays (optional)
7. **Media Projection** - For screen mirroring (runtime grant)
8. **Foreground Service** - For background operation

## Fixes Required

### Child App Fixes:
1. MonitoringService must start StreamingService with device ID
2. CommandListenerService must properly initialize services
3. SnapshotService needs proper camera initialization
4. Verify Realtime Database paths match between apps

### Parent App Fixes:
1. StreamViewerScreen WebRTC initialization
2. LiveLocationScreen real-time listener from RTDB
3. SnapshotsScreen proper connection to data
4. Ensure all navigation routes connected

## Testing Checklist

- [ ] Pairing works
- [ ] Child device shows as online on parent
- [ ] Location appears on map
- [ ] Location updates in real-time
- [ ] Camera stream works
- [ ] Screen mirror works
- [ ] Audio stream works
- [ ] Notifications sync
- [ ] Camera snapshot works
- [ ] Screen snapshot works
- [ ] Recordings work
