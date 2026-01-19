package com.myparentalcontrol.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackageChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageChangeReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.data?.schemeSpecificPart ?: return
        
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                Log.d(TAG, "Package installed: $packageName")
                // TODO: Report new app installation to parent
                // TODO: Check if app should be blocked
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                Log.d(TAG, "Package removed: $packageName")
                // TODO: Report app uninstallation to parent
            }
        }
    }
}
