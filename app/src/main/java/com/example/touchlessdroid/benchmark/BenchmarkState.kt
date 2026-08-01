package com.example.touchlessdroid.benchmark

enum class BenchmarkState {
    IDLE, WARMING_UP, RECORDING, FINISHING, SAVING, COMPLETED
}

object BenchmarkStateMachine {
    fun canTransition(from: BenchmarkState, to: BenchmarkState): Boolean = when (from) {
        BenchmarkState.IDLE, BenchmarkState.COMPLETED -> to == BenchmarkState.WARMING_UP
        BenchmarkState.WARMING_UP -> to == BenchmarkState.RECORDING || to == BenchmarkState.IDLE
        BenchmarkState.RECORDING -> to == BenchmarkState.FINISHING || to == BenchmarkState.IDLE
        BenchmarkState.FINISHING -> to == BenchmarkState.SAVING || to == BenchmarkState.IDLE
        BenchmarkState.SAVING -> to == BenchmarkState.COMPLETED || to == BenchmarkState.IDLE
    }
}

data class BenchmarkUiState(
    val state: BenchmarkState = BenchmarkState.IDLE,
    val remainingSeconds: Int = 0,
    val savedFileName: String? = null,
    val savedLocation: String? = null,
    val error: String? = null
) {
    val controlsEnabled: Boolean
        get() = state == BenchmarkState.IDLE || state == BenchmarkState.COMPLETED

    val statusText: String
        get() = when (state) {
            BenchmarkState.IDLE -> "Ready to benchmark"
            BenchmarkState.WARMING_UP -> "Warming up: $remainingSeconds s"
            BenchmarkState.RECORDING -> "Recording: $remainingSeconds s"
            BenchmarkState.FINISHING -> "Finishing current frame..."
            BenchmarkState.SAVING -> "Saving results..."
            BenchmarkState.COMPLETED -> "Benchmark completed: ${savedFileName.orEmpty()}"
        }
}
