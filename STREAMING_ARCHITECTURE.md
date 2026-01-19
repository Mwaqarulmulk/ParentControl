# Streaming Architecture - Camera Monitoring, Screen Mirroring & One-Way Audio

## Overview

This parental control app implements real-time streaming features similar to FlashGet Kids using **WebRTC** for peer-to-peer communication and **Firebase Realtime Database** for signaling.

## Technology Stack

### Core Technologies

| Feature | Technology | Description |
|---------|------------|-------------|
| **Video/Audio Streaming** | WebRTC | Peer-to-peer real-time communication |
| **Camera Capture** | WebRTC CameraVideoCapturer | Uses Camera2 API internally |
| **Screen Capture** | MediaProjection API | Android's screen capture system |
| **Audio Capture** | WebRTC AudioTrack | Direct microphone access |
| **Signaling** | Firebase Realtime Database | Exchange of SDP offers/answers and ICE candidates |
| **NAT Traversal** | STUN/TURN Servers | Google's public STUN servers |

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CHILD DEVICE                                   │
│                                                                          │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │  CameraManager   │    │ScreenCaptureManager│   │ MicrophoneManager │  │
│  │  (Video Track)   │    │  (Video Track)      │   │  (Audio Track)    │  │
│  └────────┬─────────┘    └────────┬───────────┘   └────────┬─────────┘  │
│           │                       │                        │             │
│           └───────────────────────┼────────────────────────┘             │
│                                   ▼                                      │
│                        ┌──────────────────┐                              │
│                        │  WebRTCManager   │                              │
│                        │  (Peer Conn.)    │                              │
│                        └────────┬─────────┘                              │
│                                 │                                        │
│                        ┌────────▼─────────┐                              │
│                        │ StreamingService │ (Foreground Service)        │
│                        └────────┬─────────┘                              │
│                                 │                                        │
│                        ┌────────▼─────────┐                              │
│                        │ SignalingManager │                              │
│                        └────────┬─────────┘                              │
└─────────────────────────────────┼────────────────────────────────────────┘
                                  │
                    Firebase Realtime Database
                    (/streaming/sessions/{childId}/)
                    - /offer (SDP from child)
                    - /answer (SDP from parent)
                    - /ice_candidates (ICE from both)
                                  │
┌─────────────────────────────────┼────────────────────────────────────────┐
│                                 ▼                                        │
│                        ┌──────────────────┐                              │
│                        │ SignalingManager │                              │
│                        └────────┬─────────┘                              │
│                                 │                                        │
│                        ┌────────▼─────────┐                              │
│                        │  WebRTCManager   │                              │
│                        │  (Peer Conn.)    │                              │
│                        └────────┬─────────┘                              │
│                                 │                                        │
│           ┌─────────────────────┼─────────────────────┐                  │
│           ▼                     ▼                     ▼                  │
│   ┌───────────────┐    ┌───────────────┐    ┌───────────────┐           │
│   │ Video Track   │    │ Audio Track   │    │Stream Controls│           │
│   │ (SurfaceView) │    │  (Speaker)    │    │   (UI)        │           │
│   └───────────────┘    └───────────────┘    └───────────────┘           │
│                                                                          │
│                           PARENT DEVICE                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

## Stream Types

The app supports multiple stream types defined in `StreamType` enum:

```kotlin
enum class StreamType {
    CAMERA_FRONT,    // Front camera with optional audio
    CAMERA_BACK,     // Back camera with optional audio
    SCREEN,          // Screen mirroring with optional audio
    AUDIO_ONLY       // One-way audio (microphone only)
}
```

## Feature Implementation Details

### 1. Camera Monitoring

**Flow:**
1. Parent requests camera stream from `StreamViewerScreen`
2. Request is written to Firebase: `/streaming/sessions/{childId}/request`
3. Child's `StreamingService` receives request via `SignalingManager`
4. `CameraManager` initializes camera and creates VideoTrack
5. `WebRTCManager` adds track to peer connection and creates SDP offer
6. Offer sent to Firebase, parent receives and creates answer
7. ICE candidates exchanged, direct P2P connection established
8. Video streamed in real-time to parent

**Key Classes (Child):**
- [CameraManager.kt](app-child/src/main/java/com/myparentalcontrol/child/streaming/video/CameraManager.kt) - Camera capture using WebRTC's CameraVideoCapturer
- [WebRTCManager.kt](app-child/src/main/java/com/myparentalcontrol/child/streaming/core/WebRTCManager.kt) - Creates peer connection, adds tracks
- [StreamingService.kt](app-child/src/main/java/com/myparentalcontrol/child/streaming/services/StreamingService.kt) - Orchestrates streaming

**Key Classes (Parent):**
- [WebRTCManager.kt](app-parent/src/main/java/com/myparentalcontrol/parent/streaming/core/WebRTCManager.kt) - Receives tracks
- [StreamViewerScreen.kt](app-parent/src/main/java/com/myparentalcontrol/parent/streaming/ui/StreamViewerScreen.kt) - Displays stream

**Camera Features:**
- Front/Back camera switching
- Quality adjustment (low/medium/high)
- Audio toggle (microphone on/off)

### 2. Screen Mirroring

**Flow:**
1. Parent requests screen stream
2. Child's `StreamingService` receives request
3. `ScreenCaptureManager` uses MediaProjection API to capture screen
4. Screen content converted to VideoTrack
5. Streamed via WebRTC to parent

