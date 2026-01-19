# Firebase Setup for Parental Control App

## ✅ Deployed via Firebase CLI

The following rules have been deployed using `firebase deploy`:

- ✅ **Realtime Database Rules** - Deployed
- ✅ **Firestore Rules** - Deployed  
- ✅ **Firestore Indexes** - Deployed
- ⚠️ **Storage Rules** - Requires manual setup (see below)

## ⚠️ CRITICAL: Enable These Services in Firebase Console

### 1. Enable Anonymous Authentication

1. Go to Firebase Console: https://console.firebase.google.com
2. Select your project: `parentalcontrolapp-fefed`
3. Go to **Authentication** → **Sign-in method**
4. Click **Add new provider**
5. Enable **Anonymous** ✅
6. Also enable **Email/Password** ✅
7. Click **Save**

### 2. Firebase Storage (OPTIONAL - Not Required)

**Note:** This app now stores snapshots as Base64 in Realtime Database, so Firebase Storage is **NOT required**. 

If you want larger file storage in the future:
1. Upgrade to Blaze plan
2. Go to Firebase Console: https://console.firebase.google.com/project/parentalcontrolapp-fefed/storage
3. Click **"Get Started"**
4. Then run: `firebase deploy --only storage`

## Firestore Security Rules

Copy and paste these rules into your Firebase Console:
`Firestore Database > Rules`

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is authenticated (includes anonymous)
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Pairing Codes - Allow read/write by any authenticated user
    match /pairingCodes/{codeId} {
      allow read, write: if isAuthenticated();
    }
    
    // Users collection - Parent profiles
    match /users/{userId} {
      allow read, write: if isAuthenticated();
      
      // Children subcollection
      match /children/{childId} {
        allow read, write: if isAuthenticated();
      }
    }
    
    // Parents collection
    match /parents/{parentId} {
      allow read, write: if isAuthenticated();
    }
    
    // Devices collection - Child devices
    match /devices/{deviceId} {
      allow read, write: if isAuthenticated();
    }
    
    // App Usage collection
    match /appUsage/{docId} {
      allow read, write: if isAuthenticated();
    }
    
    // Locations collection
    match /locations/{docId} {
      allow read, write: if isAuthenticated();
    }
    
    // Call Logs collection
    match /callLogs/{docId} {
      allow read, write: if isAuthenticated();
    }
    
    // SMS Logs collection
    match /smsLogs/{docId} {
      allow read, write: if isAuthenticated();
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

## Quick Setup Steps

### Step 1: Enable Authentication
1. Firebase Console → Authentication → Sign-in method
2. Enable **Anonymous** (required for child app)
3. Enable **Email/Password** (required for parent app)

### Step 2: Setup Firestore
1. Firebase Console → Firestore Database
2. Click "Create database" (if not exists)
3. Choose production mode
4. Go to **Rules** tab
5. Paste the Firestore rules above
6. Click **Publish**

### Step 3: Setup Realtime Database
1. Firebase Console → Realtime Database
2. Click "Create database" (if not exists)
3. Go to **Rules** tab
4. Paste the Realtime Database rules above
5. Click **Publish**

## Troubleshooting

### "PERMISSION_DENIED: Missing or insufficient permissions"
**Solution:** 
1. Ensure Anonymous authentication is enabled
2. Update Firestore rules and publish
3. Restart the app

### "Not authenticated" error
**Solution:**
1. Check that Anonymous auth is enabled in Firebase Console
2. Verify google-services.json is in the correct location
3. Check internet connectivity

### Pairing code not generating
**Solution:**
1. Check Firestore rules are updated
2. Verify the parent is signed in
3. Check Logcat for detailed errors

### Pairing code not working on child device
**Solution:**
1. Ensure both apps use the same Firebase project
2. Verify the code hasn't expired (5 minutes)
3. Enable Anonymous authentication
4. Update Firestore rules to allow authenticated writes

## Testing Checklist

- [ ] Anonymous auth enabled in Firebase
- [ ] Email/Password auth enabled in Firebase
- [ ] Firestore rules updated and published
- [ ] Realtime Database rules updated (if using streaming)
- [ ] Parent app can sign in
- [ ] Parent app can generate pairing code
- [ ] Child app can enter and verify code
- [ ] Child device appears in parent dashboard
