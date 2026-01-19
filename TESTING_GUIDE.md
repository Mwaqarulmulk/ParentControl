# Parental Control App - Testing Guide

## Prerequisites

### 1. Firebase Setup
Ensure your Firebase project has:
- Firestore Database enabled
- Realtime Database enabled
- Firebase Storage enabled
- Authentication enabled (Email/Password)

### 2. Firebase Rules (Update in Firebase Console)

#### Realtime Database Rules:
```json
{
  "rules": {
    "commands": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "locations": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "devices": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "notifications": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "snapshots": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "signaling": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "streamRequests": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

#### Firebase Storage Rules:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /snapshots/{deviceId}/{allPaths=**} {
      allow read, write: if true;
    }
    match /recordings/{deviceId}/{allPaths=**} {
      allow read, write: if true;
    }
  }
}
```

---

## Testing Steps

### Step 1: Install Both Apps
1. Build and install `app-child-debug.apk` on the CHILD device
2. Build and install `app-parent-debug.apk` on the PARENT device

### Step 2: Parent App Setup
1. Open Parent App
2. Create account or sign in
3. Tap "Add Device" or "Pair Device"
4. Note the 6-digit pairing code

### Step 3: Child App Setup & Pairing
1. Open Child App
2. Grant ALL requested permissions when prompted:
   - Camera
   - Microphone
   - Location (Allow all the time for background)
   - Notification Access (opens Settings - enable for the app)
3. Enter the 6-digit pairing code from parent
4. Wait for pairing confirmation
5. Child app should show "Parental Control Active" screen

### Step 4: Verify Pairing
- On Parent App: Child device should appear in dashboard
- On Parent App: Device should show as "Online"

---

## Feature Testing

### Test 1: Location Tracking ✅
**Expected:**
1. On Parent: Tap the child device → Go to "Live Location"
2. Map should show child device location
3. Tap refresh icon to request immediate location update
4. Location should update within 30 seconds

**If Not Working:**
- Ensure child has granted "Allow all the time" for location
- Check if LocationTrackingService is running on child
- Verify Realtime Database path: `locations/{deviceId}/current`

### Test 2: Live Camera Streaming 📹
**Expected:**
1. On Parent: Tap "Remote Camera" on device screen
2. Video should start showing within 10-15 seconds
3. Can switch between front/back camera

**If Not Working:**
- Ensure child has Camera permission granted
- Check if StreamingService is running
- Verify WebRTC signaling paths in Realtime Database

### Test 3: Screen Mirroring 🖥️
**Expected:**
1. On Parent: Tap "Screen Mirroring"
2. Child device will show a permission dialog (Media Projection)
3. Child must tap "Start Now" to allow screen capture
4. Screen should appear on parent

**If Not Working:**
- Child MUST manually grant screen capture permission
- This is an Android security requirement

### Test 4: One-Way Audio 🎤
**Expected:**
1. On Parent: Tap "One-Way Audio"
2. Should hear ambient audio from child device
3. Child device shows nothing (silent listening)

**If Not Working:**
- Ensure Microphone permission granted on child
- Check audio track in WebRTC

### Test 5: Notification Sync 🔔
**Expected:**
1. On Child: Receive any notification (text message, app alert)
2. On Parent: Notification should appear in real-time
3. Shows app name, title, content, time

**Required Setup:**
1. On Child: Go to Settings → Apps → Special Access → Notification Access
2. Enable for "Parental Control" app
3. This is a REQUIRED manual step

**If Not Working:**
- Notification Access MUST be enabled manually in Settings
- Check Realtime Database: `notifications/{deviceId}/history`

### Test 6: Camera Snapshot 📷
**Expected:**
1. On Parent: Tap Camera Snapshot button
2. Wait 5-10 seconds
3. Image should appear in Snapshots screen
4. Can view full-screen

**If Not Working:**
- Camera permission required on child
- Check Firebase Storage for uploaded images
- Check Realtime Database: `snapshots/{deviceId}`

### Test 7: Screen Snapshot 🖼️
**Expected:**
1. On Parent: Tap Screen Snapshot button
2. Child needs Media Projection permission
3. Screenshot of child's screen uploads

**Note:** Screen snapshot requires same permission as screen mirroring

### Test 8: Device Status 📊
**Expected:**
- Battery level shows correctly
- Online/Offline status updates
- Network type (WiFi/Mobile) shows

**Data Source:**
- Realtime Database: `devices/{deviceId}/status`
- Firestore: `devices/{deviceId}`

---

## Common Issues & Solutions

### Issue: Child device shows as Offline
**Solution:**
1. Open Child app
2. Check if "Parental Control Active" screen is showing
3. Check if MonitoringService notification is visible
4. Force close and reopen app

### Issue: Location not updating
**Solution:**
1. Ensure Location permission is "Allow all the time"
2. Disable battery optimization for child app
3. Tap refresh on parent app
4. Check `locations/{deviceId}/current` in Firebase

### Issue: Streaming not working
**Solution:**
1. Both devices need internet connection
2. Check if signaling data exists in Firebase
3. Try stopping and restarting stream
4. Check for WebRTC errors in Logcat

### Issue: Notifications not syncing
**Solution:**
1. MUST enable Notification Access in Settings manually
2. Settings → Apps → Special App Access → Notification Access → Enable
3. Restart child app after enabling

### Issue: Snapshots not appearing
**Solution:**
1. Check Camera permission
2. Check Firebase Storage for uploaded files
3. Check `snapshots/{deviceId}` in Realtime Database
4. Look for errors in Logcat on child device

---

## Debug Commands (ADB)

### View Child App Logs:
```bash
adb logcat -s MonitoringService CommandListenerService LocationTrackingService StreamingService SnapshotService NotificationListener
```

### View Parent App Logs:
```bash
adb logcat -s StreamingViewModel LiveLocationViewModel SnapshotsViewModel
```

### Check Running Services:
```bash
adb shell dumpsys activity services | grep myparentalcontrol
```

---

## Feature Status Summary

| Feature | Status | Notes |
|---------|--------|-------|
| Device Pairing | ✅ Working | 6-digit code pairing |
| Location Tracking | ✅ Ready | Requires location permission |
| Camera Streaming | ✅ Ready | Requires camera permission |
| Screen Mirroring | ✅ Ready | Requires MediaProjection permission |
| Audio Streaming | ✅ Ready | Requires microphone permission |
| Notification Sync | ✅ Ready | Requires manual Notification Access enable |
| Camera Snapshot | ✅ Ready | Uploads to Firebase Storage |
| Screen Snapshot | ✅ Ready | Requires MediaProjection permission |
| Recordings | ✅ Ready | Uploads to Firebase Storage |
| Real-time Status | ✅ Ready | Battery, online, network |
| Geofencing | 🔧 Partial | UI exists, alerts need testing |
