package com.myparentalcontrol.parent.di

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.myparentalcontrol.parent.streaming.core.SignalingManager
import com.myparentalcontrol.parent.streaming.core.WebRTCManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for streaming dependencies in parent app
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
}
