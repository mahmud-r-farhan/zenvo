package com.bengalbytes.zenvo.core

import java.util.concurrent.TimeUnit

/**
 * Pure logic for the focus timer.
 * We calculate remaining time based on timestamps to avoid drift and issues with Doze mode.
 */
class TimerEngine {

    fun calculateRemainingMillis(startTimeMillis: Long, durationMillis: Long, currentTimeMillis: Long): Long {
        if (startTimeMillis <= 0) return 0
        val elapsed = currentTimeMillis - startTimeMillis
        val remaining = durationMillis - elapsed
        return remaining.coerceAtLeast(0)
    }

    fun isTimerRunning(startTimeMillis: Long, durationMillis: Long, currentTimeMillis: Long): Boolean {
        if (startTimeMillis <= 0) return false
        return calculateRemainingMillis(startTimeMillis, durationMillis, currentTimeMillis) > 0
    }

    fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
