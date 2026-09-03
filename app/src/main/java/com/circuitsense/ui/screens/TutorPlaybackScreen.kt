package com.circuitsense.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.circuitsense.audio.NarrationManager
import com.circuitsense.data.CircuitHistoryManager
import com.circuitsense.model.CircuitGraph
import com.circuitsense.qa.AskDoubtBottomSheet
import com.circuitsense.renderer.CameraDirector
import com.circuitsense.renderer.CameraTransform
import com.circuitsense.renderer.StoryPhase
import com.circuitsense.ui.canvas.CircuitMotionCanvas
import com.circuitsense.ui.components.JsonInspectorSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Main Interactive Tutor Playback Screen.
 * Demonstrates:
 * 1. Generic JSON-driven Canvas renderer.
 * 2. 4-step cinematic camera choreography.
 * 3. TextToSpeech narration + high-contrast subtitles for ADHD focus.
 * 4. Real-time Ohm's Law physics parameters sliders (V, R -> I) dynamically altering animation speed.
 * 5. Debug JSON inspector bottom sheet proving live graph execution.
 * 6. Offline "Ask a Doubt" physics assistant.
 */
@Composable
fun TutorPlaybackScreen(
    initialGraph: CircuitGraph,
    isFallback: Boolean = false,
    fallbackReason: String? = null,
    onRescanClick: () -> Unit
) {
    val context = LocalContext.current
    var activeGraph by remember { mutableStateOf(initialGraph) }
    var showFallbackBanner by remember(isFallback) { mutableStateOf(isFallback) }
    var currentPhase by remember { mutableStateOf(StoryPhase.OVERVIEW) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }

    var showJsonSheet by remember { mutableStateOf(false) }
    var showDoubtSheet by remember { mutableStateOf(false) }

    val narrationManager = remember {
        NarrationManager(context).apply {
            this.isMuted = isMuted
        }
    }
    DisposableEffect(narrationManager) {
        onDispose {
            narrationManager.release()
        }
    }

    val cameraDirector = remember(activeGraph) { CameraDirector(activeGraph) }
    var cameraTransform by remember { mutableStateOf(CameraTransform(1.0f, 0f, 0f)) }
    var animationProgress by remember { mutableStateOf(0f) }

    // Synchronize narration on phase changes
    LaunchedEffect(currentPhase, activeGraph) {
        narrationManager.isMuted = isMuted
        narrationManager.narratePhase(currentPhase, activeGraph)
    }

    // Main animation & camera choreography loop
    LaunchedEffect(currentPhase, isPlaying, playbackSpeed, activeGraph) {
        if (!isPlaying) return@LaunchedEffect

        val targetTransform = cameraDirector.getTargetTransform(currentPhase)
        val startTransform = cameraTransform
        var elapsedMs = 0L
        val phaseDurationMs = when (currentPhase) {
            StoryPhase.OVERVIEW -> 4000L
            StoryPhase.BATTERY_FOCUS -> 4500L
            StoryPhase.WIRE_TRANSIT -> 4000L
            StoryPhase.RESISTOR_FOCUS -> 4800L
            StoryPhase.FULL_LOOP -> 12000L
        }

        val effectiveDuration = (phaseDurationMs / playbackSpeed).toLong()

        while (isActive && isPlaying) {
            val fraction = (elapsedMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)
            animationProgress = fraction
            cameraTransform = cameraDirector.lerpTransform(startTransform, targetTransform, fraction)

            delay(16) // ~60 FPS
            elapsedMs += 16

            if (elapsedMs >= effectiveDuration) {
                // Auto advance to next phase in sequence
                currentPhase = when (currentPhase) {
                    StoryPhase.OVERVIEW -> StoryPhase.BATTERY_FOCUS
                    StoryPhase.BATTERY_FOCUS -> StoryPhase.WIRE_TRANSIT
                    StoryPhase.WIRE_TRANSIT -> StoryPhase.RESISTOR_FOCUS
                    StoryPhase.RESISTOR_FOCUS -> StoryPhase.FULL_LOOP
                    StoryPhase.FULL_LOOP -> StoryPhase.OVERVIEW
                }
                break
            }
        }
    }

    val subtitleText by narrationManager.currentSubtitle.collectAsState()

    Scaffold(
        containerColor = Color(0xFF0F111A),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TutorTopBar(
                currentPhase = currentPhase,
                onRescanClick = onRescanClick,
                onInspectJsonClick = { showJsonSheet = true },
                onAskDoubtClick = { showDoubtSheet = true },
                onSaveHistoryClick = {
                    CircuitHistoryManager.saveCircuit(context, activeGraph)
                    Toast.makeText(context, "Saved to Study History!", Toast.LENGTH_SHORT).show()
                }
            )
        },
        bottomBar = {
            TutorBottomControls(
                graph = activeGraph,
                currentPhase = currentPhase,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                isMuted = isMuted,
                subtitleText = subtitleText,
                onPlayPauseToggle = { isPlaying = !isPlaying },
                onStepSelect = { selectedPhase ->
                    currentPhase = selectedPhase
                    animationProgress = 0f
                },
                onNextStep = {
                    currentPhase = when (currentPhase) {
                        StoryPhase.OVERVIEW -> StoryPhase.BATTERY_FOCUS
                        StoryPhase.BATTERY_FOCUS -> StoryPhase.WIRE_TRANSIT
                        StoryPhase.WIRE_TRANSIT -> StoryPhase.RESISTOR_FOCUS
                        StoryPhase.RESISTOR_FOCUS -> StoryPhase.FULL_LOOP
                        StoryPhase.FULL_LOOP -> StoryPhase.OVERVIEW
                    }
                    animationProgress = 0f
                },
                onPrevStep = {
                    currentPhase = when (currentPhase) {
                        StoryPhase.FULL_LOOP -> StoryPhase.RESISTOR_FOCUS
                        StoryPhase.RESISTOR_FOCUS -> StoryPhase.WIRE_TRANSIT
                        StoryPhase.WIRE_TRANSIT -> StoryPhase.BATTERY_FOCUS
                        StoryPhase.BATTERY_FOCUS -> StoryPhase.OVERVIEW
                        StoryPhase.OVERVIEW -> StoryPhase.FULL_LOOP
                    }
                    animationProgress = 0f
                },
                onSpeedToggle = {
                    playbackSpeed = when (playbackSpeed) {
                        1.0f -> 1.5f
                        1.5f -> 2.0f
                        2.0f -> 0.75f
                        else -> 1.0f
                    }
                },
                onMuteToggle = {
                    isMuted = !isMuted
                    narrationManager.isMuted = isMuted
                    if (isMuted) narrationManager.stop()
                },
                onParamsChanged = { newV, newR ->
                    activeGraph = activeGraph.recalculateWith(newV, newR)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Motion Canvas
            CircuitMotionCanvas(
                graph = activeGraph,
                currentPhase = currentPhase,
                animationProgress = animationProgress,
                cameraTransform = cameraTransform,
                modifier = Modifier.fillMaxSize()
            )

            // Live Formula HUD Badge
            FormulaOverlayBadge(
                graph = activeGraph,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // Explicit Fallback Alert Banner (Transparency: informs user if OCR defaulted)
            if (showFallbackBanner && fallbackReason != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xF2241A0B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFAB00)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(0.70f)
                        .padding(start = 14.dp, top = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Notice",
                            tint = Color(0xFFFFAB00),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fallbackReason,
                            color = Color(0xFFFFE082),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showFallbackBanner = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFFFAB00),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets
    if (showJsonSheet) {
        JsonInspectorSheet(
            graph = activeGraph,
            onDismiss = { showJsonSheet = false }
        )
    }

    if (showDoubtSheet) {
        AskDoubtBottomSheet(
            graph = activeGraph,
            onDismiss = { showDoubtSheet = false }
        )
    }
}

@Composable
private fun TutorTopBar(
    currentPhase: StoryPhase,
    onRescanClick: () -> Unit,
    onInspectJsonClick: () -> Unit,
    onAskDoubtClick: () -> Unit,
    onSaveHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onRescanClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Rescan",
                tint = Color.White
            )
        }

        // Current phase pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E2433),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF00E5FF), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentPhase.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Buttons: Save, JSON Inspector & Ask a Doubt
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = onSaveHistoryClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF14241B),
                    contentColor = Color(0xFF4ADE80)
                ),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Save to History",
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            FilledTonalIconButton(
                onClick = onInspectJsonClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF1C2230),
                    contentColor = Color(0xFF00E5FF)
                ),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DataObject,
                    contentDescription = "Inspect JSON",
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            FilledTonalIconButton(
                onClick = onAskDoubtClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF261D10),
                    contentColor = Color(0xFFFFAB00)
                ),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Ask a Doubt",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FormulaOverlayBadge(
    graph: CircuitGraph,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xE6141824),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Ohm's Law: I = V / R",
                color = Color(0xFF90A4AE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "I = ",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${graph.formula.I} A",
                    color = Color(0xFF00E5FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = "V: ${graph.formula.V.toInt()}V | R: ${graph.formula.R.toInt()}Ω",
                color = Color(0xFFFFAB00),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TutorBottomControls(
    graph: CircuitGraph,
    currentPhase: StoryPhase,
    isPlaying: Boolean,
    playbackSpeed: Float,
    isMuted: Boolean,
    subtitleText: String,
    onPlayPauseToggle: () -> Unit,
    onStepSelect: (StoryPhase) -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onSpeedToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onParamsChanged: (Double, Double) -> Unit
) {
    var showSliders by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF00D1017))
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ADHD Subtitles Banner (High contrast, focal anchoring)
        if (subtitleText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF181F2E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitleText,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Story Phase Stepper Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(StoryPhase.values()) { phase ->
                val isSelected = phase == currentPhase
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2433),
                    modifier = Modifier.clickable { onStepSelect(phase) }
                ) {
                    Text(
                        text = when (phase) {
                            StoryPhase.OVERVIEW -> "1. Overview"
                            StoryPhase.BATTERY_FOCUS -> "2. Battery ⚡"
                            StoryPhase.WIRE_TRANSIT -> "3. Wire ➡️"
                            StoryPhase.RESISTOR_FOCUS -> "4. Resistor 🔥"
                            StoryPhase.FULL_LOOP -> "5. Loop 🔄"
                        },
                        color = if (isSelected) Color(0xFF0D1017) else Color(0xFFB0BEC5),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Physics Parameter Tweak Slider Drawer (Toggleable)
        AnimatedVisibility(visible = showSliders) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131722), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Live Ohm's Law Experimentation (Real-Time Recalculation)",
                    color = Color(0xFFFFAB00),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Voltage Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("V: ${graph.formula.V.toInt()}V", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                    Slider(
                        value = graph.formula.V.toFloat(),
                        onValueChange = { onParamsChanged(it.toDouble(), graph.formula.R) },
                        valueRange = 1f..36f,
                        steps = 35,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )
                }

                // Resistance Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R: ${graph.formula.R.toInt()}Ω", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                    Slider(
                        value = graph.formula.R.toFloat(),
                        onValueChange = { onParamsChanged(graph.formula.V, it.toDouble()) },
                        valueRange = 10f..400f,
                        steps = 39,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF9100), activeTrackColor = Color(0xFFFF9100))
                    )
                }
            }
        }

        // Playback Controller Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed toggle
            TextButton(onClick = onSpeedToggle) {
                Text("${playbackSpeed}x", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Step Back
            IconButton(onClick = onPrevStep) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
            }

            // Play / Pause FAB
            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF00E5FF), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFF0D1017),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Step Forward
            IconButton(onClick = onNextStep) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
            }

            // Audio Mute / Unmute
            IconButton(onClick = onMuteToggle) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute Toggle",
                    tint = if (isMuted) Color(0xFFFF5252) else Color(0xFF00E5FF)
                )
            }

            // Toggle Slider Controls
            IconButton(onClick = { showSliders = !showSliders }) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Tune Parameters",
                    tint = if (showSliders) Color(0xFFFFAB00) else Color(0xFF90A4AE)
                )
            }
        }
    }
}
