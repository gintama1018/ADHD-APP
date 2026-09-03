package com.circuitsense.data

import com.circuitsense.model.CircuitGraph

/**
 * Curated textbook circuit diagrams and corresponding validated CircuitGraph JSONs.
 * Ensures foolproof live presentation recording even if camera lighting or angle is imperfect.
 */
data class SampleCircuitItem(
    val id: String,
    val title: String,
    val description: String,
    val voltage: Double,
    val resistance: Double,
    val graph: CircuitGraph
)

object SampleCircuits {
    val items = listOf(
        SampleCircuitItem(
            id = "sample_standard_9v_100ohm",
            title = "Standard 9V + 100Ω",
            description = "Textbook Ohm's law with 9V DC battery and 100Ω carbon resistor.",
            voltage = 9.0,
            resistance = 100.0,
            graph = CircuitGraph.createOhmsLawGraph(voltage = 9.0, resistance = 100.0)
        ),
        SampleCircuitItem(
            id = "sample_high_voltage_24v_60ohm",
            title = "High Potential: 24V + 60Ω",
            description = "High electric potential driving rapid electron flow (I = 0.40A).",
            voltage = 24.0,
            resistance = 60.0,
            graph = CircuitGraph.createOhmsLawGraph(voltage = 24.0, resistance = 60.0)
        ),
        SampleCircuitItem(
            id = "sample_high_resistance_12v_300ohm",
            title = "High Resistance: 12V + 300Ω",
            description = "Heavy electron scattering in the resistor slowing down flow (I = 0.04A).",
            voltage = 12.0,
            resistance = 300.0,
            graph = CircuitGraph.createOhmsLawGraph(voltage = 12.0, resistance = 300.0)
        ),
        SampleCircuitItem(
            id = "sample_low_voltage_3v_15ohm",
            title = "Low Voltage: 3V + 15Ω",
            description = "Low voltage battery with small resistance (I = 0.20A).",
            voltage = 3.0,
            resistance = 15.0,
            graph = CircuitGraph.createOhmsLawGraph(voltage = 3.0, resistance = 15.0)
        )
    )

    val defaultSample: SampleCircuitItem get() = items[0]
}
