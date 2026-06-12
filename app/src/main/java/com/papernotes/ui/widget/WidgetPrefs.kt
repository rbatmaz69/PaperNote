package com.papernotes.ui.widget

import android.content.Context

/** Pro-Widget-Konfiguration (gewählte Notiz + Farbe) in synchronen SharedPreferences. */
object WidgetPrefs {
    private const val FILE = "sticky_note_widget"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setConfig(context: Context, appWidgetId: Int, noteId: Long, palette: String) {
        prefs(context).edit()
            .putLong("note_$appWidgetId", noteId)
            .putString("palette_$appWidgetId", palette)
            .commit()
    }

    fun noteId(context: Context, appWidgetId: Int): Long =
        prefs(context).getLong("note_$appWidgetId", 0L)

    fun palette(context: Context, appWidgetId: Int): String =
        prefs(context).getString("palette_$appWidgetId", PALETTE_MOOD) ?: PALETTE_MOOD

    fun clear(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove("note_$appWidgetId")
            .remove("palette_$appWidgetId")
            .apply()
    }
}
