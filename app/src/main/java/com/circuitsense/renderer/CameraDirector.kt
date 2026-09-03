package com.circuitsense.renderer

import androidx.compose.ui.geometry.Offset
import com.circuitsense.model.CircuitGraph

/**
 * Storytelling phases for the physics tutor motion graphics.
 */
enum class StoryPhase(val title: String, val subtitle: String) {
    OVERVIEW("Circuit Schematics", "Recognized circuit graph loaded from JSON"),
    BATTERY_FOCUS("Step 1: Current is Born", "Chemical potential sets up electric field (Voltage)"),
    WIRE_TRANSIT("Step 2: Traveling along the Conductor", "Electrons accelerate through the metallic wire"),
    RESISTOR_FOCUS("Step 3: Resistance Encountered", "Collisions with lattice atoms impede electron flow"),
    FULL_LOOP("Step 4: Continuous Equilibrium", "Steady-state current loop governed by Ohm's Law: I = V / R")
}

data class CameraTransform(
    val scale: Float,
    val panX: Float,
    val panY: Float
)

/**
 * Coordinates cinematic camera zoom/pan choreography across the canvas.
 */
class CameraDirector(
    private val graph: CircuitGraph,
    private val canvasWidth: Float = 600f,
    private val canvasHeight: Float = 400f
) {
    val battery = graph.getBattery() ?: graph.components.firstOrNull()
    val resistor = graph.getResistor() ?: graph.components.getOrNull(1)

    private val centerOffset = Offset(canvasWidth / 2f, canvasHeight / 2f)

    /**
     * Calculates the target camera transform for a given story phase.
     */
    fun getTargetTransform(phase: StoryPhase, transitProgress: Float = 0f): CameraTransform {
        return when (phase) {
            StoryPhase.OVERVIEW -> {
                CameraTransform(scale = 1.0f, panX = 0f, panY = 0f)
            }
            StoryPhase.BATTERY_FOCUS -> {
                val bx = battery?.x ?: (canvasWidth * 0.25f)
                val by = battery?.y ?: (canvasHeight * 0.5f)
                val scale = 2.5f
                val panX = (centerOffset.x - bx) * (scale - 1.0f)
                val panY = (centerOffset.y - by) * (scale - 1.0f)
                CameraTransform(scale = scale, panX = panX, panY = panY)
            }
            StoryPhase.WIRE_TRANSIT -> {
                val startX = battery?.x ?: 120f
                val endX = resistor?.x ?: 460f
                val currX = startX + (endX - startX) * transitProgress
                val currY = (battery?.y ?: 240f) - 90f // top wire level
                val scale = 1.8f
                val panX = (centerOffset.x - currX) * (scale - 1.0f)
                val panY = (centerOffset.y - currY) * (scale - 1.0f)
                CameraTransform(scale = scale, panX = panX, panY = panY)
            }
            StoryPhase.RESISTOR_FOCUS -> {
                val rx = resistor?.x ?: (canvasWidth * 0.75f)
                val ry = resistor?.y ?: (canvasHeight * 0.5f)
                val scale = 2.6f
                val panX = (centerOffset.x - rx) * (scale - 1.0f)
                val panY = (centerOffset.y - ry) * (scale - 1.0f)
                CameraTransform(scale = scale, panX = panX, panY = panY)
            }
            StoryPhase.FULL_LOOP -> {
                CameraTransform(scale = 1.0f, panX = 0f, panY = 0f)
            }
        }
    }

    /**
     * Linear interpolation between two camera transforms.
     */
    fun lerpTransform(start: CameraTransform, end: CameraTransform, fraction: Float): CameraTransform {
        val t = fraction.coerceIn(0f, 1f)
        // Smooth ease-in-out curve for cinematic feel
        val easeT = t * t * (3f - 2f * t)
        return CameraTransform(
            scale = start.scale + (end.scale - start.scale) * easeT,
            panX = start.panX + (end.panX - start.panX) * easeT,
            panY = start.panY + (end.panY - start.panY) * easeT
        )
    }
}
