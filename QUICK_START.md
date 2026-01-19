# 🚀 QUICK START GUIDE - Parent & Child Apps

## ⚡ FASTEST PATH TO RUNNING APP

### **Prerequisites (One-Time Setup)**

1. **Firebase Console** (https://console.firebase.google.com)
   - Go to your project
   - Enable **Firestore Database** (test mode)
   - Enable **Realtime Database** (test mode)
   - Enable **Authentication** (Anonymous)
   - ✅ google-services.json already added to both apps

2. **Google Maps API Key**
   - Get key: https://console.cloud.google.com/
   - Enable "Maps SDK for Android"
   - Add to `local.properties`:
   ```properties
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```

---

## 📱 **INSTALL APPS** (2 minutes)

```bash
# Parent app
./gradlew :app-parent:installDebug

# Child app (on different device or emulator)
./gradlew :app-child:installDebug
```

---

## 🔗 **PAIR DEVICES** (30 seconds)

### On Parent Device:
1. Open parent app
2. Tap **"+"** button (bottom right)
3. Tap **"Generate Pairing Code"**
4. See code: **`583729`** (example)
5. **Keep screen open**

### On Child Device:
1. Open child app
2. Enter code: **`583729`**
3. Tap **"Pair Device"**
4. Wait for ✓ success

### Back on Parent:
- Device automatically appears in dashboard
- Tap device card to see details

---

## ✅ **TEST FEATURES** (2 minutes)

### 1. **Location** (15 seconds)
```
Parent: Device Details → Location tab
→ See map with child location
→ Tap "Update Location Now"
→ See location refresh
```

### 2. **Notifications** (30 seconds)
```
Parent: Device Details → Notifications tab
Child: Open any app, receive notification
Parent: See notification appear in list
```

### 3. **Camera Control** (30 seconds)
```
Parent: Device Details → Control tab
→ Tap "Start Camera Stream"
Child: Camera LED lights up (camera active)
Parent: Tap "Stop Camera Stream"
Child: Camera LED turns off
```

### 4. **Screen Mirror** (30 seconds)
```
Parent: Control tab → "Start Screen Mirror"
Child: Screen is being captured
Parent: "Stop Screen Mirror"
```

### 5. **Audio Stream** (15 seconds)
```
Parent: Control tab → "Start Audio Stream"
Child: Microphone active
Parent: "Stop Audio Stream"
```

---

## 🎯 **KEY SCREENS**

### **Parent App:**
```
Dashboard (Main)
├─ Child Device Card
│  ├─ Device name
│  ├─ Online status
│  ├─ Battery level
│  └─ Network type
│
└─ Device Details (Tap card)
   ├─ Overview Tab → Device info
   ├─ Location Tab → Map + history
   ├─ Notifications Tab → All notifications
   └─ Control Tab → Commands
```

### **Child App:**
```
Pairing Screen → Enter 6-digit code
↓
Home Screen → Shows active features
```

---

## 🔥 **FIREBASE STRUCTURE**

### **Quick Check (Firebase Console):**
```
Firestore:
├─ pairingCodes/{code} → Generated codes
├─ devices/{deviceId} → Child device data
├─ users/{parentId}/children → Paired list
├─ locations/{deviceId}/history → GPS history
└─ notifications/{deviceId}/history → Captured notifications

Realtime Database:
└─ commands/{deviceId}/{commandId} → Parent commands
```

---

## 🛠️ **TROUBLESHOOTING**

### **Device Not Appearing:**
- ✓ Check internet on both devices
- ✓ Verify Firebase services enabled
- ✓ Check code expiration (< 5 min)
- ✓ Try regenerating code

### **Location Not Showing:**
- ✓ Enable location on child device
- ✓ Grant location permission
- ✓ Wait 15 minutes for first auto-update
- ✓ Or tap "Update Location Now"

### **Notifications Not Captured:**
- ✓ Enable notification access on child
- ✓ Settings → Apps → Child App → Notification access
- ✓ Toggle ON

### **Camera Not Starting:**
- ✓ Grant camera permission on child
- ✓ Check if camera is in use by other app
- ✓ Restart child app

### **Maps Not Loading:**
- ✓ Add Google Maps API key to local.properties
- ✓ Enable "Maps SDK for Android" in Google Cloud Console
- ✓ Check billing is enabled

---

## 📊 **STATUS CHECK**

### **Is Everything Working?**
```bash
# Build without errors
./gradlew build

# Check Firebase connection
# Parent app → Dashboard should load
# Child app → Should pair successfully
```

### **Firebase Test Mode Rules:**

**Firestore:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

**Realtime Database:**
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

---

## 🎯 **5-MINUTE FULL TEST**

```
✓ Install both apps (1 min)
✓ Pair devices (30 sec)
✓ Test location (30 sec)
✓ Test notifications (1 min)
✓ Test camera control (1 min)
✓ Test screen mirror (30 sec)
✓ Test audio stream (30 sec)
✓ Test ring device (10 sec)
```

---

## 📝 **COMMAND REFERENCE**

### **All Available Commands:**
```kotlin
// Camera
startCameraStream(cameraType = "front", withAudio = true)
stopCameraStream()

// Screen
startScreenMirror(withAudio = true)
stopScreenMirror()

// Audio
startAudioStream()
stopAudioStream()

// Location
requestLocationUpdate()

// Utility
ringDevice()
syncData()
```

---

## 🔐 **PERMISSIONS TO GRANT**

### **On Child Device (Runtime):**
1. ✓ Camera
2. ✓ Microphone
3. ✓ Location
4. ✓ Notification Access (Settings)

### **Notification Access Setup:**
```
Child Device:
Settings
→ Apps
→ Parental Control (Child)
→ Notification access
→ Toggle ON
```

---

## 📱 **FEATURES MATRIX**

| Feature | Parent App | Child App |
|---------|-----------|-----------|
| Pairing | Generate code | Enter code |
| Dashboard | View all children | - |
| Location | View on map | Track every 15 min |
| Notifications | View all | Capture all |
| Camera | Send start/stop | Stream video |
| Screen | Send start/stop | Mirror screen |
| Audio | Send start/stop | Stream mic |
| Ring | Send command | Play sound |

---

## 🎉 **DONE!**

Your app is fully functional. All features are:
- ✅ Implemented
- ✅ Integrated
- ✅ Tested
- ✅ Ready to use

**Time to complete setup: ~5 minutes**
**Time to test all features: ~5 minutes**

---

## 📚 **Full Documentation:**
- `INTEGRATION_COMPLETE.md` - Complete integration guide
- `PARENT_APP_COMPLETE.md` - Parent app details
- `PAIRING_FLOW_COMPLETE.md` - Pairing flow explained
- `FIREBASE_QUICK_SETUP.md` - Firebase setup steps
- `CHILD_APP_FEATURES_STATUS.md` - Feature status

---

**Ready to go! 🚀**
