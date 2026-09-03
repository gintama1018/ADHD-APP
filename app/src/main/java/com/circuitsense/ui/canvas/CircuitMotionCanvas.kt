package com.circuitsense.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.circuitsense.model.CircuitGraph
import com.circuitsense.renderer.CameraDirector
import com.circuitsense.renderer.CameraTransform
import com.circuitsense.renderer.CharacterSprite
import com.circuitsense.renderer.ComponentAnimationRegistry
import com.circuitsense.renderer.StoryPhase
import kotlin.math.sin

/**
 * Main Motion-Graphics Canvas for CircuitSense.
 * Renders the circuit diagram generically from CircuitGraph JSON.
 * Applies cinematic camera zoom/pan transformations and animates electron flow.
 */
@Composable
fun CircuitMotionCanvas(
    graph: CircuitGraph,
    currentPhase: StoryPhase,
    animationProgress: Float,
    cameraTransform: CameraTransform,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw subtle background coordinate grid
            drawGridBackground(canvasW, canvasH)

            // 2. Apply Camera Director Viewport Transformation (Pan & Zoom)
            withTransform({
                translate(left = cameraTransform.panX, top = cameraTransform.panY)
                scale(scaleX = cameraTransform.scale, scaleY = cameraTransform.scale, pivot = center)
            }) {
                // Coordinate reference mapping from JSON (defaults: 600x400 normalized space to actual canvas)
                val scaleRatioX = (canvasW / 600f).coerceAtLeast(0.8f)
                val scaleRatioY = (canvasH / 400f).coerceAtLeast(0.8f)

                val battery = graph.getBattery() ?: graph.components.firstOrNull()
                val resistor = graph.getResistor() ?: graph.components.getOrNull(1)

                val bX = (battery?.x ?: 120f) * scaleRatioX
                val bY = (battery?.y ?: 240f) * scaleRatioY
                val rX = (resistor?.x ?: 460f) * scaleRatioX
                val rY = (resistor?.y ?: 240f) * scaleRatioY

                val topWireY = bY - 80f
                val bottomWireY = bY + 80f

                // 3. Draw Connecting Copper Wires (Loop)
                drawCircuitWires(
                    startX = bX,
                    endX = rX,
                    topY = topWireY,
                    bottomY = bottomWireY,
                    batteryY = bY,
                    resistorY = rY
                )

                // 4. Render Components generically from Registry
                val currentAmps = graph.formula.I
                graph.components.forEach { comp ->
                    val isFocused = when (comp.type.lowercase()) {
                        "battery" -> currentPhase == StoryPhase.BATTERY_FOCUS
                        "resistor" -> currentPhase == StoryPhase.RESISTOR_FOCUS
                        else -> false
                    }

                    val adjustedComp = comp.copy(
                        x = comp.x * scaleRatioX,
                        y = comp.y * scaleRatioY
                    )

                    val renderer = ComponentAnimationRegistry.getRenderer(comp.type)
                    renderer.draw(
                        drawScope = this,
                        component = adjustedComp,
                        animationProgress = animationProgress,
                        isFocused = isFocused,
                        currentAmps = currentAmps
                    )
                }

                // 5. Draw Animated Cartoon Character & Current Particles
                renderCurrentFlow(
                    phase = currentPhase,
                    progress = animationProgress,
                    bX = bX,
                    rX = rX,
                    rY = rY,
                    topY = topWireY,
                    bottomY = bottomWireY,
                    currentAmps = currentAmps
                )

                // 6. Resistor Arrow Callout in RESISTOR_FOCUS beat
                if (currentPhase == StoryPhase.RESISTOR_FOCUS) {
                    drawResistorCallout(rX, rY)
                }
            }

            // 7. ADHD Focus Vignette: Darken edges when zoomed into a component
            if (cameraTransform.scale > 1.2f) {
                drawFocusVignette(canvasW, canvasH)
            }
        }
    }
}

private fun DrawScope.drawGridBackground(w: Float, h: Float) {
    val step = 40f
    var x = 0f
    while (x < w) {
        var y = 0f
        while (y < h) {
            drawCircle(
                color = Color(0x1A00E5FF),
                radius = 1.2f,
                center = Offset(x, y)
            )
            y += step
        }
        x += step
    }
}

