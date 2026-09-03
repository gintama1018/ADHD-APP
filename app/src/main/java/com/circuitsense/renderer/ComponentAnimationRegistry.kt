package com.circuitsense.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.circuitsense.model.CircuitComponent
import kotlin.math.sin

/**
 * Storytelling / Narration Beat tied to component animation.
 */
data class AnimationBeat(
    val id: String,
    val title: String,
    val narrationText: String,
    val cameraTarget: Offset,
    val cameraZoom: Float,
    val durationMs: Long,
    val calloutText: String? = null
)

/**
 * Strategy interface for generic component rendering & animation behavior.
 * Decouples the Canvas engine from hardcoded component types.
 */
interface ComponentRenderer {
    fun draw(
        drawScope: DrawScope,
        component: CircuitComponent,
        animationProgress: Float,
        isFocused: Boolean,
        currentAmps: Double
    )

    fun getFocusZoom(): Float = 2.5f

    fun createIntroBeat(component: CircuitComponent): AnimationBeat
}

/**
 * Registry holding renderers for any circuit component type.
 * Demonstrates clean, extensible architecture: new components can be added
 * without touching core canvas loop.
 */
object ComponentAnimationRegistry {
    private val renderers = mutableMapOf<String, ComponentRenderer>()

    init {
        register("battery", BatteryComponentRenderer())
        register("resistor", ResistorComponentRenderer())
        register("lamp", LampComponentRenderer())
        register("bulb", LampComponentRenderer())
        register("default", GenericBoxRenderer())
    }

    fun register(type: String, renderer: ComponentRenderer) {
        renderers[type.lowercase()] = renderer
    }

    fun getRenderer(type: String): ComponentRenderer {
        return renderers[type.lowercase()] ?: renderers["default"]!!
    }
}

/**
 * Battery Renderer:
 * Visualizes DC battery symbol (parallel plates: long positive, short negative),
 * electric potential field aura, and charge separation ("Current is born here!").
 */
class BatteryComponentRenderer : ComponentRenderer {

