# 📱 ParentControl

A two-app Android parental monitoring system built with Kotlin and Jetpack Compose. One app for parents, one for the child's device — connected in real time via Firebase.

---

## What it does

**Parent app** lets you monitor and control your child's Android device remotely:
- 📍 See their location on a live map
- 🔔 Read every notification they receive
- 📷 Start a live camera stream (front or back)
- 🖥️ Mirror their screen in real time
- 🎙️ Listen via microphone (one-way audio)
- 🔔 Ring the device remotely
- 📸 Take remote snapshots

**Child app** runs quietly in the background and sends everything to the parent. Setup takes about 30 seconds via a 6-digit pairing code.

---

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Backend:** Firebase Firestore, Realtime Database & Storage
- **Streaming:** WebRTC
- **DI:** Hilt
- **Camera:** CameraX

---

## Project layout

```
ParentControl/
├── app-parent/   # Parent monitoring app
├── app-child/    # Child device app
└── shared/       # Shared models & utilities
```

---

## Getting started

### 1. Firebase setup

1. Create a project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable **Firestore**, **Realtime Database**, and **Authentication (Anonymous)**
3. Download `google-services.json` and place it in both `app-parent/` and `app-child/`

### 2. Google Maps API key

Get a key from [Google Cloud Console](https://console.cloud.google.com), enable **Maps SDK for Android**, then add it to `local.properties`:

```properties
MAPS_API_KEY=YOUR_KEY_HERE
```

### 3. Build & install

```bash
# Install parent app on the parent's phone
./gradlew :app-parent:installDebug

# Install child app on the child's phone / emulator
./gradlew :app-child:installDebug
```

---

## Pairing devices

1. **Parent phone** → open the app → tap **"+"** → tap **"Generate Pairing Code"**
2. **Child phone** → open the app → type in the 6-digit code → tap **"Pair Device"**
3. Done — the child device shows up on the parent dashboard instantly

Codes expire after 5 minutes and can only be used once.

---

## Permissions (child device)

The child app needs a few permissions to work:

| Permission | What for |
|---|---|
| Camera | Camera streaming & snapshots |
| Microphone | Audio streaming |
| Location (background) | GPS tracking |
| Notification access | Read incoming notifications |

Grant them when prompted or via **Settings → Apps → Parental Control (Child)**.

---

## Features at a glance

| Feature | Parent | Child |
|---|---|---|
| Pairing | Generate code | Enter code |
| Location | View on map | Reports every 15 min |
| Notifications | View all | Captures all |
| Camera stream | Start / stop | Streams live |
| Screen mirror | Start / stop | Mirrors screen |
| Audio stream | Start / stop | Streams mic |
| Ring device | Tap button | Device rings |
| Snapshots | Request & view | Captures & uploads |

---

## Troubleshooting

**Device not pairing?** Make sure both phones have internet, Firebase services are enabled, and the code hasn't expired (5-minute limit).

**Location not showing?** Grant background location permission on the child device and wait up to 15 minutes for the first update, or tap **"Update Location Now"**.

**Notifications not captured?** Go to **Settings → Apps → Parental Control (Child) → Notification access** and toggle it on.

**Maps not loading?** Double-check your `MAPS_API_KEY` in `local.properties` and make sure the Maps SDK is enabled in Google Cloud Console.

---

## Security note

This app is intended for monitoring devices you own and have parental responsibility for. Always inform children that monitoring is in place. Review Firebase security rules before going to production — test-mode rules allow open access and should not be used in a live deployment.

See [SECURITY.md](SECURITY.md) for details.

---

## License

MIT
