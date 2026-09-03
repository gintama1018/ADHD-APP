package com.circuitsense

import com.circuitsense.model.CircuitGraph
import com.circuitsense.renderer.CameraDirector
import com.circuitsense.renderer.StoryPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraDirectorTest {

    @Test
    fun testPhaseCameraTransforms() {
        val graph = CircuitGraph.createOhmsLawGraph()
        val director = CameraDirector(graph, 600f, 400f)

        val overview = director.getTargetTransform(StoryPhase.OVERVIEW)
        assertEquals(1.0f, overview.scale, 0.01f)
        assertEquals(0f, overview.panX, 0.01f)
        assertEquals(0f, overview.panY, 0.01f)

        val batteryZoom = director.getTargetTransform(StoryPhase.BATTERY_FOCUS)
        assertEquals(2.5f, batteryZoom.scale, 0.01f)

        val resistorZoom = director.getTargetTransform(StoryPhase.RESISTOR_FOCUS)
        assertEquals(2.6f, resistorZoom.scale, 0.01f)

        val fullLoop = director.getTargetTransform(StoryPhase.FULL_LOOP)
        assertEquals(1.0f, fullLoop.scale, 0.01f)
    }

    @Test
    fun testLerpTransform() {
        val graph = CircuitGraph.createOhmsLawGraph()
        val director = CameraDirector(graph, 600f, 400f)

        val start = director.getTargetTransform(StoryPhase.OVERVIEW)
        val end = director.getTargetTransform(StoryPhase.BATTERY_FOCUS)

        val mid = director.lerpTransform(start, end, 0.5f)
        assertTrue(mid.scale > 1.0f && mid.scale < 2.5f)
    }
}