    override fun draw(
        drawScope: DrawScope,
        component: CircuitComponent,
        animationProgress: Float,
        isFocused: Boolean,
        currentAmps: Double
    ) {
        val cx = component.x
        val cy = component.y
        val strokeWidth = 5f

        with(drawScope) {
            // If focused, draw pulsing electric field aura
            if (isFocused) {
                val pulseRadius = 55f + (sin(animationProgress * 6.28f) * 8f)
                drawCircle(
                    color = Color(0x3300E5FF),
                    radius = pulseRadius,
                    center = Offset(cx, cy)
                )
            }

            // Positive terminal plate (longer horizontal plate at top)
            val plateHalfWidth = 28f
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(cx - plateHalfWidth, cy - 10f),
                end = Offset(cx + plateHalfWidth, cy - 10f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Negative terminal plate (shorter, thicker horizontal plate at bottom)
            val negHalfWidth = 16f
            drawLine(
                color = Color(0xFFFF5252),
                start = Offset(cx - negHalfWidth, cy + 10f),
                end = Offset(cx + negHalfWidth, cy + 10f),
                strokeWidth = strokeWidth * 1.8f,
                cap = StrokeCap.Round
            )

            // Battery terminal markings (+ and -)
            // "+" symbol near positive plate
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(cx - 36f, cy - 14f),
                end = Offset(cx - 36f, cy - 6f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(cx - 40f, cy - 10f),
                end = Offset(cx - 32f, cy - 10f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // "-" symbol near negative plate
            drawLine(
                color = Color(0xFFFF5252),
                start = Offset(cx - 40f, cy + 10f),
                end = Offset(cx - 32f, cy + 10f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Top lead stub (connects seamlessly to top wire at cy - 35f)
            drawLine(
                color = Color(0xFF475569),
                start = Offset(cx, cy - 35f),
                end = Offset(cx, cy - 10f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Bottom lead stub (connects seamlessly to bottom wire at cy + 35f)
            drawLine(
                color = Color(0xFF475569),
                start = Offset(cx, cy + 10f),
                end = Offset(cx, cy + 35f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
    }

    override fun createIntroBeat(component: CircuitComponent): AnimationBeat {
        return AnimationBeat(
            id = "beat_battery_${component.id}",
            title = "Potential Difference (Voltage)",
            narrationText = "Here at the ${component.value} battery, chemical energy separates electrical charges. A potential difference is established, and current is born!",
            cameraTarget = Offset(component.x, component.y),
            cameraZoom = 2.6f,
            durationMs = 4500,
            calloutText = "⚡ ${component.value}: Potential Difference"
        )
    }
}

/**
 * Resistor Renderer:
 * Visualizes zigzag resistor symbol with animated resistance friction callout,
 * electron scattering nodes, and heat dissipation waves.
 * Oriented along the right circuit leg with vertical leads.
 */
class ResistorComponentRenderer : ComponentRenderer {

    override fun draw(
        drawScope: DrawScope,
        component: CircuitComponent,
        animationProgress: Float,
        isFocused: Boolean,
        currentAmps: Double
    ) {
        val cx = component.x
        val cy = component.y

        with(drawScope) {
            // Focused heat glow effect
            if (isFocused) {
                val heatAlpha = (0.25f + 0.15f * sin(animationProgress * 10f)).coerceIn(0.1f, 0.5f)
                drawCircle(
                    color = Color(0xFFFF6D00).copy(alpha = heatAlpha),
                    radius = 50f,
                    center = Offset(cx, cy)
                )
            }

            // Vertical Zigzag Path from (cx, cy - 35f) to (cx, cy + 35f)
            val zigzagPath = Path().apply {
                moveTo(cx, cy - 35f)
                lineTo(cx, cy - 25f)
                lineTo(cx - 14f, cy - 17f)
                lineTo(cx + 14f, cy - 1f)
                lineTo(cx - 14f, cy + 15f)
                lineTo(cx, cy + 25f)
                lineTo(cx, cy + 35f)
            }

            // Resistor conductor
            drawPath(
                path = zigzagPath,
                color = Color(0xFFFF9100),
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Animated heat particles if focused
            if (isFocused) {
                val waveOffset = (animationProgress * 30f) % 20f
                drawLine(
                    color = Color(0xFFFF3D00).copy(alpha = 0.7f),
                    start = Offset(cx + 20f, cy - 15f - waveOffset),
                    end = Offset(cx + 30f, cy - 20f - waveOffset),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFF3D00).copy(alpha = 0.7f),
                    start = Offset(cx + 20f, cy + 10f - waveOffset),
                    end = Offset(cx + 30f, cy + 5f - waveOffset),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    override fun createIntroBeat(component: CircuitComponent): AnimationBeat {
        return AnimationBeat(
            id = "beat_resistor_${component.id}",
            title = "Electrical Resistance",
            narrationText = "Now current enters the ${component.value} resistor. Collisions with the atomic lattice impede the flow, converting kinetic electrical energy into thermal heat!",
            cameraTarget = Offset(component.x, component.y),
            cameraZoom = 2.6f,
            durationMs = 4800,
            calloutText = "🔥 ${component.value}: Limits Flow (Ohm's Law)"
        )
    }
}

/**
 * Lamp Component Renderer (bonus component proving extensible registry architecture).
 */
class LampComponentRenderer : ComponentRenderer {
    override fun draw(
        drawScope: DrawScope,
        component: CircuitComponent,
        animationProgress: Float,
        isFocused: Boolean,
        currentAmps: Double
    ) {
        val cx = component.x
        val cy = component.y
        with(drawScope) {
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 28f,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
            // Cross filament inside
            drawLine(
                color = Color(0xFFFFCA28),
                start = Offset(cx - 18f, cy - 18f),
                end = Offset(cx + 18f, cy + 18f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFFFFCA28),
                start = Offset(cx - 18f, cy + 18f),
                end = Offset(cx + 18f, cy - 18f),
                strokeWidth = 3f
            )
        }
    }

    override fun createIntroBeat(component: CircuitComponent): AnimationBeat {
        return AnimationBeat(
            id = "beat_lamp_${component.id}",
            title = "Electrical Load",
            narrationText = "Current passes through the filament, producing visible photons!",
            cameraTarget = Offset(component.x, component.y),
            cameraZoom = 2.2f,
            durationMs = 3500
        )
    }
}

/**
 * Generic Box Renderer for unrecognized/extensible custom components.
 */
class GenericBoxRenderer : ComponentRenderer {
    override fun draw(
        drawScope: DrawScope,
        component: CircuitComponent,
        animationProgress: Float,
        isFocused: Boolean,
        currentAmps: Double
    ) {
        val cx = component.x
        val cy = component.y
        with(drawScope) {
            drawRect(
                color = Color(0xFF64B5F6),
                topLeft = Offset(cx - 30f, cy - 20f),
                size = Size(60f, 40f),
                style = Stroke(width = 4f)
            )
        }
    }

    override fun createIntroBeat(component: CircuitComponent): AnimationBeat {
        return AnimationBeat(
            id = "beat_gen_${component.id}",
            title = component.label ?: "Circuit Element",
            narrationText = "Current flows through ${component.type} with value ${component.value}.",
            cameraTarget = Offset(component.x, component.y),
            cameraZoom = 2.0f,
            durationMs = 3000
        )
    }
}
