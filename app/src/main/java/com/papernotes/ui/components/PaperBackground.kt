package com.papernotes.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.papernotes.ui.theme.LocalPaperTheme
import com.papernotes.util.PAPER_SHADER_SRC

/**
 * Vollflächiger "Aquarell-Trickfilm-Papier"-Hintergrund: AGSL-Farbwäsche mit sanfter
 * Cel-Posterisierung + dezentes Dot-Grid-Raster. [content] wird darüber gelegt.
 *
 * minSdk ist 33, daher ist [RuntimeShader] garantiert verfügbar.
 */
@Composable
fun PaperBackground(
    modifier: Modifier = Modifier,
    // Bewusst aus dem *gesetzten* (nicht animierten) Theme statt aus den animierten
    // MaterialTheme-Farben: so ändert sich baseColor/dotColor nur bei echtem Theme-Wechsel
    // und das teure Shader-Bitmap wird nur einmal pro Theme gebacken, nicht pro Frame des
    // 450ms-Farb-Crossfades. Die Farbwerte sind identisch (background=paper, onBackground=ink).
    baseColor: Color = LocalPaperTheme.current.paper,
    washColor: Color = baseColor.washTint(),
    strength: Float = 1f,
    dotGrid: Boolean = true,
    dotColor: Color = LocalPaperTheme.current.ink.copy(alpha = 0.035f),
    content: @Composable () -> Unit,
) {
    val shader = remember { RuntimeShader(PAPER_SHADER_SRC) }
    val density = LocalDensity.current
    val dotSpacingPx = with(density) { 28.dp.toPx() }
    val dotRadiusPx = with(density) { 1.2.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iBase", baseColor.red, baseColor.green, baseColor.blue)
                shader.setFloatUniform("iWash", washColor.red, washColor.green, washColor.blue)
                shader.setFloatUniform("iStrength", strength)

                // Punkt-Positionen einmalig (pro Größe) berechnen.
                val areaW = size.width
                val areaH = size.height
                val dots = if (dotGrid) buildList {
                    var y = dotSpacingPx
                    while (y < areaH) {
                        var x = dotSpacingPx
                        while (x < areaW) {
                            add(Offset(x, y))
                            x += dotSpacingPx
                        }
                        y += dotSpacingPx
                    }
                } else emptyList()

                // Der fbm-Shader ist teuer (4 Oktaven Value-Noise pro Pixel) und sein Ergebnis
                // ist zeit-unabhängig, also statisch. Würde er in onDrawBehind gezeichnet, liefe
                // er bei JEDEM Frame über alle Pixel (Scrollen, Editor-Morph, Theme-Crossfade
                // re-komponieren den ganzen Bildschirm) → durchgängige Last.
                // Stattdessen einmal pro Größe/Farbe via Picture in ein Hardware-Bitmap backen
                // (RuntimeShader braucht HW-Beschleunigung) und pro Frame nur noch die Textur
                // blitten. Optisch identisch, aber praktisch kostenlos pro Frame.
                val bw = areaW.toInt().coerceAtLeast(1)
                val bh = areaH.toInt().coerceAtLeast(1)
                val picture = Picture()
                val rec = picture.beginRecording(bw, bh)
                val bgPaint = Paint()
                bgPaint.shader = shader
                rec.drawRect(0f, 0f, areaW, areaH, bgPaint)
                val dotPaint = Paint()
                dotPaint.isAntiAlias = true
                dotPaint.color = dotColor.toArgb()
                dots.forEach { rec.drawCircle(it.x, it.y, dotRadiusPx, dotPaint) }
                picture.endRecording()
                val baked = Bitmap.createBitmap(picture).asImageBitmap()

                onDrawBehind {
                    drawImage(baked)
                }
            },
    ) {
        content()
    }
}

/** Etwas dunklerer, wärmerer Ton derselben Farbe – Standard-Wäsche für die Aquarell-Optik. */
private fun Color.washTint(): Color = Color(
    red = (red * 0.86f),
    green = (green * 0.84f),
    blue = (blue * 0.80f),
    alpha = 1f,
)
