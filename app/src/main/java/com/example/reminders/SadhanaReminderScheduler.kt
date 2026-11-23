package com.example.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.R
import com.example.domain.ReminderSettings
import java.util.Calendar

object SadhanaReminderScheduler {

    const val CHANNEL_ID = "sadhana_daily_reminders"
    const val EXTRA_FOLLOW_UP_STEP = "extra_follow_up_step"
    private const val PRIMARY_REQUEST_CODE = 7100
    private const val FOLLOWUP_REQUEST_CODE = 7101

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.reminder_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun schedulePrimaryReminder(context: Context, settings: ReminderSettings, forceTomorrow: Boolean = false) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!settings.enabled) {
            cancelAllReminders(context)
            return
        }

        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.primaryHour)
            set(Calendar.MINUTE, settings.primaryMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (forceTomorrow || timeInMillis <= System.currentTimeMillis() + 5_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, SadhanaReminderReceiver::class.java).apply {
            putExtra(EXTRA_FOLLOW_UP_STEP, 0)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PRIMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerCal.timeInMillis,
            pendingIntent
        )
    }

    fun scheduleFollowUpReminder(context: Context, delayMinutes: Int, nextStep: Int) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + delayMinutes.coerceAtLeast(15) * 60_000L

        val intent = Intent(context, SadhanaReminderReceiver::class.java).apply {
            putExtra(EXTRA_FOLLOW_UP_STEP, nextStep)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            FOLLOWUP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    fun cancelFollowUpReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SadhanaReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            FOLLOWUP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(PRIMARY_REQUEST_CODE, FOLLOWUP_REQUEST_CODE).forEach { code ->
            val intent = Intent(context, SadhanaReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
