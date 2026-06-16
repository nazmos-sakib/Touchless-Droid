package com.example.touchlessdroid.module

import android.content.Context
import com.example.touchlessdroid.data.datasource.LocalModelDataSource
import com.example.touchlessdroid.data.repository.BluetoothDataTransfer
import com.example.touchlessdroid.data.repository.ObjectDetectionRepository
import com.example.touchlessdroid.domain.usecase.BluetoothManager
import com.example.touchlessdroid.domain.usecase.GestureDetector
import com.example.touchlessdroid.domain.usecase.GestureToCommandUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLocalModelDataSource(
        @ApplicationContext context: Context
    ): LocalModelDataSource {
        return LocalModelDataSource(context)
    }

    @Provides
    @Singleton
    fun provideObjectDetectionRepository(
        localModelDataSource: LocalModelDataSource
    ): ObjectDetectionRepository {
        return ObjectDetectionRepository(localModelDataSource)
    }

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return BluetoothManager(context)
    }

    @Provides
    fun provideBlDataTransfer(
        bluetoothManager: BluetoothManager
    ): BluetoothDataTransfer = bluetoothManager

    @Provides
    fun provideUseCase(
        sender: BluetoothDataTransfer
    ): GestureToCommandUseCase {
        return GestureToCommandUseCase(sender)
    }

    @Provides
    fun provideGestureDetector(): GestureDetector {
        return GestureDetector()
    }
}