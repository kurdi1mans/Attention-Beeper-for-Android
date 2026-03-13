package com.attentionbeeper

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure session state — no Android framework scheduling dependencies.
 * Scheduling lives in BeepService; this class only holds observable state.
 */
class SessionViewModel : ViewModel() {

    private val _sessionRunning = MutableStateFlow(false)
    val sessionRunning: StateFlow<Boolean> = _sessionRunning.asStateFlow()

    private val _intervalValue = MutableStateFlow(60)
    val intervalValue: StateFlow<Int> = _intervalValue.asStateFlow()

    private val _intervalUnit = MutableStateFlow(IntervalUnit.SECONDS)
    val intervalUnit: StateFlow<IntervalUnit> = _intervalUnit.asStateFlow()

    private val _intervalMode = MutableStateFlow(IntervalMode.RANDOM)
    val intervalMode: StateFlow<IntervalMode> = _intervalMode.asStateFlow()

    private val _selectedSound = MutableStateFlow("digital")
    val selectedSound: StateFlow<String> = _selectedSound.asStateFlow()

    /** Milliseconds remaining until next beep; -1 when session is stopped. */
    private val _countdown = MutableStateFlow(-1L)
    val countdown: StateFlow<Long> = _countdown.asStateFlow()

    fun setIntervalValue(v: Int) { _intervalValue.value = v }
    fun setIntervalUnit(u: IntervalUnit) { _intervalUnit.value = u }
    fun setIntervalMode(m: IntervalMode) { _intervalMode.value = m }
    fun setSelectedSound(s: String) { _selectedSound.value = s }

    fun setRunning(running: Boolean) {
        _sessionRunning.value = running
        if (!running) _countdown.value = -1L
    }

    fun setCountdown(ms: Long) { _countdown.value = ms }

    /** Compute interval in milliseconds from current value + unit. */
    fun intervalMs(): Long {
        val v = _intervalValue.value.toLong()
        return if (_intervalUnit.value == IntervalUnit.SECONDS) v * 1000L else v * 60_000L
    }
}
