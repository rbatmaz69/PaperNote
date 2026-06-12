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
import com.papernotes.data.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Empfängt fällige Alarme:
 * - [ACTION_REMIND]: postet eine Erinnerungs-Notification; ist die Erinnerung wiederkehrend,
 *   wird die nächste Fälligkeit gespeichert und neu eingeplant.
 * - [ACTION_CAPSULE]: öffnet eine Zeitkapsel (entsiegelt die Notiz) und meldet das.
 * Ein Tap öffnet die zugehörige Notiz im Editor ([MainActivity] liest [EXTRA_NOTE_ID]).
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: NoteRepository

    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        if (noteId == 0L) return

        when (intent.action) {
            ACTION_REMIND -> {
                val title = intent.getStringExtra(EXTRA_NOTE_TITLE)?.takeIf { it.isNotBlank() }
                    ?: "Notiz"
                notify(context, noteId, title, "Deine Notiz wartet auf dich.")
                rescheduleIfRecurring(noteId)
            }
            ACTION_CAPSULE -> openCapsule(context, noteId)
        }
    }

    /** Wiederkehrende Erinnerung: nächste Fälligkeit persistieren + neuen Alarm setzen. */
    private fun rescheduleIfRecurring(noteId: Long) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val note = repository.getNote(noteId) ?: return@launch
                if (!note.isRecurring) return@launch
                val base = note.reminderAt ?: return@launch
                var next = note.reminderRule.next(base)
                val now = System.currentTimeMillis()
                while (next <= now) next = note.reminderRule.next(next) // verpasste Termine überspringen
                repository.setReminder(noteId, next)
                scheduler.schedule(noteId, note.title, next, note.reminderRule)
            } finally {
                pending.finish()
            }
        }
    }

    /** Zeitkapsel-Termin erreicht: Siegel lösen + benachrichtigen. */
    private fun openCapsule(context: Context, noteId: Long) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val note = repository.getNote(noteId) ?: return@launch
                repository.save(note.copy(sealed = false, capsuleAt = null))
                val title = note.title.takeIf { it.isNotBlank() } ?: "Zeitkapsel"
                notify(context, noteId, title, "Ein Brief hat sich geöffnet.")
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, noteId: Long, title: String, text: String) {
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
            .setContentText(text)
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
        const val ACTION_CAPSULE = "com.papernotes.reminder.ACTION_CAPSULE"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        const val EXTRA_RULE = "note_rule"
        private const val CHANNEL_ID = "note_reminders"
    }
}
