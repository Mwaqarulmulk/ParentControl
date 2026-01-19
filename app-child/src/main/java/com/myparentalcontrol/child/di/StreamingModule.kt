package com.myparentalcontrol.child.di

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.myparentalcontrol.child.streaming.audio.DeviceAudioManager
import com.myparentalcontrol.child.streaming.audio.MicrophoneManager
import com.myparentalcontrol.child.streaming.core.SignalingManager
import com.myparentalcontrol.child.streaming.core.WebRTCManager
import com.myparentalcontrol.child.streaming.video.CameraManager
import com.myparentalcontrol.child.streaming.video.ScreenCaptureManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for streaming dependencies in child app
 */
@Module
@InstallIn(SingletonComponent::class)
object StreamingModule {
    
    @Provides
    @Singleton
    fun provideWebRTCManager(
        @ApplicationContext context: Context
    ): WebRTCManager {
        return WebRTCManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSignalingManager(
        database: FirebaseDatabase
    ): SignalingManager {
        return SignalingManager(database)
    }
    
    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager {
        return CameraManager(context)
    }
    
    @Provides
    @Singleton
    fun provideScreenCaptureManager(
        @ApplicationContext context: Context
    ): ScreenCaptureManager {
        return ScreenCaptureManager(context)
    }
    
    @Provides
    @Singleton
    fun provideMicrophoneManager(
        @ApplicationContext context: Context
    ): MicrophoneManager {
        return MicrophoneManager(context)
    }
    
    @Provides
    @Singleton
    fun provideDeviceAudioManager(
        @ApplicationContext context: Context
    ): DeviceAudioManager {
        return DeviceAudioManager(context)
    }
}
