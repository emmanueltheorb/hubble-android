package com.orb.hubble.di

import android.content.Context
import androidx.room.Room
import com.orb.hubble.data.CacheStorageStrategy
import com.orb.hubble.data.MessageDao
import com.orb.hubble.data.PostDao
import com.orb.hubble.data.PostDatabase
import com.orb.hubble.data.StorageMediaExporter
import com.orb.hubble.data.VideoMediaExporter
import com.orb.hubble.repository.HubbleRepository
import com.orb.hubble.repository.PostRepository
import com.orb.hubble.repository.UserRepository
import com.orb.hubble.repository.UserRepositoryImpl
import com.orb.hubble.service.WebsocketService
import com.orb.hubble.utils.FileReader
import com.orb.hubble.utils.MediaExporter
import com.orb.hubble.utils.SaveToStorageStrategy
import com.orb.hubble.utils.StorageShareSheet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Provides
    @Singleton
    fun provideWebsocketService(): WebsocketService =
        WebsocketService()

    @Provides
    fun providePostDatabase(@ApplicationContext context: Context): PostDatabase =
        Room.databaseBuilder(context, PostDatabase::class.java, "post.db")
            .addMigrations(PostDatabase.migration4to5)
            .build()

    @Provides
    fun providePostDao(database: PostDatabase): PostDao = database.postDao

    @Provides
    fun provideMessageDao(database: PostDatabase): MessageDao = database.messageDao

    @Provides
    @Singleton
    fun providePostRepository(@ApplicationContext context: Context): PostRepository =
        PostRepository(fileReader = FileReader(context))

    @Provides
    @Singleton
    fun provideFileReader(@ApplicationContext context: Context): FileReader =
        FileReader(context = context)

    @Provides
    @Singleton
    fun provideUserRepository(websocketService: WebsocketService): UserRepository =
        UserRepositoryImpl(websocketService)

    @Provides
    @Singleton
    fun provideStorageMediaExporter(@ApplicationContext context: Context): StorageMediaExporter =
        StorageMediaExporter(context)

    @Provides
    @Singleton
    fun provideVideoMediaExporter(@ApplicationContext context: Context): VideoMediaExporter =
        VideoMediaExporter(context)

    @Provides
    @Singleton
    fun provideSaveToStorageStrategy(@ApplicationContext context: Context): SaveToStorageStrategy =
        CacheStorageStrategy(context)

    @Provides
    @Singleton
    fun provideStorageShareSheet(@ApplicationContext context: Context): StorageShareSheet =
        StorageShareSheet(context)
}