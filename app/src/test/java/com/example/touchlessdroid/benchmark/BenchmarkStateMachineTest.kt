package com.example.touchlessdroid.benchmark

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkStateMachineTest {
    @Test
    fun normalBenchmarkSequence_isAllowed() {
        val sequence = listOf(
            BenchmarkState.IDLE,
            BenchmarkState.WARMING_UP,
            BenchmarkState.RECORDING,
            BenchmarkState.FINISHING,
            BenchmarkState.SAVING,
            BenchmarkState.COMPLETED
        )

        sequence.zipWithNext().forEach { (from, to) ->
            assertTrue("$from -> $to", BenchmarkStateMachine.canTransition(from, to))
        }
    }

    @Test
    fun simultaneousStart_andSkippingFinish_areRejected() {
        assertFalse(
            BenchmarkStateMachine.canTransition(
                BenchmarkState.WARMING_UP,
                BenchmarkState.WARMING_UP
            )
        )
        assertFalse(
            BenchmarkStateMachine.canTransition(
                BenchmarkState.RECORDING,
                BenchmarkState.SAVING
            )
        )
    }
}
