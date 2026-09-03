package com.bengalbytes.zenvo

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bengalbytes.zenvo.core.TimerEngine
import com.bengalbytes.zenvo.data.FocusPreferences
import com.bengalbytes.zenvo.data.TimerRepository
import com.bengalbytes.zenvo.data.WidgetSettings
import com.bengalbytes.zenvo.data.IslamicContent
import com.bengalbytes.zenvo.data.Hadith
import com.bengalbytes.zenvo.services.ZenvoFocusTimerService
import com.bengalbytes.zenvo.widget.ZenvoWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerUiState(
    val remainingTime: String = "15:00",
    val progress: Float = 1f,
    val isRunning: Boolean = false,
    val sessionsCompleted: Int = 0,
    val statusText: String = "Ready to Focus",
    val dailyFocusMinutes: Long = 0,
    val streakDays: Int = 0,
    val defaultDuration: Long = 15 * 60 * 1000L,
    val customDuration: Long = 15 * 60 * 1000L,
    // Widget settings
    val widgetSettings: WidgetSettings = WidgetSettings(),
    // Focus preferences
    val focusPreferences: FocusPreferences = FocusPreferences(),
    // Islamic motivation
    val currentHadith: Hadith = IslamicContent.HADITH_LIST[0],
    val currentHadithIndex: Int = 0
) {
    val dailyGoalProgress: Float
        get() {
            val goal = focusPreferences.dailyGoalMinutes
            return if (goal > 0) (dailyFocusMinutes.toFloat() / goal).coerceIn(0f, 1f) else 0f
        }
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: TimerRepository,
    private val application: Application
) : ViewModel() {

    private val timerEngine = TimerEngine()
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadInitialData()
        observeTimerState()
        observeWidgetSettings()
        observeFocusPreferences()
        rotateHadithByTime()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.sessionStatsFlow.collect { stats ->
                _uiState.update {
                    it.copy(
                        sessionsCompleted = stats.first,
                        streakDays = stats.third
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.dailyFocusMinutesFlow.collect { dailyMinutes ->
                _uiState.update { it.copy(dailyFocusMinutes = dailyMinutes) }
            }
        }
    }

    private fun observeTimerState() {
        viewModelScope.launch {
            repository.timerState.collect { state ->
                if (state.startTimeMillis > 0) {
                    startUpdatingUi(state.startTimeMillis, state.durationMillis)
                } else {
                    stopUpdatingUi()
                }
            }
        }
    }

    private fun observeWidgetSettings() {
        viewModelScope.launch {
            repository.widgetSettings.collect { settings ->
                _uiState.update { it.copy(
                    widgetSettings = settings,
                    customDuration = settings.customDurationMin * 60 * 1000L
                ) }
            }
        }
    }

    private fun observeFocusPreferences() {
        viewModelScope.launch {
            repository.focusPreferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        focusPreferences = prefs,
                        currentHadithIndex = prefs.currentHadithIndex,
                        currentHadith = IslamicContent.HADITH_LIST[prefs.currentHadithIndex % IslamicContent.HADITH_LIST.size]
                    )
                }
            }
        }
    }

    private fun rotateHadithByTime() {
        val index = IslamicContent.getHadithIndexByTime()
        viewModelScope.launch {
            repository.updateHadithIndex(index)
        }
    }

    fun refreshHadith() {
        val nextIndex = (uiState.value.currentHadithIndex + 1) % IslamicContent.HADITH_LIST.size
        viewModelScope.launch {
            repository.updateHadithIndex(nextIndex)
            // Immediately update widget
            ZenvoWidget.pushStateAndUpdate(application)
        }
    }

    fun toggleTimer(isCustom: Boolean = false) {
        if (_uiState.value.isRunning) {
            stopTimer()
        } else {
            val duration = if (isCustom) _uiState.value.customDuration else _uiState.value.defaultDuration
            startTimer(duration)
        }
    }

    private fun startTimer(durationMillis: Long) {
        val intent = ZenvoFocusTimerService.startTimerIntent(application, durationMillis)
        application.startService(intent)
    }

    private fun stopTimer() {
        val intent = ZenvoFocusTimerService.stopTimerIntent(application)
        application.startService(intent)
    }

    private fun startUpdatingUi(startTime: Long, duration: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = timerEngine.calculateRemainingMillis(startTime, duration, System.currentTimeMillis())
                val progressVal = if (duration > 0) remaining.toFloat() / duration.toFloat() else 0f
                val minutes = remaining / 60000

                _uiState.update {
                    it.copy(
                        remainingTime = timerEngine.formatTime(remaining),
                        progress = progressVal.coerceIn(0f, 1f),
                        isRunning = remaining > 0,
                        statusText = if (remaining > 0) {
                            when {
                                minutes > 10 -> "Deep focus for Allah ☝️"
                                minutes > 5 -> "Stay consistent ✨"
                                else -> "Finishing with Itqan 🎯"
                            }
                        } else "Session Complete! Alhamdulillah"
                    )
                }

                if (remaining <= 0) break
                delay(100)
            }
        }
    }

    private fun stopUpdatingUi() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                remainingTime = timerEngine.formatTime(it.customDuration),
                progress = 1f,
                isRunning = false,
                statusText = "Ready to Focus"
            )
        }
    }

    fun updateWidgetDuration(min: Int) {
        viewModelScope.launch {
            val updated = _uiState.value.widgetSettings.copy(customDurationMin = min)
            repository.saveWidgetSettings(updated)
            ZenvoWidget.pushStateAndUpdate(application)
        }
    }

    fun updateDailyGoal(min: Int) {
        viewModelScope.launch {
            val updated = _uiState.value.focusPreferences.copy(dailyGoalMinutes = min)
            repository.saveFocusPreferences(updated)
        }
    }
}
