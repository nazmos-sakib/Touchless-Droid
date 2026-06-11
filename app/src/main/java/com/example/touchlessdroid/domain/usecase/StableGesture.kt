package com.example.touchlessdroid.domain.usecase

class StableGesture {

    private val history = ArrayDeque<String>()
    private val maxSize = 5

    fun update(newGesture: String): String {

        history.addLast(newGesture)
        if (history.size > maxSize) history.removeFirst()

        val counts = history.groupingBy { it }.eachCount()

        val best = counts.maxByOrNull { it.value }?.key ?: "NONE"

        return if (counts[best]!! >= 3) best else "NONE"
    }
}