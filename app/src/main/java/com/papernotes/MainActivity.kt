package com.papernotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.papernotes.reminder.ReminderReceiver
import com.papernotes.ui.navigation.PaperNotesNavGraph
import com.papernotes.ui.navigation.QuickCapture
import com.papernotes.ui.notes.NotesViewModel
import com.papernotes.ui.theme.PaperNotesTheme
import com.papernotes.ui.theme.ThemeViewModel
import com.papernotes.ui.widget.StickyNoteWidgets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val notesViewModel: NotesViewModel by viewModels()

    // Notiz-Öffnen-Anfragen (Widget-/Notification-Tap). CONFLATED: nur die letzte zählt, und sie
    // überlebt, bis die UI sie einsammelt – auch wenn die App schon läuft (onNewIntent).
    private val openNoteRequests = Channel<Long>(Channel.CONFLATED)

    /** Liest eine Notiz-id aus einem Tap-Intent (Widget/Notification) und reicht sie an die UI. */
    private fun handleNoteIntent(intent: Intent?) {
        intent?.getLongExtra(ReminderReceiver.EXTRA_NOTE_ID, 0L)
            ?.takeIf { it != 0L }
            ?.let { openNoteRequests.trySend(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNoteIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash halten, bis Theme (DataStore) UND Notizen (Room) geladen sind – kein Aufblitzen
        // eines Standard-Themes oder leeren Rasters.
        installSplashScreen().setKeepOnScreenCondition {
            !(themeViewModel.ready.value && notesViewModel.uiState.value.loaded)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Per Erinnerungs-Notification oder Widget-Tap geöffnet? → diese Notiz anzeigen.
        handleNoteIntent(intent)

        // „Einkleben": geteilter Text/Link aus einer anderen App → neuer Zettel.
        // Launcher-Shortcut „Neuer Zettel" → leerer Zettel.
        val quickCapture = when {
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val body = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                QuickCapture(title = subject, body = body)
            }
            intent?.action == ACTION_NEW_NOTE -> QuickCapture(title = "", body = "")
            else -> null
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val theme by themeViewModel.theme.collectAsStateWithLifecycle()
            // Erst animieren, wenn das gespeicherte Theme geladen ist – sonst blenden beim
            // Start die Farben vom AUTO-Default ins gespeicherte Theme über (sichtbar, weil
            // der Splash genau dann verschwindet).
            val themeReady by themeViewModel.ready.collectAsStateWithLifecycle()
            val isDark = theme.resolve(isSystemInDarkTheme()).dark

            // Statusleisten-Icons passend zum Papier-Theme (helle Icons auf dunklem Papier).
            val view = LocalView.current
            LaunchedEffect(isDark) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }

            PaperNotesTheme(theme = theme, animateThemeChange = themeReady) {
                PaperNotesNavGraph(
                    openNote = openNoteRequests.receiveAsFlow(),
                    quickCapture = quickCapture,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Nach (möglichen) Bearbeitungen die Haftnotiz-Widgets auffrischen.
        StickyNoteWidgets.updateAll(this)
    }

    companion object {
        /** Action des Launcher-Shortcuts „Neuer Zettel" (siehe res/xml/shortcuts.xml). */
        const val ACTION_NEW_NOTE = "com.papernotes.NEW_NOTE"
    }
}
