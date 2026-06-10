package com.papernotes.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kapselt das Planen/Abbrechen exakter Erinnerungs-Alarme über den [AlarmManager].
 * Jede Notiz hat genau einen Alarm (requestCode = Notiz-id), sodass erneutes Setzen
 * den vorherigen Alarm überschreibt. Feuert via [ReminderReceiver].
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(noteId: Long, title: String, triggerAtMillis: Long) {
        if (!canScheduleExact()) return
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(noteId, title, mutableUpdate = true),
        )
    }

    fun cancel(noteId: Long) {
        alarmManager.cancel(pendingIntent(noteId, title = "", mutableUpdate = false))
    }

    /** Räumt eine bereits angezeigte Erinnerungs-Notification weg (Quittieren beim Öffnen). */
    fun dismissNotification(noteId: Long) {
        NotificationManagerCompat.from(context).cancel(noteId.toInt())
    }

    private fun canScheduleExact(): Boolean = alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(noteId: Long, title: String, mutableUpdate: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, title)
        }
        // FLAG_UPDATE_CURRENT überschreibt die Extras eines bestehenden Alarms; zum Canceln
        // genügt ein zur requestCode passender Intent (Extras irrelevant fürs Matching).
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (mutableUpdate) PendingIntent.FLAG_UPDATE_CURRENT else 0
        return PendingIntent.getBroadcast(context, noteId.toInt(), intent, flags)
    }
}
