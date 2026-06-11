package com.papernotes.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Zeichnet ein Baseline-Profil der typischen „Journey" auf: App starten → Notiz-Grid
 * scrollen → (best effort) eine Notiz öffnen und schließen. Die dabei berührten
 * Compose-Pfade werden beim Installieren vor-AOT-kompiliert, sodass Kaltstart und die
 * ersten Animationen flüssig laufen statt in den ersten Frames per JIT zu interpretieren.
 *
 * Erzeugen (Gerät/Emulator verbunden):  gradle :app:generateBaselineProfile
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.papernotes",
        // Zusätzlich ein Startup-Profil erzeugen: kompiliert gezielt den Kaltstart-Pfad
        // vor (gegen das sichtbare „Aufbauen"/Icons-Laden beim ersten Frame).
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        // Warten, bis das Grid steht.
        device.waitForIdle()

        // Grid scrollen (deckt NotesScreen-/NoteCard-Komposition + Scroll-Pfade ab).
        val scrollable = device.findObject(By.scrollable(true))
        if (scrollable != null) {
            scrollable.setGestureMargin(device.displayWidth / 5)
            repeat(2) { scrollable.fling(Direction.DOWN) }
            scrollable.fling(Direction.UP)
            device.waitForIdle()
        }

        // Best effort: erste Notiz öffnen (Morph in den Editor) und wieder zurück.
        runCatching {
            val card = device.findObject(By.scrollable(true))?.children?.firstOrNull()
            if (card != null) {
                card.click()
                device.wait(Until.hasObject(By.pkg("com.papernotes").depth(0)), 2_000)
                device.waitForIdle()
                device.pressBack()
                device.waitForIdle()
            }
        }
    }
}
