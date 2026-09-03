package com.bengalbytes.zenvo.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bengalbytes.zenvo.MainActivity
import com.bengalbytes.zenvo.R
import com.bengalbytes.zenvo.data.TimerRepository
import com.bengalbytes.zenvo.services.ZenvoFocusTimerService
import com.bengalbytes.zenvo.widget.ZenvoWidget
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun timerRepository(): TimerRepository
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        ).timerRepository()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Clear active timer notification
        notificationManager.cancel(ZenvoFocusTimerService.NOTIFICATION_ID)

        CoroutineScope(Dispatchers.IO).launch {
            val state = repository.timerStateSnapshot()
            val focusedDurationMillis = state.durationMillis
            val minutesFocused = focusedDurationMillis / 60_000L

            repository.stopTimer()
            
            if (minutesFocused > 0) {
                repository.addDailyFocusMinutes(minutesFocused)
                val stats = repository.getSessionStats()
                repository.saveSessionStats(stats.first + 1, stats.second + minutesFocused)
            }

            ZenvoWidget.pushStateAndUpdate(context)
        }

        val channelId = "zenvo_timer_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Focus Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your focus session completes"
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap notification => open app
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTapIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎯 Focus Session Complete. Alhamdulilah.")
            .setContentText("You finished a session with discipline. Tap to continue.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Excellence in work is a form of worship. Take a break to refresh your heart and mind. Great work! 🧠✨")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 300, 200, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingTapIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
