package com.circuitsense.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.circuitsense.ui.theme.BackgroundDark
import com.circuitsense.ui.theme.ElectricBlue
import com.circuitsense.ui.theme.WarmAmber
import kotlin.math.sin

/**
 * Expression states for "Sparky the Electron", strictly matching DESIGN.md Section 5:
 * - EXCITED (Battery): Wide-eyed, big open smile.
 * - CALM (Wire transit): Gentle smile, content eyes.
 * - SQUISHED (Resistor): Non-uniform horizontal squish (squash & stretch), strained flat mouth.
 * - FLOWING (Full loop): Trailing alpha-faded copies for continuous motion illusion.
 */
enum class SparkyExpression {
    EXCITED,
    CALM,
    SQUISHED,
    FLOWING
}

object CharacterSprite {

    fun draw(
        drawScope: DrawScope,
        position: Offset,
        motionProgress: Float,
        expression: SparkyExpression = SparkyExpression.CALM,
        speedFactor: Float = 1.0f
    ) {
        with(drawScope) {
            val cx = position.x
            val cy = position.y
            val baseRadius = 18f

            // 1. Full loop trailing copies (faint alpha-faded duplicates along path)
            if (expression == SparkyExpression.FLOWING) {
                for (i in 1..3) {
                    val trailOffset = Offset(cx - (12f * i * speedFactor), cy)
                    val alpha = (0.35f / i)
                    drawCircle(
                        color = ElectricBlue.copy(alpha = alpha),
                        radius = baseRadius * (1f - (i * 0.15f)),
                        center = trailOffset
                    )
                }
            } else {
                // Subtle energy trail
                drawCircle(
                    color = ElectricBlue.copy(alpha = 0.25f),
                    radius = baseRadius * 0.7f,
                    center = Offset(cx - 10f * speedFactor, cy)
                )
            }

            // 2. Soft outer energy glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = 0.50f),
                        ElectricBlue.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = position,
                    radius = baseRadius * 2.2f
                ),
                radius = baseRadius * 2.2f,
                center = position
            )

            // 3. Main character body with non-uniform squash & stretch at resistor
            val scaleX = if (expression == SparkyExpression.SQUISHED) 1.35f else 1.0f
            val scaleY = if (expression == SparkyExpression.SQUISHED) 0.72f else 1.0f
            val bodyColor = if (expression == SparkyExpression.SQUISHED) WarmAmber else ElectricBlue

            withTransform({
                scale(scaleX = scaleX, scaleY = scaleY, pivot = position)
            }) {
                // Circular energy body
                drawCircle(
                    color = bodyColor,
                    radius = baseRadius,
                    center = position
                )

                // Energy highlight / glint
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = baseRadius * 0.30f,
                    center = Offset(cx - 5f, cy - 5f)
                )

                // 4. Cartoon Eyes
                val eyeColor = BackgroundDark
                val isBlinking = (expression == SparkyExpression.CALM && sin(motionProgress * 14f) > 0.94f)

                if (isBlinking) {
                    // Closed blinking curves
                    drawLine(
                        color = eyeColor,
                        start = Offset(cx - 8f, cy - 2f),
                        end = Offset(cx - 2f, cy - 2f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = eyeColor,
                        start = Offset(cx + 2f, cy - 2f),
                        end = Offset(cx + 8f, cy - 2f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                } else {
                    val eyeRadius = if (expression == SparkyExpression.EXCITED) 4.2f else 3.2f
                    // Left eye
                    drawCircle(color = eyeColor, radius = eyeRadius, center = Offset(cx - 6f, cy - 2f))
                    drawCircle(color = Color.White, radius = eyeRadius * 0.45f, center = Offset(cx - 7f, cy - 3f))

                    // Right eye
                    drawCircle(color = eyeColor, radius = eyeRadius, center = Offset(cx + 6f, cy - 2f))
                    drawCircle(color = Color.White, radius = eyeRadius * 0.45f, center = Offset(cx + 5f, cy - 3f))
                }

                // 5. Cartoon Mouth per expression state
                when (expression) {
                    SparkyExpression.EXCITED -> {
                        // Big open happy curve
                        val openMouth = Path().apply {
                            moveTo(cx - 6f, cy + 3f)
                            quadraticBezierTo(cx, cy + 9f, cx + 6f, cy + 3f)
                        }
                        drawPath(openMouth, eyeColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                    }
                    SparkyExpression.CALM, SparkyExpression.FLOWING -> {
                        // Gentle content smile
                        val smile = Path().apply {
                            moveTo(cx - 5f, cy + 4f)
                            quadraticBezierTo(cx, cy + 7f, cx + 5f, cy + 4f)
                        }
                        drawPath(smile, eyeColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
                    }
                    SparkyExpression.SQUISHED -> {
                        // Strained flattened mouth (squished by resistor collisions)
                        drawLine(
                            color = eyeColor,
                            start = Offset(cx - 6f, cy + 4f),
                            end = Offset(cx + 6f, cy + 4f),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }

    /**
     * Backward-compatible overload for existing calls.
     */
    fun draw(
        drawScope: DrawScope,
        position: Offset,
        motionProgress: Float,
        isResisting: Boolean = false,
        speedFactor: Float = 1.0f
    ) {
        val expr = if (isResisting) SparkyExpression.SQUISHED else SparkyExpression.FLOWING
        draw(drawScope, position, motionProgress, expr, speedFactor)
    }
}
