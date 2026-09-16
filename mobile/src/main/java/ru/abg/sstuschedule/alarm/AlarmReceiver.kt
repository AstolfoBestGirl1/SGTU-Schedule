package ru.abg.sstuschedule.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.abg.sstuschedule.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "schedule_alarm_channel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_OFFSET = "offset"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: ""
        val offset = intent.getIntExtra(EXTRA_OFFSET, 60)

        val title = context.getString(R.string.alarm_notification_title)
        val text = context.getString(R.string.alarm_notification_text, offset, subject)

        createChannelIfNeeded(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alarm_channel_name)
        }
        manager.createNotificationChannel(channel)
    }
}