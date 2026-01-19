# Firebase Setup for Parental Control App

## ⚠️ IMPORTANT: Enable Anonymous Authentication

Before the apps can work, you MUST enable **Anonymous Authentication** in Firebase:

1. Go to Firebase Console: https://console.firebase.google.com
2. Select your project: `parentalcontrolapp-fefed`
3. Go to **Authentication** → **Sign-in method**
4. Click **Add new provider**
5. Enable **Anonymous** authentication
6. Click **Save**

## Firestore Security Rules

Copy and paste these rules into your Firebase Console:
`Firestore Database > Rules`

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is authenticated (includes anonymous)
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns this resource
    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }
    
    // Pairing Codes - Allow read/write by any authenticated user (including anonymous)
    match /pairingCodes/{codeId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update: if isAuthenticated();
      allow delete: if false;
    }
    
    // Users collection - Parent profiles
    match /users/{userId} {
      allow read, write: if isAuthenticated();
      
      // Children subcollection - Allow authenticated users to write (for pairing)
      match /children/{childId} {
        allow read, write: if isAuthenticated();
      }
    }
    
    // Parents collection
    match /parents/{parentId} {
      allow read, write: if isOwner(parentId);
    }
    
    // Devices collection - Child devices (allow any authenticated user to create/update)
    match /devices/{deviceId} {
      allow read: if isAuthenticated();
      allow create, update: if isAuthenticated();
      allow delete: if false; 
                     resource.data.deviceId == deviceId);
      
      // Allow create/update by anyone (for initial pairing)
      allow create, update: if true;
      allow delete: if false;
    }
    
    // App Usage collection
    match /appUsage/{docId} {
      allow read: if isAuthenticated();
      allow write: if true;
    }
    
    // Locations collection
    match /locations/{docId} {
      allow read: if isAuthenticated();
      allow write: if true;
    }
    
    // Call Logs collection
    match /callLogs/{docId} {
      allow read: if isAuthenticated();
      allow write: if true;
    }
    
    // SMS Logs collection
    match /smsLogs/{docId} {
      allow read: if isAuthenticated();
      allow write: if true;
    }
    
    // Commands collection
    match /commands/{docId} {
      allow read, write: if isAuthenticated();
    }
    
    // Blocked Apps collection
    match /blockedApps/{docId} {
      allow read, write: if isAuthenticated();
    }
    
    // Screen Time Rules collection
    match /screenTimeRules/{docId} {
      allow read, write: if isAuthenticated();
    }
  }
}
```

## Realtime Database Rules

Copy and paste these rules into your Firebase Console:
`Realtime Database > Rules`

```json
{
  "rules": {
    "presence": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "signaling": {
      "$sessionId": {
        ".read": true,
        ".write": true
      }
    },
    "commands": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

## Firebase Authentication Setup

1. Go to Firebase Console > Authentication > Sign-in method
2. Enable the following providers:
   - **Email/Password** - For parent registration
   - **Anonymous** - For guest mode testing

## Firebase Project Setup Steps

1. **Create Firebase Project** (if not done):
   - Go to https://console.firebase.google.com
   - Create new project or use existing: `parentalcontrolapp-fefed`

2. **Add Android Apps**:
   - Add app with package: `com.myparentalcontrol.child`
   - Add app with package: `com.myparentalcontrol.parent`
   - Download `google-services.json` and place in both `app-child/` and `app-parent/`

3. **Enable Firestore**:
   - Go to Firestore Database
   - Create database (start in test mode initially, then apply rules above)

4. **Enable Realtime Database**:
   - Go to Realtime Database
   - Create database (start in test mode initially, then apply rules above)

5. **Enable Cloud Messaging (FCM)**:
   - Go to Cloud Messaging
   - Note your Server Key for push notifications

## Troubleshooting

### "Not authenticated" error
- Make sure Firebase Auth is enabled
- Check that google-services.json is in the correct location
- Verify authentication providers are enabled

### Pairing code not generating
- Check Firestore rules allow creating pairingCodes
- Verify network connectivity
- Check Logcat for detailed error messages

### Pairing code not working on child device
- Ensure both apps use the same Firebase project
- Verify the code hasn't expired (5 minutes)
- Check that the code hasn't been used already
