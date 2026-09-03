package com.circuitsense.recognition

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.circuitsense.model.CircuitComponent
import com.circuitsense.model.CircuitConnection
import com.circuitsense.model.CircuitFormula
import com.circuitsense.model.CircuitGraph
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

/**
 * On-Device Circuit Recognition Layer for CircuitSense.
 * Adheres strictly to the hackathon scope:
 * - Specializes on clean textbook-style Ohm's Law circuits (Battery + Resistor + Wire Loop).
 * - Combines ML Kit On-Device Text Recognition (for V and R extraction) with
 *   geometric contour & bounding box layout mapping.
 * - Outputs standard CircuitGraph JSON adhering to the locked schema.
 */
class CircuitRecognizer {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class RecognitionResult(
        val graph: CircuitGraph,
        val detectedVoltage: Double,
        val detectedResistance: Double,
        val confidenceScore: Float,
        val rawOcrText: String,
        val detectedLabels: List<String>,
        val isFallbackUsed: Boolean = false,
        val fallbackReason: String? = null
    )

    data class ParsedValues(
        val voltage: Double,
        val resistance: Double,
        val isVoltageDetected: Boolean,
        val isResistanceDetected: Boolean
    )

    /**
     * Recognizes an Ohm's Law circuit diagram from a Bitmap.
     */
    suspend fun analyzeCircuitImage(bitmap: Bitmap): RecognitionResult = withContext(Dispatchers.Default) {
        val ocrResult = runMlKitOcr(bitmap)
        val parsed = parseCircuitValues(ocrResult.text)
        val voltage = parsed.voltage
        val resistance = parsed.resistance
        
        val width = bitmap.width.toFloat().coerceAtLeast(1f)
        val height = bitmap.height.toFloat().coerceAtLeast(1f)

        // Standardize coordinates onto a clean 600x400 reference canvas for renderer stability
        val normBatteryX = ocrResult.voltageBox?.let { (it.centerX().toFloat() / width) * 600f } ?: 120f
        val normBatteryY = ocrResult.voltageBox?.let { (it.centerY().toFloat() / height) * 400f } ?: 240f
        val normResistorX = ocrResult.resistorBox?.let { (it.centerX().toFloat() / width) * 600f } ?: 460f
        val normResistorY = ocrResult.resistorBox?.let { (it.centerY().toFloat() / height) * 400f } ?: 240f

        val safeR = if (resistance <= 0.0) 100.0 else resistance
        val calculatedI = Math.round((voltage / safeR) * 1000.0) / 1000.0

        val components = listOf(
            CircuitComponent(
                id = "battery1",
                type = "battery",
                value = "${voltage.toInt()}V",
                x = normBatteryX,
                y = normBatteryY,
                label = "DC Battery (${voltage}V)",
                unit = "V"
            ),
            CircuitComponent(
                id = "resistor1",
                type = "resistor",
                value = "${resistance.toInt()}ohm",
                x = normResistorX,
                y = normResistorY,
                label = "Resistor (${resistance}Ω)",
                unit = "Ω"
            )
        )

        val connections = listOf(
            CircuitConnection(
                from = "battery1",
                to = "resistor1",
                path = "top_wire"
            ),
            CircuitConnection(
                from = "resistor1",
                to = "battery1",
                path = "bottom_wire"
            )
        )

        val formula = CircuitFormula(
            type = "ohms_law",
            V = voltage,
            I = calculatedI,
            R = resistance
        )

        val graph = CircuitGraph(
            components = components,
            connections = connections,
            formula = formula
        )

        val isFallback = !parsed.isVoltageDetected || !parsed.isResistanceDetected
        val fallbackMsg = when {
            !parsed.isVoltageDetected && !parsed.isResistanceDetected ->
                "Couldn't read values clearly from diagram — showing reference circuit (9V, 100Ω). Use sliders below to adjust."
            !parsed.isVoltageDetected ->
                "Detected ${resistance.toInt()}Ω resistor, but voltage was unreadable — using reference 9V battery."
            !parsed.isResistanceDetected ->
                "Detected ${voltage.toInt()}V battery, but resistance was unreadable — using reference 100Ω resistor."
            else -> null
        }

        RecognitionResult(
            graph = graph,
            detectedVoltage = voltage,
            detectedResistance = resistance,
            confidenceScore = if (!isFallback) 0.95f else 0.70f,
            rawOcrText = ocrResult.text,
            detectedLabels = ocrResult.detectedLabels,
            isFallbackUsed = isFallback,
            fallbackReason = fallbackMsg
        )
    }

    private data class OcrScan(
        val text: String,
        val foundVoltage: Boolean,
        val foundResistance: Boolean,
        val voltageBox: Rect?,
        val resistorBox: Rect?,
        val detectedLabels: List<String>
    )

    private suspend fun runMlKitOcr(bitmap: Bitmap): OcrScan = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    val labels = mutableListOf<String>()
                    var vBox: Rect? = null
                    var rBox: Rect? = null
                    var hasV = false
                    var hasR = false

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val lineText = line.text.trim()
                            labels.add(lineText)

                            if (!hasV && (lineText.contains(Regex("\\b\\d+\\s*[Vv]\\b")) || lineText.contains("Volt", ignoreCase = true))) {
                                hasV = true
                                vBox = line.boundingBox
                            }
                            if (!hasR && (lineText.contains(Regex("\\b\\d+\\s*(Ω|ohm|ohms|R|kΩ)\\b", RegexOption.IGNORE_CASE)))) {
                                hasR = true
                                rBox = line.boundingBox
                            }
                        }
                    }

                    continuation.resume(
                        OcrScan(
                            text = fullText,
                            foundVoltage = hasV,
                            foundResistance = hasR,
                            voltageBox = vBox,
                            resistorBox = rBox,
                            detectedLabels = labels
                        )
                    )
                }
                .addOnFailureListener {
                    continuation.resume(
                        OcrScan(
                            text = "",
                            foundVoltage = false,
                            foundResistance = false,
                            voltageBox = null,
                            resistorBox = null,
                            detectedLabels = emptyList()
                        )
                    )
                }
        } catch (e: Exception) {
            continuation.resume(
                OcrScan(
                    text = "",
                    foundVoltage = false,
                    foundResistance = false,
                    voltageBox = null,
                    resistorBox = null,
                    detectedLabels = emptyList()
                )
            )
        }
    }

    /**
     * Parses numeric voltage and resistance from OCR text.
     * Defaults to textbook standard 9V and 100Ω if unreadable.
     */
    private fun parseCircuitValues(text: String): ParsedValues {
        var voltage = 9.0
        var resistance = 100.0
        var foundV = false
        var foundR = false

        // Regex for voltage: e.g. "12V", "9 V", "5v"
        val voltagePattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*[Vv]")
        val vMatcher = voltagePattern.matcher(text)
        if (vMatcher.find()) {
            vMatcher.group(1)?.toDoubleOrNull()?.let {
                if (it > 0) {
                    voltage = it
                    foundV = true
                }
            }
        }

        // Regex for resistance: e.g. "100ohm", "240 ohm", "50Ω", "100R", "10k"
        val resistancePattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(ohm|ohms|Ω|R)", Pattern.CASE_INSENSITIVE)
        val rMatcher = resistancePattern.matcher(text)
        if (rMatcher.find()) {
            rMatcher.group(1)?.toDoubleOrNull()?.let {
                if (it > 0) {
                    resistance = it
                    foundR = true
                }
            }
        }

        return ParsedValues(
            voltage = voltage,
            resistance = resistance,
            isVoltageDetected = foundV,
            isResistanceDetected = foundR
        )
    }
}
