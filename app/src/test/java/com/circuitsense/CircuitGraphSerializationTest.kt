package com.circuitsense

import com.circuitsense.model.CircuitGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CircuitGraphSerializationTest {

    @Test
    fun testParseStandardHackathonSchemaJson() {
        val json = """
        {
          "components": [
            { "id": "battery1", "type": "battery", "value": "9V", "x": 50.0, "y": 200.0 },
            { "id": "resistor1", "type": "resistor", "value": "100ohm", "x": 300.0, "y": 200.0 }
          ],
          "connections": [
            { "from": "battery1", "to": "resistor1", "path": "top_wire" },
            { "from": "resistor1", "to": "battery1", "path": "bottom_wire" }
          ],
          "formula": { "type": "ohms_law", "V": 9.0, "I": 0.09, "R": 100.0 }
        }
        """.trimIndent()

        val graph = CircuitGraph.fromJson(json)

        assertEquals(2, graph.components.size)
        assertEquals("battery", graph.components[0].type)
        assertEquals("9V", graph.components[0].value)
        assertEquals("resistor", graph.components[1].type)
        assertEquals("100ohm", graph.components[1].value)

        assertEquals(2, graph.connections.size)
        assertEquals("top_wire", graph.connections[0].path)
        assertEquals("bottom_wire", graph.connections[1].path)

        assertEquals(9.0, graph.formula.V, 0.001)
        assertEquals(100.0, graph.formula.R, 0.001)
        assertEquals(0.09, graph.formula.I, 0.001)
    }

    @Test
    fun testBidirectionalSerialization() {
        val originalGraph = CircuitGraph.createOhmsLawGraph(
            voltage = 12.0,
            resistance = 240.0
        )

        val jsonString = originalGraph.toJson()
        assertTrue(jsonString.contains("\"battery1\""))
        assertTrue(jsonString.contains("\"resistor1\""))
        assertTrue(jsonString.contains("\"ohms_law\""))

        val deserialized = CircuitGraph.fromJson(jsonString)
        assertEquals(originalGraph.formula.V, deserialized.formula.V, 0.001)
        assertEquals(originalGraph.formula.R, deserialized.formula.R, 0.001)
        assertEquals(originalGraph.formula.I, deserialized.formula.I, 0.001)
    }
}
