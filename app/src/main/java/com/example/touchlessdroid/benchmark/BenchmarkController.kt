package com.example.touchlessdroid.benchmark

import android.content.Context
import android.os.SystemClock
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.utils.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

class BenchmarkController(context: Context) {
    private val csvWriter = BenchmarkCsvWriter(context)
    private val nextFrameNumber = AtomicLong(0)
    private val sessionActive = AtomicBoolean(false)
    private val finishMutex = Mutex()
    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    @Volatile private var runId: String? = null
    @Volatile private var measurementStartNs: Long? = null
    @Volatile private var runJob: Job? = null

    fun start(scope: CoroutineScope, configuration: InferenceConfiguration): Boolean {
        if (!_uiState.value.controlsEnabled || !sessionActive.compareAndSet(false, true)) return false
        val id = UUID.randomUUID().toString()
        runId = id
        measurementStartNs = null
        nextFrameNumber.set(0)
        val filename = filename(configuration, id)
        try {
            csvWriter.open(scope, filename)
        } catch (e: Exception) {
            sessionActive.set(false)
            _uiState.value = BenchmarkUiState(error = e.message)
            return false
        }
        transition(BenchmarkState.WARMING_UP, WARM_UP_SECONDS)
        runJob = scope.launch {
            try {
                countDown(WARM_UP_SECONDS)
                measurementStartNs = SystemClock.elapsedRealtimeNanos()
                transition(BenchmarkState.RECORDING, RECORDING_SECONDS)
                countDown(RECORDING_SECONDS)
                transition(BenchmarkState.FINISHING)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                csvWriter.cancel()
                sessionActive.set(false)
                _uiState.value = BenchmarkUiState(error = e.message)
            }
        }
        return true
    }

    fun reportStartError(message: String) {
        if (_uiState.value.controlsEnabled) {
            _uiState.value = _uiState.value.copy(error = message)
        }
    }

    fun snapshotAcceptedFrame(): BenchmarkFrameSnapshot {
        val state = _uiState.value.state
        val recording = state == BenchmarkState.RECORDING
        return BenchmarkFrameSnapshot(
            runId = runId,
            frameNumber = if (recording) nextFrameNumber.incrementAndGet() else null,
            measurementStartNs = if (recording) measurementStartNs else null,
            shouldRecord = recording
        )
    }

    fun submit(metrics: CompletedFrameMetrics) = csvWriter.submit(metrics)

    suspend fun finishIfReady(hasInFlightFrame: Boolean) {
        finishMutex.withLock {
            if (_uiState.value.state != BenchmarkState.FINISHING || hasInFlightFrame) return
            transition(BenchmarkState.SAVING)
            val file = csvWriter.close()
            transition(
                BenchmarkState.COMPLETED,
                savedFileName = file.name,
                savedLocation = file.absolutePath
            )
            sessionActive.set(false)
        }
    }

    suspend fun cancel() {
        runJob?.cancel()
        runJob = null
        csvWriter.cancel()
        sessionActive.set(false)
        _uiState.value = BenchmarkUiState()
    }

    private suspend fun countDown(seconds: Int) {
        val deadlineNs = SystemClock.elapsedRealtimeNanos() + seconds * NANOS_PER_SECOND
        while (true) {
            val remainingNs = deadlineNs - SystemClock.elapsedRealtimeNanos()
            if (remainingNs <= 0L) break
            val remainingSeconds = ceil(remainingNs / NANOS_PER_SECOND.toDouble()).toInt()
            _uiState.value = _uiState.value.copy(remainingSeconds = remainingSeconds)
            delay(minOf(1_000L, (remainingNs + NANOS_PER_MILLISECOND - 1) / NANOS_PER_MILLISECOND))
        }
    }

    private fun transition(
        state: BenchmarkState,
        remainingSeconds: Int = 0,
        savedFileName: String? = null,
        savedLocation: String? = null
    ) {
        check(BenchmarkStateMachine.canTransition(_uiState.value.state, state))
        _uiState.value = BenchmarkUiState(state, remainingSeconds, savedFileName, savedLocation)
    }

    private fun filename(configuration: InferenceConfiguration, id: String): String {
        fun clean(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return listOf(
            Utility.getDeviceName(),
            configuration.runtime.label,
            configuration.delegate.label,
            configuration.precision.label,
            id.take(8),
            timestamp
        ).joinToString("_") { clean(it) } + ".csv"
    }

    companion object {
        const val WARM_UP_SECONDS = 10
        const val RECORDING_SECONDS = 50
        private const val NANOS_PER_SECOND = 1_000_000_000L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
