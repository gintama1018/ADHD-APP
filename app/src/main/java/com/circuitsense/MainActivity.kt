package com.circuitsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.circuitsense.data.SampleCircuits
import com.circuitsense.model.CircuitGraph
import com.circuitsense.ui.screens.CameraCaptureScreen
import com.circuitsense.ui.screens.TutorPlaybackScreen
import com.circuitsense.ui.theme.CircuitSenseTheme

sealed class AppScreen {
    data object Camera : AppScreen()
    data class Tutor(
        val graph: CircuitGraph,
        val isFallback: Boolean = false,
        val fallbackReason: String? = null
    ) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CircuitSenseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F111A)
                ) {
                    var currentScreen by remember {
                        mutableStateOf<AppScreen>(AppScreen.Camera)
                    }

                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is AppScreen.Camera -> {
                                CameraCaptureScreen(
                                    onCircuitReady = { recognizedGraph, isFallback, fallbackReason ->
                                        currentScreen = AppScreen.Tutor(recognizedGraph, isFallback, fallbackReason)
                                    }
                                )
                            }
                            is AppScreen.Tutor -> {
                                TutorPlaybackScreen(
                                    initialGraph = screen.graph,
                                    isFallback = screen.isFallback,
                                    fallbackReason = screen.fallbackReason,
                                    onRescanClick = {
                                        currentScreen = AppScreen.Camera
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
