package com.example.touchlessdroid.benchmark

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameTimingTest {
    @Test
    fun completedMetrics_calculatesEveryDurationFromRawTimestamps() {
        val timing = FrameTiming(
            runId = "run",
            frameNumber = 7,
            device = "device",
            runtime = "runtime",
            backend = "backend",
            precision = "precision",
            frameAcceptedNs = 1_000,
            measurementStartNs = 500,
            shouldRecord = true
        ).apply {
            bitmapReadyNs = 1_100
            modelInputReadyNs = 1_300
            inferenceCompletedNs = 1_700
            postprocessingCompletedNs = 1_900
            gestureCompletedNs = 2_000
            bluetoothWriteStartedNs = 2_050
            bluetoothWriteCompletedNs = 2_150
        }

        val metrics = timing.completedMetrics()

        assertEquals(500, metrics.elapsedSinceRunStartNs)
        assertEquals(100, metrics.imageConversionNs)
        assertEquals(200, metrics.modelPreprocessingNs)
        assertEquals(300, metrics.totalPreprocessingNs)
        assertEquals(400, metrics.inferenceNs)
        assertEquals(200, metrics.postprocessingNs)
        assertEquals(100, metrics.gestureNs)
        assertEquals(50, metrics.bluetoothQueueNs)
        assertEquals(100, metrics.bluetoothWriteNs)
        assertEquals(150, metrics.gestureToBluetoothNs)
        assertEquals(1_150, metrics.endToEndNs)
    }
}
