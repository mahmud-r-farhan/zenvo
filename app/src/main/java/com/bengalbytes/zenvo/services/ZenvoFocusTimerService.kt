package com.bengalbytes.zenvo.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bengalbytes.zenvo.MainActivity
import com.bengalbytes.zenvo.R
import com.bengalbytes.zenvo.core.TimerEngine
import com.bengalbytes.zenvo.data.TimerRepository
import com.bengalbytes.zenvo.widget.ZenvoWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ZenvoFocusTimerService : Service() {

    @Inject lateinit var repository: TimerRepository
    private val timerEngine = TimerEngine()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "zenvo_focus_timer"
        const val NOTIFICATION_ID = 3001
        const val ACTION_START = "com.bengalbytes.zenvo.ACTION_START_TIMER"
        const val ACTION_STOP = "com.bengalbytes.zenvo.ACTION_STOP_TIMER"
        const val EXTRA_DURATION = "extra_duration"

        fun startTimerIntent(context: Context, durationMillis: Long): Intent {
            return Intent(context, ZenvoFocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION, durationMillis)
            }
        }

        fun stopTimerIntent(context: Context): Intent {
            return Intent(context, ZenvoFocusTimerService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, 15 * 60 * 1000L)
                startFocusTimer(duration)
            }
            ACTION_STOP -> stopFocusTimer()
        }
        return START_NOT_STICKY
    }

    private fun startFocusTimer(durationMillis: Long) {
        // We no longer call startForeground() to avoid the restricted FGS permission.
        // Instead, we show an ongoing notification that uses the system chronometer.
        val endTime = System.currentTimeMillis() + durationMillis
        
        showNotification(endTime)

        serviceScope.launch {
            repository.startTimer(durationMillis)
            ZenvoWidget.pushStateAndUpdate(this@ZenvoFocusTimerService)
        }
    }

    private fun stopFocusTimer() {
        serviceScope.launch {
            repository.stopTimer()
            ZenvoWidget.pushStateAndUpdate(this@ZenvoFocusTimerService)
            
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Focus Timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows your active focus countdown"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showNotification(endTime: Long) {
        val tapIntent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingTapIntent = PendingIntent.getActivity(this, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(this, 1, stopTimerIntent(this), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Focusing…")
            .setContentText("Your session is active")
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(true)
            .setWhen(endTime)
            .setUsesChronometer(true)
            .setChronometerCountDown(true) 
            .setContentIntent(pendingTapIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
