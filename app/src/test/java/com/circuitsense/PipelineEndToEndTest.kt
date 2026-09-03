package com.circuitsense

import com.circuitsense.data.SampleCircuits
import com.circuitsense.model.CircuitGraph
import com.circuitsense.renderer.CameraDirector
import com.circuitsense.renderer.ComponentAnimationRegistry
import com.circuitsense.renderer.StoryPhase
import org.junit.Assert.*
import org.junit.Test

/**
 * End-to-end integration test verifying the full pipeline:
 * Preset / Diagram -> CircuitGraph -> JSON Serialization -> Component Registry -> Camera Director Choreography.
 */
class PipelineEndToEndTest {

    @Test
    fun testPreset1EndToEndPipeline() {
        val preset = SampleCircuits.items[0] // 9V + 100Ω
        val graph = preset.graph

        // 1. Assert graph structure
        assertEquals(9.0, graph.formula.V, 0.001)
        assertEquals(100.0, graph.formula.R, 0.001)
        assertEquals(0.09, graph.formula.I, 0.001)
        assertEquals(2, graph.components.size)
        assertEquals(2, graph.connections.size)

        // 2. Assert JSON round-trip
        val json = graph.toJson()
        assertTrue(json.contains("\"ohms_law\""))
        val reloaded = CircuitGraph.fromJson(json)
        assertEquals(graph.formula.I, reloaded.formula.I, 0.001)

        // 3. Assert Component Registry resolution
        graph.components.forEach { comp ->
            val renderer = ComponentAnimationRegistry.getRenderer(comp.type)
            assertNotNull(renderer)
            val beat = renderer.createIntroBeat(comp)
            assertNotNull(beat)
            assertTrue(beat.cameraZoom in 1.5f..4.0f)
        }

        // 4. Assert Camera Director choreography across all 5 story beats
        val director = CameraDirector(graph, 600f, 400f)
        for (phase in StoryPhase.values()) {
            val transform = director.getTargetTransform(phase)
            assertFalse(transform.scale.isNaN())
            assertFalse(transform.panX.isNaN())
            assertFalse(transform.panY.isNaN())
            assertTrue(transform.scale in 0.5f..5.0f)
        }
    }

    @Test
    fun testPreset2EndToEndPipeline() {
        val preset = SampleCircuits.items[1] // 24V + 60Ω -> 0.40A
        val graph = preset.graph

        assertEquals(24.0, graph.formula.V, 0.001)
        assertEquals(60.0, graph.formula.R, 0.001)
        assertEquals(0.40, graph.formula.I, 0.001)

        // Test dynamic parameter tweaking on this graph
        val updated = graph.recalculateWith(newV = 36.0, newR = 72.0)
        assertEquals(0.50, updated.formula.I, 0.001)

        val director = CameraDirector(updated, 600f, 400f)
        val fullLoopTransform = director.getTargetTransform(StoryPhase.FULL_LOOP)
        assertEquals(1.0f, fullLoopTransform.scale, 0.01f)
    }

    @Test
    fun testDynamicRecalculationMaintainsSchemaIntegrity() {
        val graph = CircuitGraph.createOhmsLawGraph(12.0, 300.0)
        assertEquals(0.04, graph.formula.I, 0.001)

        val modified = graph.recalculateWith(15.0, 150.0)
        assertEquals(0.10, modified.formula.I, 0.001)

        val json = modified.toJson()
        val parsed = CircuitGraph.fromJson(json)
        assertEquals(0.10, parsed.formula.I, 0.001)
    }
}
