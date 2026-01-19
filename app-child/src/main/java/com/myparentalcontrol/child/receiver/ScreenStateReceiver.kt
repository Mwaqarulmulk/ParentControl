package com.myparentalcontrol.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned on")
                // TODO: Notify parent that screen is on
                // TODO: Start screen time tracking
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen turned off")
                // TODO: Notify parent that screen is off
                // TODO: Pause screen time tracking
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "User unlocked device")
                // TODO: Notify parent that device is unlocked
            }
        }
    }
}
