package com.example.touchlessdroid.monitoring

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.example.touchlessdroid.utils.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/** Logs this application's process CPU and PSS memory usage once per second. */
class ResourceUsageLogger(context: Context) {
    private val activityManager =
        context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private var samplingJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (samplingJob?.isActive == true) return

        samplingJob = scope.launch(Dispatchers.IO) {
            var sampleNumber = 0L
            val monitoringStartNs = SystemClock.elapsedRealtimeNanos()
            var previousCpuTimeMs = Process.getElapsedCpuTime()
            var previousWallTimeMs = SystemClock.elapsedRealtime()

            while (isActive) {
                delay(SAMPLE_INTERVAL_MS)

                val currentCpuTimeMs = Process.getElapsedCpuTime()
                val currentWallTimeMs = SystemClock.elapsedRealtime()
                val cpuPercent = calculateProcessCpuPercent(
                    previousCpuTimeMs = previousCpuTimeMs,
                    currentCpuTimeMs = currentCpuTimeMs,
                    previousWallTimeMs = previousWallTimeMs,
                    currentWallTimeMs = currentWallTimeMs
                )
                previousCpuTimeMs = currentCpuTimeMs
                previousWallTimeMs = currentWallTimeMs

                val totalPssMb = runCatching { getProcessMemoryMb() }
                    .getOrElse { error ->
                        Log.w(LOG_TAG, "Unable to sample process memory", error)
                        Double.NaN
                    }

                sampleNumber += 1
                val elapsedMs =
                    (SystemClock.elapsedRealtimeNanos() - monitoringStartNs) / 1_000_000L
                Log.i(
                    LOG_TAG,
                    String.format(
                        Locale.US,
                        "sample=%d,elapsed_ms=%d,device=%s,cpu_percent=%.2f,total_pss_mb=%.2f",
                        sampleNumber,
                        elapsedMs,
                        Utility.getDeviceName(),
                        cpuPercent,
                        totalPssMb
                    )
                )
            }
        }
    }

    fun stop() {
        samplingJob?.cancel()
        samplingJob = null
    }

    private fun getProcessMemoryMb(): Double {
        val memoryInfo = activityManager
            .getProcessMemoryInfo(intArrayOf(Process.myPid()))
            .first()
        return memoryInfo.totalPss / 1024.0
    }

    companion object {
        const val LOG_TAG = "RESOURCE_USAGE"
        private const val SAMPLE_INTERVAL_MS = 1_000L

        internal fun calculateProcessCpuPercent(
            previousCpuTimeMs: Long,
            currentCpuTimeMs: Long,
            previousWallTimeMs: Long,
            currentWallTimeMs: Long
        ): Double {
            val cpuDeltaMs = currentCpuTimeMs - previousCpuTimeMs
            val wallDeltaMs = currentWallTimeMs - previousWallTimeMs
            if (cpuDeltaMs < 0L || wallDeltaMs <= 0L) return 0.0
            return cpuDeltaMs.toDouble() / wallDeltaMs * 100.0
        }
    }
}
