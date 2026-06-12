package com.papernotes.domain.model

import java.util.Calendar

/**
 * Wiederholungs-Regel einer Erinnerung. [next] berechnet rein kalendarisch den nächsten
 * Auslösezeitpunkt (gleiche Uhrzeit) – ohne DB-Zugriff, damit der BroadcastReceiver die
 * Folge-Erinnerung selbst einplanen kann.
 */
enum class ReminderRule(val label: String) {
    NONE("Einmalig"),
    DAILY("Täglich"),
    WEEKDAYS("Werktags"),
    WEEKLY("Wöchentlich");

    /** Nächster Auslösezeitpunkt nach [from]; [NONE] gibt [from] unverändert zurück. */
    fun next(from: Long): Long {
        if (this == NONE) return from
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (this) {
            DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
            WEEKDAYS -> do {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            } while (
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            )
            NONE -> Unit
        }
        return cal.timeInMillis
    }

    companion object {
        fun fromName(name: String?): ReminderRule =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}
