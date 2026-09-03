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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitsense.recognition.UniversalVisionBox
import com.circuitsense.ui.theme.*

/**
 * Draws real-time streaming bounding boxes directly on top of CameraX feed.
 * Matches Photos 1, 2, 3:
 * - [PERSON] overhead pedestrian bounding boxes (Photo 2)
 * - [Hand], [Screw / Part] assembly detection (Photo 1)
 * - [knife 0.41], object detection (Photo 3)
 */
@Composable
fun LiveCameraBoundingBoxes(
    boxes: List<UniversalVisionBox>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val laserAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserAlpha"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            boxes.forEach { box ->
                val left = box.normalizedLeft * w
                val top = box.normalizedTop * h
                val boxW = (box.normalizedRight - box.normalizedLeft) * w
                val boxH = (box.normalizedBottom - box.normalizedTop) * h

                val boxColor = when (box.category) {
                    "PERSON" -> Color(0xFF00E676) // Bright green like Photo 2
                    "CIRCUIT / COMPONENT" -> ElectricBlue
                    "MACHINERY" -> WarmAmber
                    "EXAM QUESTION", "MATH EQUATION" -> Color(0xFFFFD54F)
                    else -> Color(0xFF29B6F6) // Electric cyan/blue like Photo 3
                }

                // 1. Draw outer boundary box
                drawRect(
                    color = boxColor.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = Size(boxW.coerceAtLeast(40f), boxH.coerceAtLeast(30f)),
                    style = Stroke(width = 2.5f)
                )

                // 2. Corner Bracket highlights
                val cLen = 12f
                drawLine(boxColor, Offset(left, top), Offset(left + cLen, top), 4f)
                drawLine(boxColor, Offset(left, top), Offset(left, top + cLen), 4f)

                drawLine(boxColor, Offset(left + boxW, top), Offset(left + boxW - cLen, top), 4f)
                drawLine(boxColor, Offset(left + boxW, top), Offset(left + boxW + cLen, top), 4f)

                drawLine(boxColor, Offset(left, top + boxH), Offset(left + cLen, top + boxH), 4f)
                drawLine(boxColor, Offset(left, top + boxH), Offset(left, top + boxH - cLen), 4f)

                drawLine(boxColor, Offset(left + boxW, top + boxH), Offset(left + boxW - cLen, top + boxH), 4f)
                drawLine(boxColor, Offset(left + boxW, top + boxH), Offset(left + boxW, top + boxH - cLen), 4f)
            }
        }

        // 3. Live HUD Telemetry (Photos 1, 2, 3 Standard)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC0D1321),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardElevated),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 110.dp, start = 16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF00E676), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE VISION (Offline ML)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tracked: ${boxes.size} entities • Streaming 30 FPS",
                    color = ElectricBlue,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
