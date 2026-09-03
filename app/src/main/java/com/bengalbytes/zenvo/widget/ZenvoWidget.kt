package com.bengalbytes.zenvo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import com.bengalbytes.zenvo.R
import com.bengalbytes.zenvo.core.TimerEngine
import com.bengalbytes.zenvo.data.IslamicContent
import com.bengalbytes.zenvo.ui.theme.ErrorRed
import com.bengalbytes.zenvo.ui.theme.PrimaryAccent
import com.bengalbytes.zenvo.ui.theme.SuccessGreen
import com.bengalbytes.zenvo.ui.theme.TextPrimary
import com.bengalbytes.zenvo.ui.theme.TextSecondary
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

object WidgetStateKeys {
    const val START_TIME = "w_start_time"
    const val DURATION = "w_duration"
    const val IDLE_DURATION = "w_idle_duration"
    const val HADITH_TEXT = "w_hadith_text"
    const val HADITH_REF = "w_hadith_ref"
    const val GRADIENT = "w_gradient"
}

class ZenvoWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    companion object {
        suspend fun pushStateAndUpdate(context: Context) {
            val repository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            ).timerRepository()

            val timerState = repository.timerStateSnapshot()
            val widgetSettings = repository.widgetSettingsSnapshot()
            val prefs = repository.focusPreferencesSnapshot()
            val hadith = IslamicContent.HADITH_LIST[prefs.currentHadithIndex % IslamicContent.HADITH_LIST.size]

            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(ZenvoWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[longKey(WidgetStateKeys.START_TIME)] = timerState.startTimeMillis
                        this[longKey(WidgetStateKeys.DURATION)] = timerState.durationMillis
                        this[longKey(WidgetStateKeys.IDLE_DURATION)] = widgetSettings.customDurationMin * 60 * 1000L
                        this[stringKey(WidgetStateKeys.HADITH_TEXT)] = hadith.text
                        this[stringKey(WidgetStateKeys.HADITH_REF)] = hadith.reference
                        this[stringKey(WidgetStateKeys.GRADIENT)] = widgetSettings.gradientPreset
                    }
                }
                ZenvoWidget().update(context, glanceId)
            }
        }

        suspend fun updateAllInstances(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(ZenvoWidget::class.java)
            glanceIds.forEach { glanceId ->
                ZenvoWidget().update(context, glanceId)
            }
        }

        private fun longKey(name: String) = androidx.datastore.preferences.core.longPreferencesKey(name)
        private fun stringKey(name: String) = androidx.datastore.preferences.core.stringPreferencesKey(name)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun timerRepository(): com.bengalbytes.zenvo.data.TimerRepository
}

@Composable
private fun WidgetContent() {
    val prefs = currentState<Preferences>()
    val timerEngine = TimerEngine()

    val startTime = prefs[longKey(WidgetStateKeys.START_TIME)] ?: 0L
    val duration = prefs[longKey(WidgetStateKeys.DURATION)] ?: 0L
    val idleDuration = prefs[longKey(WidgetStateKeys.IDLE_DURATION)] ?: (15 * 60 * 1000L)
    val hadithText = prefs[stringKey(WidgetStateKeys.HADITH_TEXT)] ?: "Allah loves consistency."
    val hadithRef = prefs[stringKey(WidgetStateKeys.HADITH_REF)] ?: "Bukhari"
    val gradient = prefs[stringKey(WidgetStateKeys.GRADIENT)] ?: "ocean"

    val remaining = if (startTime > 0 && duration > 0) {
        timerEngine.calculateRemainingMillis(startTime, duration, System.currentTimeMillis())
    } else 0L

    val isRunning = remaining > 0
    val size = LocalSize.current
    val timeFontSize = if (size.width > 200.dp) 42.sp else 32.sp

    val bgDrawable = when (gradient) {
        "aurora" -> R.drawable.widget_bg_aurora
        else -> R.drawable.widget_bg_ocean
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ImageProvider(bgDrawable)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header / Refresh Hadith
            Row(
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshHadithAction>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRunning) "● " else "○ ",
                    style = TextStyle(color = ColorProvider(if (isRunning) SuccessGreen else TextSecondary), fontSize = 10.sp)
                )
                Text(
                    text = "ZENVO FOCUS",
                    style = TextStyle(color = ColorProvider(PrimaryAccent), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Timer
            Text(
                text = if (isRunning) timerEngine.formatTime(remaining) else timerEngine.formatTime(idleDuration),
                style = TextStyle(color = ColorProvider(TextPrimary), fontSize = timeFontSize, fontWeight = FontWeight.Bold)
            )

            if (size.height > 110.dp) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                // Hadith
                Text(
                    text = hadithText,
                    style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 11.sp, textAlign = TextAlign.Center),
                    maxLines = 2
                )
                Text(
                    text = "— $hadithRef",
                    style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 9.sp, textAlign = TextAlign.Center)
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Action Button
            Box(
                modifier = GlanceModifier
                    .cornerRadius(16.dp)
                    .background(ColorProvider(if (isRunning) ErrorRed else PrimaryAccent))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clickable(actionRunCallback(if (isRunning) StopTimerAction::class.java else StartTimerAction::class.java)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "STOP" else "START",
                    style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

class RefreshHadithAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java).timerRepository()
        val prefs = repository.focusPreferencesSnapshot()
        val nextIndex = (prefs.currentHadithIndex + 1) % IslamicContent.HADITH_LIST.size
        repository.updateHadithIndex(nextIndex)
        ZenvoWidget.pushStateAndUpdate(context)
    }
}

private fun longKey(name: String) = androidx.datastore.preferences.core.longPreferencesKey(name)
private fun stringKey(name: String) = androidx.datastore.preferences.core.stringPreferencesKey(name)
