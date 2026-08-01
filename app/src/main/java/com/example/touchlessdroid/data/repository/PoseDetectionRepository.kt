package com.example.touchlessdroid.data.repository

import android.graphics.Bitmap
import com.example.touchlessdroid.benchmark.FrameTiming
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.domain.model.camera.DetectedPose
import com.example.touchlessdroid.domain.model.camera.ReverseMapping

interface PoseDetectionRepository {
    fun initialize(configuration: InferenceConfiguration)

    suspend fun detectPose(
        bitmap: Bitmap,
        revMapping: ReverseMapping,
        infConfig: InferenceConfiguration,
        timing: FrameTiming
    ): List<DetectedPose>

    fun release()
}
