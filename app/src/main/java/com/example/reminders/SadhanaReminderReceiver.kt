package com.example.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.LocalPreferencesStore
import com.example.data.local.SadhanaDatabase
import com.example.domain.SadhanaScoringEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SadhanaReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val followUpStep = intent.getIntExtra(SadhanaReminderScheduler.EXTRA_FOLLOW_UP_STEP, 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = LocalPreferencesStore(context)
                val settings = prefs.getReminderSettingsOnce()
                if (!settings.enabled) return@launch

                // Always re-schedule tomorrow's primary reminder when the primary fires
                if (followUpStep == 0) {
                    SadhanaReminderScheduler.schedulePrimaryReminder(context, settings, forceTomorrow = true)
                }

                val uid = prefs.getActiveUidOnce()
                val todayKey = SadhanaScoringEngine.todayDateKey()
                val alreadyFilledToday = if (uid.isNotBlank()) {
                    SadhanaDatabase.getInstance(context).sadhanaLogDao().hasLogForDate(uid, todayKey)
                } else {
                    false
                }

                if (alreadyFilledToday) {
                    SadhanaReminderScheduler.cancelFollowUpReminder(context)
                    return@launch
                }

                postReminderNotification(context, followUpStep)

                if (settings.followUpEnabled && followUpStep < settings.maxFollowUps) {
                    SadhanaReminderScheduler.scheduleFollowUpReminder(
                        context = context,
                        delayMinutes = settings.followUpIntervalMinutes,
                        nextStep = followUpStep + 1
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postReminderNotification(context: Context, followUpStep: Int) {
        SadhanaReminderScheduler.ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            7200,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (followUpStep == 0) {
            "Hare Krishna! Time for Sadhana Check-In"
        } else {
            "Gentle Follow-Up #$followUpStep: Sadhana Card Pending"
        }
        val body = if (followUpStep == 0) {
            "Offer a calm moment to record today's japa, book reading, hearing, and rest schedule."
        } else {
            "Today's Sadhana Card is still awaiting check-in. Log before Prasadam for full +10 consistency points."
        }

        val notification = NotificationCompat.Builder(context, SadhanaReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(7300 + followUpStep, notification)
    }
}
