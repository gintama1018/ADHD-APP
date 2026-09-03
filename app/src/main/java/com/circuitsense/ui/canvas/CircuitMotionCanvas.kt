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
import com.circuitsense.renderer.SparkyExpression
import com.circuitsense.renderer.StoryPhase
import com.circuitsense.ui.components.drawPlaygroundDotGrid
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

            // 1. Draw Photo 4 Dot-Matrix Playground Background
            drawPlaygroundDotGrid(canvasW, canvasH)

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

                val centerY = canvasH * 0.46f
                val loopHalfHeight = (canvasH * 0.18f).coerceIn(130f, 260f)
                val topWireY = centerY - loopHalfHeight
                val bottomWireY = centerY + loopHalfHeight

                val bX = canvasW * 0.20f
                val bY = centerY
                val rX = canvasW * 0.80f
                val rY = centerY

                // 3. Draw Connecting Copper Wires (Loop)
                drawCircuitWires(
                    startX = bX,
                    endX = rX,
                    topY = topWireY,
                    bottomY = bottomWireY,
                    batteryY = bY,
                    resistorY = rY
                )

                // 4. Render Components precisely at wire gap junctions
                val currentAmps = graph.formula.I
                graph.components.forEach { comp ->
                    val isBattery = comp.type.equals("battery", ignoreCase = true)
                    val isResistor = comp.type.equals("resistor", ignoreCase = true)

                    val isFocused = when {
                        isBattery -> currentPhase == StoryPhase.BATTERY_FOCUS
                        isResistor -> currentPhase == StoryPhase.RESISTOR_FOCUS
                        else -> false
                    }

                    // Align precisely with the left/right wire endpoints!
                    val compX = if (isBattery) bX else rX
                    val compY = if (isBattery) bY else rY

                    val adjustedComp = comp.copy(
                        x = compX,
                        y = compY
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
                    bY = bY,
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
                color = Color(0x222EC5FF),
                radius = 1.5f,
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
        // Battery top lead curving gently up to top wire
        moveTo(startX, batteryY - 35f)
        quadraticBezierTo(startX, topY, (startX + endX) / 2f, topY)
        quadraticBezierTo(endX, topY, endX, resistorY - 35f)

        // Resistor bottom lead curving gently down to bottom wire
        moveTo(endX, resistorY + 35f)
        quadraticBezierTo(endX, bottomY, (startX + endX) / 2f, bottomY)
        quadraticBezierTo(startX, bottomY, startX, batteryY + 35f)
    }

    // Outer subtle copper wire glow
    drawPath(
        path = wirePath,
        color = Color(0x332EC5FF),
        style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Solid conductor line
    drawPath(
        path = wirePath,
        color = Color(0xFF475569),
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.renderCurrentFlow(
    phase: StoryPhase,
    progress: Float,
    bX: Float,
    bY: Float,
    rX: Float,
    rY: Float,
    topY: Float,
    bottomY: Float,
    currentAmps: Double
) {
    when (phase) {
        StoryPhase.OVERVIEW -> {
            // Idle subtle pulse right at battery
            CharacterSprite.draw(
                drawScope = this,
                position = Offset(bX, bY),
                motionProgress = progress,
                expression = SparkyExpression.CALM,
                speedFactor = 0.5f
            )
        }
        StoryPhase.BATTERY_FOCUS -> {
            // Sparky appears at the battery terminal with excited wide eyes
            val bounceY = bY + sin(progress * 8f) * 6f
            CharacterSprite.draw(
                drawScope = this,
                position = Offset(bX, bounceY),
                motionProgress = progress,
                expression = SparkyExpression.EXCITED,
                speedFactor = 1.0f
            )
        }
        StoryPhase.WIRE_TRANSIT -> {
            // Sparky travels from battery up through left leg and along top wire
            val sparkyOffset = when {
                progress < 0.3f -> {
                    // Up from battery to top wire
                    val p = progress / 0.3f
                    Offset(bX, bY - (bY - topY) * p)
                }
                progress < 0.8f -> {
                    // Across top wire
                    val p = (progress - 0.3f) / 0.5f
                    Offset(bX + (rX - bX) * p, topY)
                }
                else -> {
                    // Down from top wire into resistor
                    val p = (progress - 0.8f) / 0.2f
                    Offset(rX, topY + (rY - topY) * p)
                }
            }
            CharacterSprite.draw(
                drawScope = this,
                position = sparkyOffset,
                motionProgress = progress,
                expression = SparkyExpression.CALM,
                speedFactor = 1.5f
            )
        }
        StoryPhase.RESISTOR_FOCUS -> {
            // Sparky inside the resistor, non-uniformly squished by resistance collisions
            val jitterX = rX + sin(progress * 25f) * 3f
            val jitterY = rY + sin(progress * 30f) * 3f
            CharacterSprite.draw(
                drawScope = this,
                position = Offset(jitterX, jitterY),
                motionProgress = progress,
                expression = SparkyExpression.SQUISHED,
                speedFactor = 0.6f
            )
        }
        StoryPhase.FULL_LOOP -> {
            // Continuous looped current flow
            val speedMultiplier = (currentAmps * 2.0).coerceIn(0.5, 4.0).toFloat()
            val loopProgress = (progress * speedMultiplier) % 1.0f

            val totalPerimeter = 2 * (rX - bX) + 2 * (bottomY - topY)
            val topDist = (rX - bX)
            val rightDist = (bottomY - topY)
            val bottomDist = (rX - bX)

            val currentDist = loopProgress * totalPerimeter
            val pos: Offset = when {
                currentDist < topDist -> {
                    Offset(bX + currentDist, topY)
                }
                currentDist < topDist + rightDist -> {
                    val d = currentDist - topDist
                    Offset(rX, topY + d)
                }
                currentDist < topDist + rightDist + bottomDist -> {
                    val d = currentDist - (topDist + rightDist)
                    Offset(rX - d, bottomY)
                }
                else -> {
                    val d = currentDist - (topDist + rightDist + bottomDist)
                    Offset(bX, bottomY - d)
                }
            }

            val isInResistor = (pos.x in (rX - 30f)..(rX + 30f)) && (pos.y in (topY + 20f)..(bottomY - 20f))

            // Draw leading character sprite with expression matching state
            CharacterSprite.draw(
                drawScope = this,
                position = pos,
                motionProgress = progress,
                expression = if (isInResistor) SparkyExpression.SQUISHED else SparkyExpression.FLOWING,
                speedFactor = speedMultiplier
            )

            // Draw trailing secondary electrons in loop
            for (i in 1..4) {
                val offsetFraction = (loopProgress + (i * 0.22f)) % 1.0f
                val secDist = offsetFraction * totalPerimeter
                val secPos = calculateLoopPosition(secDist, bX, rX, topY, bottomY)
                drawCircle(
                    color = Color(0xFF2EC5FF).copy(alpha = 0.75f),
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
