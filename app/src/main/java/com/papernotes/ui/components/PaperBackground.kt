package com.papernotes.ui.components

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    baseColor: Color = MaterialTheme.colorScheme.background,
    washColor: Color = baseColor.washTint(),
    strength: Float = 1f,
    dotGrid: Boolean = true,
    dotColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.035f),
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
                val brush = ShaderBrush(shader)

                // Punkt-Positionen einmalig (pro Größe) berechnen, statt in jedem Frame –
                // entlastet das Zeichnen u.a. während der Theme-Überblendung.
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

                onDrawBehind {
                    drawRect(brush = brush)
                    dots.forEach { drawCircle(color = dotColor, radius = dotRadiusPx, center = it) }
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
