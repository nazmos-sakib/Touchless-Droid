package com.example.touchlessdroid.benchmark

import android.os.SystemClock
import com.example.touchlessdroid.domain.model.InferenceConfiguration

class FrameTiming(
    val runId: String?,
    val frameNumber: Long?,
    val device: String,
    val runtime: String,
    val backend: String,
    val precision: String,
    val frameAcceptedNs: Long,
    val measurementStartNs: Long?,
    val shouldRecord: Boolean
) {
    var bitmapReadyNs: Long = 0L
    var modelInputReadyNs: Long = 0L
    var inferenceCompletedNs: Long = 0L
    var postprocessingCompletedNs: Long = 0L
    var gestureCompletedNs: Long = 0L
    var bluetoothWriteStartedNs: Long = 0L
    var bluetoothWriteCompletedNs: Long = 0L

    fun completedMetrics(): CompletedFrameMetrics {
        check(shouldRecord && runId != null && frameNumber != null && measurementStartNs != null)
        check(
            listOf(
                frameAcceptedNs,
                bitmapReadyNs,
                modelInputReadyNs,
                inferenceCompletedNs,
                postprocessingCompletedNs,
                gestureCompletedNs,
                bluetoothWriteStartedNs,
                bluetoothWriteCompletedNs
            ).zipWithNext().all { (start, end) -> start > 0L && end >= start }
        ) { "Frame timing is incomplete or non-monotonic" }
        return CompletedFrameMetrics(
            runId = runId,
            frameNumber = frameNumber,
            device = device,
            runtime = runtime,
            backend = backend,
            precision = precision,
            elapsedSinceRunStartNs = frameAcceptedNs - measurementStartNs,
            imageConversionNs = bitmapReadyNs - frameAcceptedNs,
            modelPreprocessingNs = modelInputReadyNs - bitmapReadyNs,
            totalPreprocessingNs = modelInputReadyNs - frameAcceptedNs,
            inferenceNs = inferenceCompletedNs - modelInputReadyNs,
            postprocessingNs = postprocessingCompletedNs - inferenceCompletedNs,
            gestureNs = gestureCompletedNs - postprocessingCompletedNs,
            bluetoothQueueNs = bluetoothWriteStartedNs - gestureCompletedNs,
            bluetoothWriteNs = bluetoothWriteCompletedNs - bluetoothWriteStartedNs,
            gestureToBluetoothNs = bluetoothWriteCompletedNs - gestureCompletedNs,
            endToEndNs = bluetoothWriteCompletedNs - frameAcceptedNs
        )
    }

    companion object {
        fun accepted(
            snapshot: BenchmarkFrameSnapshot,
            configuration: InferenceConfiguration,
            device: String
        ) = FrameTiming(
            runId = snapshot.runId,
            frameNumber = snapshot.frameNumber,
            device = device,
            runtime = configuration.runtime.label,
            backend = configuration.delegate.label,
            precision = configuration.precision.label,
            frameAcceptedNs = SystemClock.elapsedRealtimeNanos(),
            measurementStartNs = snapshot.measurementStartNs,
            shouldRecord = snapshot.shouldRecord
        )
    }
}

data class CompletedFrameMetrics(
    val runId: String,
    val frameNumber: Long,
    val device: String,
    val runtime: String,
    val backend: String,
    val precision: String,
    val elapsedSinceRunStartNs: Long,
    val imageConversionNs: Long,
    val modelPreprocessingNs: Long,
    val totalPreprocessingNs: Long,
    val inferenceNs: Long,
    val postprocessingNs: Long,
    val gestureNs: Long,
    val bluetoothQueueNs: Long,
    val bluetoothWriteNs: Long,
    val gestureToBluetoothNs: Long,
    val endToEndNs: Long
)

data class BenchmarkFrameSnapshot(
    val runId: String?,
    val frameNumber: Long?,
    val measurementStartNs: Long?,
    val shouldRecord: Boolean
)
