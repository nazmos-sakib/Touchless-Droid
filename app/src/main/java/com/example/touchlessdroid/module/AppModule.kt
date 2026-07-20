package com.example.touchlessdroid.module

import android.content.Context
import com.example.touchlessdroid.data.datasource.ONNXModelDataSource
import com.example.touchlessdroid.data.datasource.PyTorchModelDataSource
import com.example.touchlessdroid.data.datasource.TFLiteModelDataSource
import com.example.touchlessdroid.data.repository.BluetoothDataTransfer
import com.example.touchlessdroid.data.repository.NCNNPoseRepository
import com.example.touchlessdroid.data.repository.ONNXPoseDetectionRepository
import com.example.touchlessdroid.data.repository.PyTorchPoseDetectionRepository
import com.example.touchlessdroid.data.repository.TFLitePoseDetectionRepository
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
    fun provideTfliteModelDataSource(
        @ApplicationContext context: Context
    ): TFLiteModelDataSource {
        return TFLiteModelDataSource(context)
    }

    @Provides
    fun provideTFLitePoseDetectionRepository(
        tfliteModelDataSource: TFLiteModelDataSource
    ): TFLitePoseDetectionRepository {
        return TFLitePoseDetectionRepository(tfliteModelDataSource)
    }

   @Provides
   @Singleton
   fun provideONNXModelDataSource(
       @ApplicationContext context: Context
   ): ONNXModelDataSource{
       return ONNXModelDataSource(context)
   }

   @Provides
   fun provideONNXPoseDetectionRepository(
       dataSource: ONNXModelDataSource
   ): ONNXPoseDetectionRepository{
       return ONNXPoseDetectionRepository(dataSource)
   }

   @Provides
   @Singleton
   fun providePyTorchModelDataSource(
       @ApplicationContext context: Context
   ): PyTorchModelDataSource{
       return PyTorchModelDataSource(context)
   }

   @Provides
   fun providePyTorchPoseDetectionRepository(
       dataSource: PyTorchModelDataSource
   ): PyTorchPoseDetectionRepository{
       return PyTorchPoseDetectionRepository(dataSource)
   }

   @Provides
   fun provideNCNNPoseDetectionRepository(
       @ApplicationContext context: Context
   ): NCNNPoseRepository {
       return NCNNPoseRepository(context)
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