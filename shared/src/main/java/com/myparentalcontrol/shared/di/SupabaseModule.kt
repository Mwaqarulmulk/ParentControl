package com.myparentalcontrol.shared.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import com.myparentalcontrol.shared.data.supabase.SupabaseConfig
import com.myparentalcontrol.shared.data.supabase.SupabaseRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            // Install Supabase plugins
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
    
    @Provides
    @Singleton
    fun provideSupabaseRepository(supabaseClient: SupabaseClient): SupabaseRepository {
        return SupabaseRepository(supabaseClient)
    }
}
