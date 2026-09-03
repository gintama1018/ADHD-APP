package com.circuitsense.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Standard Circuit JSON Schema for CircuitSense.
 * Adheres strictly to the hackathon-specified schema:
 * {
 *   "components": [
 *     { "id": "battery1", "type": "battery", "value": "9V", "x": 50, "y": 200 },
 *     { "id": "resistor1", "type": "resistor", "value": "100ohm", "x": 300, "y": 200 }
 *   ],
 *   "connections": [
 *     { "from": "battery1", "to": "resistor1", "path": "top_wire" },
 *     { "from": "resistor1", "to": "battery1", "path": "bottom_wire" }
 *   ],
 *   "formula": { "type": "ohms_law", "V": 9, "I": 0.09, "R": 100 }
 * }
 */
@Serializable
data class CircuitComponent(
    val id: String,
    val type: String, // "battery", "resistor", "wire", "switch", "lamp"
    val value: String, // e.g. "9V", "100ohm", "12V"
    val x: Float,
    val y: Float,
    val label: String? = null,
    val unit: String? = null
)

@Serializable
data class CircuitConnection(
    val from: String,
    val to: String,
    val path: String // e.g. "top_wire", "bottom_wire", "right_wire", "left_wire"
)

@Serializable
data class CircuitFormula(
    val type: String = "ohms_law",
    val V: Double,
    val I: Double,
    val R: Double
)

@Serializable
data class CircuitGraph(
    val components: List<CircuitComponent>,
    val connections: List<CircuitConnection>,
    val formula: CircuitFormula
) {
    companion object {
        private val jsonFormat = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun fromJson(jsonString: String): CircuitGraph {
            return jsonFormat.decodeFromString(serializer(), jsonString)
        }

        /**
         * Creates a standard validated Ohm's Law circuit graph.
         */
        fun createOhmsLawGraph(
            voltage: Double = 9.0,
            resistance: Double = 100.0,
            batteryX: Float = 120f,
            batteryY: Float = 260f,
            resistorX: Float = 420f,
            resistorY: Float = 260f
        ): CircuitGraph {
            val safeR = if (resistance <= 0.0) 0.01 else resistance
            val current = voltage / safeR

            val components = listOf(
                CircuitComponent(
                    id = "battery1",
                    type = "battery",
                    value = "${voltage.toInt()}V",
                    x = batteryX,
                    y = batteryY,
                    label = "DC Battery",
                    unit = "V"
                ),
                CircuitComponent(
                    id = "resistor1",
                    type = "resistor",
                    value = "${resistance.toInt()}ohm",
                    x = resistorX,
                    y = resistorY,
                    label = "Resistor",
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
                I = (Math.round(current * 1000.0) / 1000.0), // 3 decimal places
                R = resistance
            )

            return CircuitGraph(
                components = components,
                connections = connections,
                formula = formula
            )
        }
    }

    fun toJson(): String {
        return jsonFormat.encodeToString(this)
    }

    /**
     * Recalculates formula and component values with new V and R.
     */
    fun recalculateWith(newV: Double, newR: Double): CircuitGraph {
        val safeR = if (newR <= 0.0) 0.01 else newR
        val safeCurrent = (Math.round((newV / safeR) * 10000.0) / 10000.0)

        val updatedComponents = components.map { comp ->
            when (comp.type.lowercase()) {
                "battery" -> comp.copy(value = "${newV.toInt()}V")
                "resistor" -> comp.copy(value = "${newR.toInt()}ohm")
                else -> comp
            }
        }

        return this.copy(
            components = updatedComponents,
            formula = this.formula.copy(
                V = newV,
                I = safeCurrent,
                R = newR
            )
        )
    }

    fun getBattery(): CircuitComponent? = components.find { it.type.equals("battery", ignoreCase = true) }
    fun getResistor(): CircuitComponent? = components.find { it.type.equals("resistor", ignoreCase = true) }
}
