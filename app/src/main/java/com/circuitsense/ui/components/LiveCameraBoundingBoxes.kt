package com.circuitsense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.circuitsense.recognition.UniversalVisionBox

/**
 * Draws real-time streaming bounding boxes with label pills directly on top of CameraX feed.
 * Matches Photos 1, 2, 3:
 * - [PERSON] overhead pedestrian bounding boxes with label badges (Photo 2)
 * - [Hand], [Screw / Part] assembly detection (Photo 1)
 * - [knife 0.41], object detection with top label pill (Photo 3)
 */
@Composable
fun LiveCameraBoundingBoxes(
    boxes: List<UniversalVisionBox>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        boxes.forEach { box ->
            val left = box.normalizedLeft * w
            val top = box.normalizedTop * h
            val boxW = ((box.normalizedRight - box.normalizedLeft) * w).coerceAtLeast(60f)
            val boxH = ((box.normalizedBottom - box.normalizedTop) * h).coerceAtLeast(40f)

            val boxColor = when (box.category) {
                "PERSON" -> Color(0xFF00E676) // Bright green like Photo 2
                "CIRCUIT / COMPONENT" -> Color(0xFF00E5FF) // Electric blue
                "MACHINERY" -> Color(0xFFFFB300) // Warm amber
                "EXAM QUESTION", "MATH EQUATION" -> Color(0xFFFFD54F)
                else -> Color(0xFF00B0FF) // Vivid blue like Photo 3
            }

            // 1. Draw outer boundary rectangle
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                style = Stroke(width = 3f)
            )

            // 2. Corner Bracket highlights
            val cLen = 14f
            // Top-Left
            drawLine(boxColor, Offset(left, top), Offset(left + cLen, top), 5f)
            drawLine(boxColor, Offset(left, top), Offset(left, top + cLen), 5f)
            // Top-Right
            drawLine(boxColor, Offset(left + boxW, top), Offset(left + boxW - cLen, top), 5f)
            drawLine(boxColor, Offset(left + boxW, top), Offset(left + boxW, top + cLen), 5f)
            // Bottom-Left
            drawLine(boxColor, Offset(left, top + boxH), Offset(left + cLen, top + boxH), 5f)
            drawLine(boxColor, Offset(left, top + boxH), Offset(left, top + boxH - cLen), 5f)
            // Bottom-Right
            drawLine(boxColor, Offset(left + boxW, top + boxH), Offset(left + boxW - cLen, top + boxH), 5f)
            drawLine(boxColor, Offset(left + boxW, top + boxH), Offset(left + boxW, top + boxH - cLen), 5f)

            // 3. Top Label Badge Pill (Like Photo 3 "knife 0.41" & Photo 2 "PERSON")
            val labelString = "${box.label} ${String.format("%.2f", box.confidence)}"
            val textLayoutResult = textMeasurer.measure(
                text = labelString,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )

            val badgeWidth = textLayoutResult.size.width + 16f
            val badgeHeight = textLayoutResult.size.height + 8f
            val badgeTop = (top - badgeHeight).coerceAtLeast(0f)

            // Solid badge background
            drawRoundRect(
                color = boxColor,
                topLeft = Offset(left, badgeTop),
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Text on top of badge
            drawText(
                textMeasurer = textMeasurer,
                text = labelString,
                topLeft = Offset(left + 8f, badgeTop + 4f),
                style = TextStyle(
                    color = Color(0xFF0F111A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}
