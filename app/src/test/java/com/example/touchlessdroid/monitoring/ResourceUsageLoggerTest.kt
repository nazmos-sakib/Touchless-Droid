package com.example.touchlessdroid.monitoring

import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceUsageLoggerTest {
    @Test
    fun `cpu percentage uses cpu delta divided by wall delta`() {
        val result = ResourceUsageLogger.calculateProcessCpuPercent(
            previousCpuTimeMs = 1_000,
            currentCpuTimeMs = 1_750,
            previousWallTimeMs = 5_000,
            currentWallTimeMs = 6_000
        )

        assertEquals(75.0, result, 0.001)
    }

    @Test
    fun `cpu percentage can exceed one hundred for multicore work`() {
        val result = ResourceUsageLogger.calculateProcessCpuPercent(
            previousCpuTimeMs = 1_000,
            currentCpuTimeMs = 3_000,
            previousWallTimeMs = 5_000,
            currentWallTimeMs = 6_000
        )

        assertEquals(200.0, result, 0.001)
    }

    @Test
    fun `invalid wall interval returns zero`() {
        val result = ResourceUsageLogger.calculateProcessCpuPercent(
            previousCpuTimeMs = 1_000,
            currentCpuTimeMs = 1_100,
            previousWallTimeMs = 5_000,
            currentWallTimeMs = 5_000
        )

        assertEquals(0.0, result, 0.001)
    }
}
