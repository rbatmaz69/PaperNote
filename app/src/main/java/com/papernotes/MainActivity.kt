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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val notesViewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash halten, bis Theme (DataStore) UND Notizen (Room) geladen sind – kein Aufblitzen
        // eines Standard-Themes oder leeren Rasters.
        installSplashScreen().setKeepOnScreenCondition {
            !(themeViewModel.ready.value && notesViewModel.uiState.value.loaded)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Per Erinnerungs-Notification geöffnet? Dann direkt diese Notiz anzeigen.
        val initialNoteId = intent
            ?.getLongExtra(ReminderReceiver.EXTRA_NOTE_ID, 0L)
            ?.takeIf { it != 0L }

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
                PaperNotesNavGraph(initialNoteId = initialNoteId, quickCapture = quickCapture)
            }
        }
    }

    companion object {
        /** Action des Launcher-Shortcuts „Neuer Zettel" (siehe res/xml/shortcuts.xml). */
        const val ACTION_NEW_NOTE = "com.papernotes.NEW_NOTE"
    }
}