**Key Classes:**
- [ScreenCaptureManager.kt](app-child/src/main/java/com/myparentalcontrol/child/streaming/video/ScreenCaptureManager.kt) - MediaProjection-based screen capture

**Requirements:**
- User must grant screen capture permission (one-time dialog)
- Requires foreground service with `mediaProjection` foreground service type
- Android 5.0 (API 21) minimum

**Implementation Details:**
```kotlin
// MediaProjection setup
val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

// Create virtual display for capture
mediaProjection.createVirtualDisplay(
    "ScreenCapture",
    width, height, dpi,
    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
    surface, null, null
)
```

### 3. One-Way Audio (Live Listening)

**Flow:**
1. Parent requests audio-only stream
2. Child's `MicrophoneManager` captures audio from microphone
3. AudioTrack created and added to peer connection
4. Real-time audio streamed to parent (child cannot hear parent)

**Key Classes:**
- [MicrophoneManager.kt](app-child/src/main/java/com/myparentalcontrol/child/streaming/audio/MicrophoneManager.kt) - Microphone capture

**Audio Sources:**
```kotlin
enum class AudioSource {
    MICROPHONE,      // Device microphone
    DEVICE_AUDIO,    // Internal audio (requires special permissions)
    MIXED            // Both microphone and device audio
}
```

## WebRTC Signaling via Firebase

### Database Structure

```
/streaming/
  /sessions/
    /{childDeviceId}/
      /request          - Stream request from parent
      /offer            - SDP offer from child
      /answer           - SDP answer from parent
      /ice_candidates/
        /child/         - ICE candidates from child
        /parent/        - ICE candidates from parent
      /status           - Current stream status
```

### Signaling Flow

```
1. Parent writes request to /streaming/sessions/{childId}/request
2. Child's SignalingManager listens for requests
3. Child creates SDP offer, writes to /offer
4. Parent receives offer, creates answer, writes to /answer
5. Child receives answer, connection negotiation begins
6. Both sides generate ICE candidates, exchange via /ice_candidates
7. Direct P2P connection established
8. Media streams flow directly between devices
```

## Service Architecture

### Child App Services

| Service | Purpose | Triggered By |
|---------|---------|--------------|
| `StreamingService` | Main streaming orchestrator | SignalingManager (Firebase) |
| `CameraStreamingService` | Bridge service for camera | CommandListenerService |
| `ScreenMirroringService` | Bridge service for screen | CommandListenerService |
| `AudioStreamingService` | Bridge service for audio | CommandListenerService |

The bridge services (`CameraStreamingService`, `ScreenMirroringService`, `AudioStreamingService`) act as entry points from `CommandListenerService` and delegate to the main `StreamingService` which handles actual WebRTC streaming.

### Parent App Components

| Component | Purpose |
|-----------|---------|
| `StreamViewerScreen` | UI for viewing streams |
| `StreamViewerViewModel` | Manages stream state |
| `WebRTCManager` | Handles WebRTC receiving |
| `SignalingManager` | Firebase signaling |

## Android Permissions Required

### Child App (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

### Parent App

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Quality Settings

```kotlin
data class StreamQuality(
    val width: Int,
    val height: Int,
    val frameRate: Int
)

val LOW_QUALITY = StreamQuality(480, 360, 15)
val MEDIUM_QUALITY = StreamQuality(640, 480, 24)
val HIGH_QUALITY = StreamQuality(1280, 720, 30)
```

## ICE Server Configuration

```kotlin
val iceServers = listOf(
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
    PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
    PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
)
```

For production, consider adding TURN servers for better NAT traversal:
```kotlin
PeerConnection.IceServer.builder("turn:your-turn-server.com:3478")
    .setUsername("username")
    .setPassword("password")
    .createIceServer()
```

## Comparison with FlashGet Kids

| Feature | FlashGet Kids | This App |
|---------|---------------|----------|
| Camera Monitoring | ✅ | ✅ |
| Screen Mirroring | ✅ | ✅ |
| One-Way Audio | ✅ | ✅ |
| Camera Switch | ✅ | ✅ |
| Quality Control | ✅ | ✅ |
| Real-time Streaming | ✅ | ✅ |
| Technology | WebRTC | WebRTC |
| Signaling | Proprietary Server | Firebase RTDB |

## Build Status

- ✅ Child App: BUILD SUCCESSFUL
- ✅ Parent App: BUILD SUCCESSFUL

## Files Modified

1. `CameraStreamingService.kt` - Updated to delegate to StreamingService
2. `ScreenMirroringService.kt` - Updated to delegate to StreamingService
3. `AudioStreamingService.kt` - Updated to delegate to StreamingService

## Testing the Streaming Features

1. Install both apps on separate devices
2. Pair the devices using the pairing code
3. On parent app, navigate to a paired device
4. Tap "Live Stream" or "Live Camera" button
5. Select stream type (Front Camera, Back Camera, Screen, Audio)
6. Child device will automatically start streaming
7. Use controls to switch camera, toggle audio, adjust quality

## Troubleshooting

### Stream Not Connecting
- Check Firebase Realtime Database rules allow read/write
- Verify both devices have internet connectivity
- Check ICE candidates are being exchanged

### Poor Video Quality
- Try lowering quality settings
- Check network bandwidth
- Consider adding TURN server for better NAT traversal

### Audio Not Working
- Verify RECORD_AUDIO permission is granted on child device
- Check audio is enabled in stream controls
- Verify device is not in silent mode
