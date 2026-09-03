package com.bengalbytes.zenvo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bengalbytes.zenvo.core.AlarmController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "timer_prefs")

data class TimerState(
    val startTimeMillis: Long,
    val durationMillis: Long,
    val defaultDurationMillis: Long = 15 * 60 * 1000L
)

/** Widget customization persisted in DataStore */
data class WidgetSettings(
    val customDurationMin: Int = 15,
    val showHadith: Boolean = true,
    val gradientPreset: String = "ocean"
)

/** User preferences */
data class FocusPreferences(
    val dailyGoalMinutes: Int = 120,
    val currentHadithIndex: Int = 0,
    val lastHadithUpdateTime: Long = 0L
)

@Singleton
class TimerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmController: AlarmController
) {
    // ── Timer Keys ──
    private val START_TIME_KEY = longPreferencesKey("start_time_millis")
    private val DURATION_KEY = longPreferencesKey("duration_millis")
    private val SESSIONS_KEY = intPreferencesKey("sessions_completed")
    private val TOTAL_FOCUS_KEY = longPreferencesKey("total_focus_minutes")
    private val DAILY_FOCUS_KEY = longPreferencesKey("daily_focus_minutes")
    private val LAST_RESET_DATE_KEY = stringPreferencesKey("last_reset_date")
    private val STREAK_KEY = intPreferencesKey("focus_streak_days")

    // ── Widget Settings Keys ──
    private val WIDGET_CUSTOM_DUR_KEY = intPreferencesKey("widget_custom_dur_min")
    private val WIDGET_SHOW_HADITH_KEY = stringPreferencesKey("widget_show_hadith")
    private val WIDGET_GRADIENT_KEY = stringPreferencesKey("widget_gradient_preset")

    // ── Focus Preferences Keys ──
    private val DAILY_GOAL_KEY = intPreferencesKey("daily_goal_minutes")
    private val HADITH_INDEX_KEY = intPreferencesKey("hadith_index")
    private val HADITH_UPDATE_TIME_KEY = longPreferencesKey("hadith_update_time")

    val timerState: Flow<TimerState> = context.dataStore.data.map { preferences ->
        TimerState(
            startTimeMillis = preferences[START_TIME_KEY] ?: 0L,
            durationMillis = preferences[DURATION_KEY] ?: 0L
        )
    }

    val widgetSettings: Flow<WidgetSettings> = context.dataStore.data.map { preferences ->
        WidgetSettings(
            customDurationMin = preferences[WIDGET_CUSTOM_DUR_KEY] ?: 15,
            showHadith = (preferences[WIDGET_SHOW_HADITH_KEY] ?: "true") == "true",
            gradientPreset = preferences[WIDGET_GRADIENT_KEY] ?: "ocean"
        )
    }

    val focusPreferences: Flow<FocusPreferences> = context.dataStore.data.map { preferences ->
        FocusPreferences(
            dailyGoalMinutes = preferences[DAILY_GOAL_KEY] ?: 120,
            currentHadithIndex = preferences[HADITH_INDEX_KEY] ?: 0,
            lastHadithUpdateTime = preferences[HADITH_UPDATE_TIME_KEY] ?: 0L
        )
    }

    suspend fun startTimer(durationMillis: Long) {
        val now = System.currentTimeMillis()
        context.dataStore.edit { preferences ->
            preferences[START_TIME_KEY] = now
            preferences[DURATION_KEY] = durationMillis
        }
        alarmController.scheduleAlarm(now + durationMillis)
    }

    suspend fun stopTimer() {
        context.dataStore.edit { preferences ->
            preferences[START_TIME_KEY] = 0L
            preferences[DURATION_KEY] = 0L
        }
        alarmController.cancelAlarm()
    }

    val sessionStatsFlow: Flow<Triple<Int, Long, Int>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[SESSIONS_KEY] ?: 0,
            prefs[TOTAL_FOCUS_KEY] ?: 0L,
            prefs[STREAK_KEY] ?: 0
        )
    }

    val dailyFocusMinutesFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        val today = java.time.LocalDate.now().toString()
        val lastReset = prefs[LAST_RESET_DATE_KEY] ?: ""
        if (lastReset == today) prefs[DAILY_FOCUS_KEY] ?: 0L else 0L
    }

    suspend fun getDailyFocusMinutes(): Long {
        val prefs = context.dataStore.data.first()
        val today = java.time.LocalDate.now().toString()
        val lastReset = prefs[LAST_RESET_DATE_KEY] ?: ""
        return if (lastReset == today) {
            prefs[DAILY_FOCUS_KEY] ?: 0L
        } else {
            0L
        }
    }

    suspend fun addDailyFocusMinutes(minutes: Long) {
        context.dataStore.edit { prefs ->
            val today = java.time.LocalDate.now().toString()
            val lastReset = prefs[LAST_RESET_DATE_KEY] ?: ""
            if (lastReset != today) {
                if (lastReset == java.time.LocalDate.now().minusDays(1).toString()) {
                    prefs[STREAK_KEY] = (prefs[STREAK_KEY] ?: 0) + 1
                } else if (lastReset.isNotEmpty()) {
                    prefs[STREAK_KEY] = 1
                }
                prefs[DAILY_FOCUS_KEY] = minutes
                prefs[LAST_RESET_DATE_KEY] = today
            } else {
                prefs[DAILY_FOCUS_KEY] = (prefs[DAILY_FOCUS_KEY] ?: 0L) + minutes
            }
        }
    }

    suspend fun saveSessionStats(sessions: Int, totalFocusMinutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[SESSIONS_KEY] = sessions
            prefs[TOTAL_FOCUS_KEY] = totalFocusMinutes
        }
    }

    suspend fun saveWidgetSettings(settings: WidgetSettings) {
        context.dataStore.edit { prefs ->
            prefs[WIDGET_CUSTOM_DUR_KEY] = settings.customDurationMin
            prefs[WIDGET_SHOW_HADITH_KEY] = if (settings.showHadith) "true" else "false"
            prefs[WIDGET_GRADIENT_KEY] = settings.gradientPreset
        }
    }

    suspend fun saveFocusPreferences(prefs: FocusPreferences) {
        context.dataStore.edit { p ->
            p[DAILY_GOAL_KEY] = prefs.dailyGoalMinutes
            p[HADITH_INDEX_KEY] = prefs.currentHadithIndex
            p[HADITH_UPDATE_TIME_KEY] = prefs.lastHadithUpdateTime
        }
    }

    suspend fun updateHadithIndex(index: Int) {
        context.dataStore.edit { p ->
            p[HADITH_INDEX_KEY] = index
            p[HADITH_UPDATE_TIME_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun getEffectiveWidgetDuration(): Long {
        val settings = widgetSettingsSnapshot()
        return settings.customDurationMin * 60 * 1000L
    }

    suspend fun getSessionStats(): Pair<Int, Long> {
        val prefs = context.dataStore.data.first()
        return Pair(prefs[SESSIONS_KEY] ?: 0, prefs[TOTAL_FOCUS_KEY] ?: 0L)
    }

    suspend fun timerStateSnapshot(): TimerState = timerState.first()
    suspend fun widgetSettingsSnapshot(): WidgetSettings = widgetSettings.first()
    suspend fun focusPreferencesSnapshot(): FocusPreferences = focusPreferences.first()
}
