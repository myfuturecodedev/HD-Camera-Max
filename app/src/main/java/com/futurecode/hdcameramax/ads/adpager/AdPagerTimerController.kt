package com.futurecode.hdcameramax.ads.adpager

import android.os.CountDownTimer

class AdPagerTimerController {

    private val activeTimers = mutableMapOf<String, CountDownTimer>()

    fun bind(
        pageKey: String,
        durationMs: Long,
        onTick: (secondsRemaining: Long) -> Unit,
        onUnlocked: () -> Unit
    ) {
        activeTimers.remove(pageKey)?.cancel()

        if (durationMs <= 0L) {
            onTick(0L)
            onUnlocked()
            return
        }

        onTick(durationMs.toDisplaySeconds())

        activeTimers[pageKey] = object : CountDownTimer(durationMs, TICK_MS) {
            override fun onTick(millisUntilFinished: Long) {
                onTick(millisUntilFinished.toDisplaySeconds())
            }

            override fun onFinish() {
                activeTimers.remove(pageKey)
                onTick(0L)
                onUnlocked()
            }
        }.start()
    }

    fun detach(pageKey: String) {
        activeTimers.remove(pageKey)?.cancel()
    }

    fun clear() {
        activeTimers.values.forEach { it.cancel() }
        activeTimers.clear()
    }

    private fun Long.toDisplaySeconds(): Long {
        return (this + 999L) / 1_000L
    }

    companion object {
        private const val TICK_MS = 250L
    }
}