package com.bengalbytes.zenvo.widget

import android.content.Context
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.bengalbytes.zenvo.services.ZenvoFocusTimerService
import dagger.hilt.android.EntryPointAccessors

/**
 * Called when the user taps ▶ START on the widget.
 *
 * Reads the effective duration from the user's widget settings (pomodoro / break / custom)
 * then hands off to ZenvoFocusTimerService as a foreground service — the same path as
 * pressing START in the app, so both are perfectly in sync.
 */
class StartTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).timerRepository()

        val duration = repository.getEffectiveWidgetDuration()

        val intent = ZenvoFocusTimerService.startTimerIntent(context, duration)
        context.startService(intent)
    }
}

/**
 * Called when the user taps ⏹ STOP on the widget.
 *
 * Sends the stop action to the already-running foreground service.
 * startService is correct here — the service is already in foreground,
 * we just deliver a new intent with ACTION_STOP.
 */
class StopTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = ZenvoFocusTimerService.stopTimerIntent(context)
        context.startService(intent)
    }
}
