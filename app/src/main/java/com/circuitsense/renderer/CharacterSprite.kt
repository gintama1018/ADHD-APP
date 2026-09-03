package com.circuitsense.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

/**
 * "Sparky" the Cartoon Electron Character Sprite.
 * Designed specifically for ADHD attention retention:
 * - Expressive animated eyes and cute reaction faces.
 * - Dynamic squish/stretch and speed trails along wires.
 * - Squeezed, struggling face when passing through the resistor.
 */
object CharacterSprite {

    fun draw(
        drawScope: DrawScope,
        position: Offset,
        motionProgress: Float,
        isResisting: Boolean = false,
        speedFactor: Float = 1.0f
    ) {
        with(drawScope) {
            val cx = position.x
            val cy = position.y
            val radius = 16f

            // Trailing motion particles
            val trailOffset1 = Offset(cx - 10f * speedFactor, cy)
            val trailOffset2 = Offset(cx - 20f * speedFactor, cy)
            drawCircle(
                color = Color(0x6600E5FF),
                radius = radius * 0.65f,
                center = trailOffset1
            )
            drawCircle(
                color = Color(0x3300E5FF),
                radius = radius * 0.35f,
                center = trailOffset2
            )

            // Outer glowing aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x9900E5FF),
                        Color(0x3300E5FF),
                        Color(0x0000E5FF)
                    ),
                    center = position,
                    radius = radius * 2.2f
                ),
                radius = radius * 2.2f,
                center = position
            )

            // Main electric body
            val bodyColor = if (isResisting) Color(0xFFFF9100) else Color(0xFF00E5FF)
            drawCircle(
                color = bodyColor,
                radius = radius,
                center = position
            )

            // Sparkle / energy highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.35f,
                center = Offset(cx - 4f, cy - 4f)
            )

            // Cute cartoon face
            // Eyes
            val blink = (sin(motionProgress * 15f) > 0.95f)
            if (blink) {
                // Closed blinking eye lines
                drawLine(
                    color = Color(0xFF0D1B2A),
                    start = Offset(cx - 7f, cy - 2f),
                    end = Offset(cx - 2f, cy - 2f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF0D1B2A),
                    start = Offset(cx + 2f, cy - 2f),
                    end = Offset(cx + 7f, cy - 2f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            } else {
                // Wide open cute anime eyes
                drawCircle(
                    color = Color(0xFF0D1B2A),
                    radius = 3.2f,
                    center = Offset(cx - 5f, cy - 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.2f,
                    center = Offset(cx - 6f, cy - 3f)
                )

                drawCircle(
                    color = Color(0xFF0D1B2A),
                    radius = 3.2f,
                    center = Offset(cx + 5f, cy - 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.2f,
                    center = Offset(cx + 4f, cy - 3f)
                )
            }

            // Mouth
            if (isResisting) {
                // Wavy/straining mouth (hitting resistance)
                val strainMouth = Path().apply {
                    moveTo(cx - 5f, cy + 5f)
                    lineTo(cx - 2f, cy + 3f)
                    lineTo(cx + 2f, cy + 7f)
                    lineTo(cx + 5f, cy + 4f)
                }
                drawPath(
                    path = strainMouth,
                    color = Color(0xFF0D1B2A),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            } else {
                // Happy curved smile
                val smilePath = Path().apply {
                    moveTo(cx - 5f, cy + 4f)
                    quadraticBezierTo(cx, cy + 8f, cx + 5f, cy + 4f)
                }
                drawPath(
                    path = smilePath,
                    color = Color(0xFF0D1B2A),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
