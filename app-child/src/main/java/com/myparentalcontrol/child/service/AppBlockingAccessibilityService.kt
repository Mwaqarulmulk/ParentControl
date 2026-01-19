package com.myparentalcontrol.child.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppBlockingAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockingService"
        
        // List of blocked package names (to be populated from Firebase)
        private val blockedApps = mutableSetOf<String>()
        
        fun updateBlockedApps(apps: Set<String>) {
            blockedApps.clear()
            blockedApps.addAll(apps)
            Log.d(TAG, "Updated blocked apps: $blockedApps")
        }
        
        fun addBlockedApp(packageName: String) {
            blockedApps.add(packageName)
            Log.d(TAG, "Added blocked app: $packageName")
        }
        
        fun removeBlockedApp(packageName: String) {
            blockedApps.remove(packageName)
            Log.d(TAG, "Removed blocked app: $packageName")
        }
        
        fun isAppBlocked(packageName: String): Boolean {
            return blockedApps.contains(packageName)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = it.packageName?.toString() ?: return
                
                if (isAppBlocked(packageName)) {
                    Log.d(TAG, "Blocked app detected: $packageName")
                    blockApp(packageName)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    private fun blockApp(packageName: String) {
        // Navigate to home screen to block the app
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        
        // TODO: Show a blocking overlay or notification
        Log.d(TAG, "Blocked app $packageName - navigated to home")
    }
}