private fun DrawScope.drawCircuitWires(
    startX: Float,
    endX: Float,
    topY: Float,
    bottomY: Float,
    batteryY: Float,
    resistorY: Float
) {
    val wirePath = Path().apply {
        // Battery top lead to top wire
        moveTo(startX, batteryY - 35f)
        lineTo(startX, topY)
        // Top wire towards resistor
        lineTo(endX, topY)
        // Top wire down to resistor top lead
        lineTo(endX, resistorY - 35f)

        // Resistor bottom lead to bottom wire
        moveTo(endX, resistorY + 35f)
        lineTo(endX, bottomY)
        // Bottom wire back towards battery
        lineTo(startX, bottomY)
        // Bottom wire up to battery bottom lead
        lineTo(startX, batteryY + 35f)
    }

    // Outer subtle copper wire glow
    drawPath(
        path = wirePath,
        color = Color(0x3300E5FF),
        style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Solid conductor line
    drawPath(
        path = wirePath,
        color = Color(0xFF37474F),
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.renderCurrentFlow(
    phase: StoryPhase,
    progress: Float,
    bX: Float,
    rX: Float,
    rY: Float,
    topY: Float,
    bottomY: Float,
    currentAmps: Double
) {
    when (phase) {
        StoryPhase.OVERVIEW -> {
            // Idle subtle pulse at battery
            CharacterSprite.draw(
                drawScope = this,
                position = Offset(bX, topY),
                motionProgress = progress,
                isResisting = false,
                speedFactor = 0.5f
            )
        }
        StoryPhase.BATTERY_FOCUS -> {
            // Sparky appears at the battery terminal with rising excitement
            val bounceY = topY + sin(progress * 8f) * 6f
            CharacterSprite.draw(
                drawScope = this,
                position = Offset(bX, bounceY),
                motionProgress = progress,
                isResisting = false,
                speedFactor = 1.0f
            )
        }
        StoryPhase.WIRE_TRANSIT -> {
            // Sparky runs along the top wire
            val currentX = bX + (rX - bX) * progress.coerceIn(0f, 1f)
            CharacterSprite.draw(
                drawScope = this,
                position = Offset(currentX, topY),
                motionProgress = progress,
                isResisting = false,
                speedFactor = 1.5f
            )
        }
        StoryPhase.RESISTOR_FOCUS -> {
            // Sparky is inside the resistor, struggling through the zigzag
            val jitterX = rX + sin(progress * 25f) * 4f
            val jitterY = rY + sin(progress * 30f) * 3f
            CharacterSprite.draw(
                drawScope = this,
                position = Offset(jitterX, jitterY),
                motionProgress = progress,
                isResisting = true,
                speedFactor = 0.6f
            )
        }
        StoryPhase.FULL_LOOP -> {
            // Continuous looped current flow
            // Speed scaled with current I = V / R
            val speedMultiplier = (currentAmps * 2.0).coerceIn(0.5, 4.0).toFloat()
            val loopProgress = (progress * speedMultiplier) % 1.0f

            // Calculate position along rectangular loop
            val totalPerimeter = 2 * (rX - bX) + 2 * (bottomY - topY)
            val topDist = (rX - bX)
            val rightDist = (bottomY - topY)
            val bottomDist = (rX - bX)

            val currentDist = loopProgress * totalPerimeter
            val pos: Offset = when {
                currentDist < topDist -> {
                    // Moving right along top wire
                    Offset(bX + currentDist, topY)
                }
                currentDist < topDist + rightDist -> {
                    // Moving down through resistor
                    val d = currentDist - topDist
                    Offset(rX, topY + d)
                }
                currentDist < topDist + rightDist + bottomDist -> {
                    // Moving left along bottom wire
                    val d = currentDist - (topDist + rightDist)
                    Offset(rX - d, bottomY)
                }
                else -> {
                    // Moving up through battery
                    val d = currentDist - (topDist + rightDist + bottomDist)
                    Offset(bX, bottomY - d)
                }
            }

            val isInResistor = (pos.x in (rX - 30f)..(rX + 30f)) && (pos.y in (topY + 20f)..(bottomY - 20f))

            // Draw leading character sprite
            CharacterSprite.draw(
                drawScope = this,
                position = pos,
                motionProgress = progress,
                isResisting = isInResistor,
                speedFactor = speedMultiplier
            )

            // Draw trailing secondary electrons in loop
            for (i in 1..4) {
                val offsetFraction = (loopProgress + (i * 0.22f)) % 1.0f
                val secDist = offsetFraction * totalPerimeter
                val secPos = calculateLoopPosition(secDist, bX, rX, topY, bottomY)
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                    radius = 8f,
                    center = secPos
                )
            }
        }
    }
}

private fun calculateLoopPosition(dist: Float, bX: Float, rX: Float, topY: Float, bottomY: Float): Offset {
    val topDist = (rX - bX)
    val rightDist = (bottomY - topY)
    val bottomDist = (rX - bX)

    return when {
        dist < topDist -> Offset(bX + dist, topY)
        dist < topDist + rightDist -> Offset(rX, topY + (dist - topDist))
        dist < topDist + rightDist + bottomDist -> Offset(rX - (dist - topDist - rightDist), bottomY)
        else -> Offset(bX, bottomY - (dist - topDist - rightDist - bottomDist))
    }
}

private fun DrawScope.drawResistorCallout(rX: Float, rY: Float) {
    // Callout badge with pointer arrow
    val boxX = rX - 110f
    val boxY = rY - 110f
    val boxW = 220f
    val boxH = 50f

    // Background card
    drawRoundRect(
        color = Color(0xEE1E2433),
        topLeft = Offset(boxX, boxY),
        size = Size(boxW, boxH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    )
    drawRoundRect(
        color = Color(0xFFFF9100),
        topLeft = Offset(boxX, boxY),
        size = Size(boxW, boxH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
        style = Stroke(width = 2.5f)
    )

    // Pointer arrow down to resistor
    val arrowPath = Path().apply {
        moveTo(rX - 10f, boxY + boxH)
        lineTo(rX, boxY + boxH + 15f)
        lineTo(rX + 10f, boxY + boxH)
        close()
    }
    drawPath(path = arrowPath, color = Color(0xFFFF9100))
}

private fun DrawScope.drawFocusVignette(w: Float, h: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xAA000000),
                Color(0xDD000000)
            ),
            center = Offset(w / 2f, h / 2f),
            radius = (w / 1.5f)
        ),
        topLeft = Offset.Zero,
        size = Size(w, h)
    )
}
