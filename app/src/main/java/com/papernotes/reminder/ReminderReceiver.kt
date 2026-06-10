package com.papernotes.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.papernotes.MainActivity
import com.papernotes.R

/**
 * Empfängt den fälligen Erinnerungs-Alarm und postet eine Notification. Ein Tap öffnet
 * die zugehörige Notiz direkt im Editor ([MainActivity] liest [EXTRA_NOTE_ID]).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        if (noteId == 0L) return
        val title = intent.getStringExtra(EXTRA_NOTE_TITLE)?.takeIf { it.isNotBlank() }
            ?: "Notiz"

        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(title)
            .setContentText("Deine Notiz wartet auf dich.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        // notify() ist ohne POST_NOTIFICATIONS-Recht ein No-op – kein Crash.
        NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Erinnerungen",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Erinnert dich an deine Notizen." },
            )
        }
    }

    companion object {
        const val ACTION_REMIND = "com.papernotes.reminder.ACTION_REMIND"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        private const val CHANNEL_ID = "note_reminders"
    }
}
