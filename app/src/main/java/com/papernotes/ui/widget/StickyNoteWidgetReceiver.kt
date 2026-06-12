package com.papernotes.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.papernotes.PaperNotesApp
import kotlinx.coroutines.launch

/** Klassischer AppWidgetProvider für das Haftnotiz-Widget (Rendering via [StickyNoteWidgets]). */
class StickyNoteWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // DB-Zugriff ist suspend → den Broadcast über goAsync am Leben halten.
        val pending = goAsync()
        (context.applicationContext as PaperNotesApp).appScope.launch {
            try {
                appWidgetIds.forEach {
                    runCatching { StickyNoteWidgets.render(context, appWidgetManager, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        StickyNoteWidgets.update(context, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.clear(context, it) }
    }
}
