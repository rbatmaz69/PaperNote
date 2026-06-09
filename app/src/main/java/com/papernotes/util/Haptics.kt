package com.papernotes.util

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Bündelt das haptische "Papier-Gefühl". Feine Ticks laufen über
 * [View.performHapticFeedback] (respektiert System-Einstellungen), während das
 * Knüll-Feedback ein eigenes Vibrations-Pattern nutzt.
 *
 * minSdk 33 → [VibratorManager] ist verfügbar.
 */
class PaperHaptics(
    private val view: View,
    context: Context,
) {
    private val vibrator =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator

    /** Leichtes Tippen – z.B. Karte antippen, Button drücken. */
    fun tap() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    /** Sanfter Tick – z.B. Eselsohr knicken, Schwelle erreicht. */
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    /** Bestätigung – z.B. Notiz gespeichert / Teebeutel gezogen. */
    fun confirm() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

    /** Knüll-Geste: kurzes, unregelmäßiges Knistern, dann ein dumpfer "Plopp". */
    fun crumple() {
        val v = vibrator ?: return
        val timings = longArrayOf(0, 12, 8, 10, 8, 14, 30, 40)
        val amplitudes = intArrayOf(0, 60, 0, 90, 0, 120, 0, 180)
        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}

@Composable
fun rememberPaperHaptics(): PaperHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view) { PaperHaptics(view, context) }
}
