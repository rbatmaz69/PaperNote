package com.papernotes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.papernotes.data.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Exakte Alarme überleben kein Reboot – nach dem Hochfahren werden alle noch in der
 * Zukunft liegenden Erinnerungen neu eingeplant.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: NoteRepository

    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val now = System.currentTimeMillis()
                repository.notesWithReminders().forEach { note ->
                    val at = note.reminderAt ?: return@forEach
                    if (at > now) scheduler.schedule(note.id, note.title, at)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
