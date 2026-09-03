package com.circuitsense.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitsense.model.CircuitGraph
import com.circuitsense.ui.theme.*

data class DetectedBox(
    val label: String,
    val confidence: Float,
    val x: Float, // Normalized 0..1
    val y: Float, // Normalized 0..1
    val width: Float, // Normalized 0..1
    val height: Float, // Normalized 0..1
    val color: Color
)

/**
 * AI Computer Vision Detection Overlay (Matching User's Reference Photos 1, 2, 3).
 * Renders live bounding boxes, object detection labels, confidence scores,
 * and component counters ([Inserted 8 of 8], [Fastened 7 of 8] style).
 */
@Composable
fun AiVisionDetectionOverlay(
    graph: CircuitGraph,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserScan")
    val scanFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LaserSweep"
    )

    val v = graph.formula.V.toInt()
    val r = graph.formula.R.toInt()

    val boxes = remember(graph) {
        listOf(
            DetectedBox("BATTERY ${v}V", 0.96f, 0.16f, 0.36f, 0.18f, 0.28f, ElectricBlue),
            DetectedBox("RESISTOR ${r}Ω", 0.94f, 0.66f, 0.36f, 0.20f, 0.28f, WarmAmber),
            DetectedBox("CONDUCTOR LOOP", 0.98f, 0.15f, 0.22f, 0.70f, 0.54f, Color(0xFF00E676))
        )
    }

    Box(modifier = modifier) {
        // 1. Draw Bounding Boxes and animated laser on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Animated Laser Scanning Line
            val laserY = h * scanFraction
            drawLine(
                color = ElectricBlue.copy(alpha = 0.5f),
                start = Offset(0f, laserY),
                end = Offset(w, laserY),
                strokeWidth = 2.5f
            )

            // Draw bounding boxes like YOLO / Vision detection
            boxes.forEach { box ->
                val left = box.x * w
                val top = box.y * h
                val boxW = box.width * w
                val boxH = box.height * h

                // Bounding rectangle
                drawRect(
                    color = box.color,
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    style = Stroke(width = 2.5f)
                )

                // Corner bracket accents
                val cornerLen = 14f
                // Top-Left
                drawLine(box.color, Offset(left, top), Offset(left + cornerLen, top), 4f)
                drawLine(box.color, Offset(left, top), Offset(left, top + cornerLen), 4f)
                // Top-Right
                drawLine(box.color, Offset(left + boxW, top), Offset(left + boxW - cornerLen, top), 4f)
                drawLine(box.color, Offset(left + boxW, top), Offset(left + boxW, top + cornerLen), 4f)
                // Bottom-Left
                drawLine(box.color, Offset(left, top + boxH), Offset(left + cornerLen, top + boxH), 4f)
                drawLine(box.color, Offset(left, top + boxH), Offset(left, top + boxH - cornerLen), 4f)
                // Bottom-Right
                drawLine(box.color, Offset(left + boxW, top + boxH), Offset(left + boxW - cornerLen, top + boxH), 4f)
                drawLine(box.color, Offset(left + boxW, top + boxH), Offset(left + boxW, top + boxH - cornerLen), 4f)
            }
        }

        // 2. High-Tech Telemetry Counter HUD (Matching Photo 1: Inserted 8 of 8 / Fastened 7 of 8)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC0D1321),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardElevated),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SuccessGreen, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Detected Components:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3 of 3",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ElectricBlue, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vision Confidence:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "96.4%",
                        color = ElectricBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
