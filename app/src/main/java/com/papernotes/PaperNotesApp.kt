package com.papernotes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class PaperNotesApp : Application() {
    /** Prozess-weiter Scope für Arbeit, die das Beenden einzelner Activities überleben muss. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
