package com.circuitsense.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

val PlaygroundDarkBackground = Color(0xFF16171D)
val PlaygroundDotColor = Color(0xFF383B47)

/**
 * Draws the clean dot-matrix playground background matching the user's reference image.
 * Soft, evenly-spaced technical dots on a dark charcoal surface.
 */
fun DrawScope.drawPlaygroundDotGrid(
    w: Float = size.width,
    h: Float = size.height,
    step: Float = 28f,
    dotRadius: Float = 1.6f,
    dotColor: Color = PlaygroundDotColor
) {
    var x = step / 2f
    while (x < w) {
        var y = step / 2f
        while (y < h) {
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(x, y)
            )
            y += step
        }
        x += step
    }
}
