package com.circuitsense

import com.circuitsense.model.CircuitComponent
import com.circuitsense.renderer.BatteryComponentRenderer
import com.circuitsense.renderer.ComponentAnimationRegistry
import com.circuitsense.renderer.GenericBoxRenderer
import com.circuitsense.renderer.ResistorComponentRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentRegistryTest {

    @Test
    fun testDefaultComponentsRegistered() {
        val batteryRenderer = ComponentAnimationRegistry.getRenderer("battery")
        assertTrue(batteryRenderer is BatteryComponentRenderer)

        val resistorRenderer = ComponentAnimationRegistry.getRenderer("resistor")
        assertTrue(resistorRenderer is ResistorComponentRenderer)
    }

    @Test
    fun testFallbackRendererForUnknownTypes() {
        val unknownRenderer = ComponentAnimationRegistry.getRenderer("transistor_super_custom")
        assertTrue(unknownRenderer is GenericBoxRenderer)
    }

    @Test
    fun testBeatGeneration() {
        val batteryComp = CircuitComponent(
            id = "b1",
            type = "battery",
            value = "9V",
            x = 100f,
            y = 200f
        )
        val renderer = ComponentAnimationRegistry.getRenderer("battery")
        val beat = renderer.createIntroBeat(batteryComp)

        assertNotNull(beat)
        assertTrue(beat.narrationText.contains("9V"))
        assertEquals(2.6f, beat.cameraZoom, 0.1f)
    }
}
