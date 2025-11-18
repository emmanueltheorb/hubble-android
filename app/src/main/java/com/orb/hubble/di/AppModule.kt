package com.orb.hubble.di

import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.service.WebsocketService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideWebsocketService(): WebsocketService =
        WebsocketService()

    @Provides
    @Singleton
    fun provideRepository(websocketService: WebsocketService): HubbleRepository =
        HubbleRepository(websocketService)
}