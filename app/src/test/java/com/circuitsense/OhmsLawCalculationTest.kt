package com.circuitsense

import com.circuitsense.model.CircuitGraph
import org.junit.Assert.assertEquals
import org.junit.Test

class OhmsLawCalculationTest {

    @Test
    fun testCurrentCalculationStandard() {
        val graph = CircuitGraph.createOhmsLawGraph(voltage = 9.0, resistance = 100.0)
        assertEquals(0.09, graph.formula.I, 0.001)
    }

    @Test
    fun testDynamicRecalculation() {
        val graph = CircuitGraph.createOhmsLawGraph(voltage = 10.0, resistance = 50.0)
        assertEquals(0.20, graph.formula.I, 0.001)

        // Change voltage to 20V and resistance to 100 ohms -> I should still be 0.20
        val updated = graph.recalculateWith(newV = 20.0, newR = 100.0)
        assertEquals(20.0, updated.formula.V, 0.001)
        assertEquals(100.0, updated.formula.R, 0.001)
        assertEquals(0.20, updated.formula.I, 0.001)

        // Change to 12V and 24 ohms -> I = 0.5A
        val fastFlow = updated.recalculateWith(newV = 12.0, newR = 24.0)
        assertEquals(0.5, fastFlow.formula.I, 0.001)
    }

    @Test
    fun testZeroResistanceGuard() {
        val graph = CircuitGraph.createOhmsLawGraph(voltage = 10.0, resistance = 0.0)
        // Should protect against division by zero
        assertEquals(1000.0, graph.formula.I, 0.1)
    }
}
