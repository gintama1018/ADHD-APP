package com.circuitsense.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitsense.data.CircuitHistoryManager
import com.circuitsense.model.CircuitGraph
import com.circuitsense.renderer.CharacterSprite
import com.circuitsense.renderer.ComponentAnimationRegistry
import com.circuitsense.renderer.SparkyExpression
import com.circuitsense.ui.components.AiVisionDetectionOverlay
import com.circuitsense.ui.components.JsonInspectorSheet
import com.circuitsense.ui.components.drawPlaygroundDotGrid
import com.circuitsense.ui.theme.*

enum class OverviewTab(val title: String, val icon: String) {
    OVERVIEW("Overview", "🔄"),
    BATTERY("Battery", "⚡"),
    WIRE("Wire", "🔵"),
    RESISTOR("Resistor", "🔥")
}

/**
 * Screen 2 — Detection / Overview Screen (per DESIGN.md & User Reference Photos).
 * Features:
 * - Dot-matrix technical playground background (Photo 4)
 * - AI Computer Vision detection bounding box HUD (Photos 1, 2, 3)
 * - Save to Study History / Gallery button
 * - Anime-style dialogue and Sparky expression reactions
 */
@Composable
fun DetectionOverviewScreen(
    graph: CircuitGraph,
    isFallback: Boolean = false,
    fallbackReason: String? = null,
    onStartTutor: () -> Unit,
    onRescanClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(OverviewTab.OVERVIEW) }
    var showJsonSheet by remember { mutableStateOf(false) }
    var showVisionBoxes by remember { mutableStateOf(true) }
    var isSaved by remember { mutableStateOf(false) }

    val v = graph.formula.V.toInt()
    val r = graph.formula.R.toInt()
    val i = graph.formula.I

    Scaffold(
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRescanClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Rescan",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "CircuitSense",
                            color = ElectricBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI Vision Detection",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Save to Study History Button
                    IconButton(
                        onClick = {
                            CircuitHistoryManager.saveCircuit(context, graph)
                            isSaved = true
                            Toast.makeText(context, "Saved to Study History!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save to History",
                            tint = if (isSaved) SuccessGreen else TextMuted
                        )
                    }

                    // Toggle AI CV Bounding Boxes (Photo 1/2/3 style)
                    IconButton(onClick = { showVisionBoxes = !showVisionBoxes }) {
                        Icon(
                            imageVector = if (showVisionBoxes) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Bounding Boxes",
                            tint = if (showVisionBoxes) ElectricBlue else TextMuted
                        )
                    }

                    // Inspect JSON quick button
                    OutlinedButton(
                        onClick = { showJsonSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary CTA button to launch animated tutor
                Button(
                    onClick = onStartTutor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = BackgroundDark
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Motion Graphics Tutor",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Detection Banner (Green on success, Warning Yellow on fallback)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isFallback) Color(0x26FACC15) else Color(0x264ADE80),
                border = BorderStroke(1.dp, if (isFallback) WarningYellow else SuccessGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFallback) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isFallback) WarningYellow else SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isFallback) "Reference Circuit Active" else "AI Vision: Circuit Recognized (3 of 3)",
                            color = if (isFallback) WarningYellow else SuccessGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFallback) {
                                fallbackReason ?: "Couldn't read values clearly — showing reference circuit (${v}V, ${r}Ω)"
                            } else {
                                "Bounding boxes generated for Battery (${v}V), Resistor (${r}Ω) & Loop (I = ${i}A)"
                            },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 2. Ohm's Law Badge Card (Monospace tabular figures)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardDark,
                border = BorderStroke(1.dp, CardElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricPill(label = "VOLTAGE (V)", value = "${v}V", color = ElectricBlue)
                    Text("÷", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    MetricPill(label = "RESISTANCE (R)", value = "${r}Ω", color = WarmAmber)
                    Text("=", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    MetricPill(label = "CURRENT (I)", value = "${i}A", color = SuccessGreen)
                }
            }

            // 3. Tab Navigation
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(OverviewTab.values()) { tab ->
                    val isSelected = tab == selectedTab
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ElectricBlue else CardDark,
                        modifier = Modifier.clickable { selectedTab = tab }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tab.icon, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) BackgroundDark else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 4. Dot-Matrix Technical Playground Canvas (Photo 4) with Vision Bounding Boxes (Photos 1/2/3)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CardDark,
                border = BorderStroke(1.dp, CardElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CartoonDiagramCanvas(
                        graph = graph,
                        selectedTab = selectedTab,
                        modifier = Modifier.fillMaxSize()
                    )

                    // AI Vision Detection Overlay (Bounding boxes, confidence scores)
                    if (showVisionBoxes) {
                        AiVisionDetectionOverlay(
                            graph = graph,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Anime Story Callout Bubble from Sparky
                    val animeQuote = when (selectedTab) {
                        OverviewTab.OVERVIEW -> "⚡ Sparky: \"V = ${v}V is pushing me against ${r}Ω resistance! Let's animate this!\""
                        OverviewTab.BATTERY -> "⚡ Sparky: \"WHOA! Chemical energy is pumping me up to ${v} Volts!\""
                        OverviewTab.WIRE -> "✨ Sparky: \"Cruising smoothly through copper at ${i} Amps! Wheee!\""
                        OverviewTab.RESISTOR -> "🔥 Sparky: \"OUCH! ${r} Ohms of vibrating lattice atoms! Getting squished!\""
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xF20D1321),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = animeQuote,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showJsonSheet) {
        JsonInspectorSheet(
            graph = graph,
            onDismiss = { showJsonSheet = false }
        )
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CartoonDiagramCanvas(
    graph: CircuitGraph,
    selectedTab: OverviewTab,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Photo 4: Dot-Matrix Technical Playground Background
        drawPlaygroundDotGrid(w, h, step = 28f, dotRadius = 1.6f, dotColor = Color(0xFF282B36))

        val bX = w * 0.25f
        val bY = h * 0.50f
        val rX = w * 0.75f
        val rY = h * 0.50f

        val topY = h * 0.28f
        val bottomY = h * 0.72f

        // 2. Gently curved cartoon wire path connecting directly to component leads
        val wirePath = Path().apply {
            moveTo(bX, bY - 35f)
            quadraticBezierTo(bX, topY, (bX + rX) / 2f, topY)
            quadraticBezierTo(rX, topY, rX, rY - 35f)

            moveTo(rX, rY + 35f)
            quadraticBezierTo(rX, bottomY, (bX + rX) / 2f, bottomY)
            quadraticBezierTo(bX, bottomY, bX, bY + 35f)
        }

        // Wire glow & core
        drawPath(
            path = wirePath,
            color = if (selectedTab == OverviewTab.WIRE) ElectricBlue.copy(alpha = 0.6f) else WireGlow,
            style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = wirePath,
            color = if (selectedTab == OverviewTab.WIRE) ElectricBlue else WireConductor,
            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3. Render Battery & Resistor using generic registry
        graph.components.forEach { comp ->
            val isBattery = comp.type.equals("battery", ignoreCase = true)
            val isResistor = comp.type.equals("resistor", ignoreCase = true)

            val compX = if (isBattery) bX else rX
            val compY = if (isBattery) bY else rY

            val isFocused = when (selectedTab) {
                OverviewTab.BATTERY -> isBattery
                OverviewTab.RESISTOR -> isResistor
                else -> false
            }

            val adjusted = comp.copy(x = compX, y = compY)
            val renderer = ComponentAnimationRegistry.getRenderer(comp.type)
            renderer.draw(
                drawScope = this,
                component = adjusted,
                animationProgress = 0.5f,
                isFocused = isFocused,
                currentAmps = graph.formula.I
            )
        }

        // 4. Sparky with Anime Expression
        val sparkyPos = when (selectedTab) {
            OverviewTab.BATTERY -> Offset(bX, bY)
            OverviewTab.WIRE -> Offset((bX + rX) / 2f, topY)
            OverviewTab.RESISTOR -> Offset(rX, rY)
            OverviewTab.OVERVIEW -> Offset(bX, bY)
        }

        val expr = when (selectedTab) {
            OverviewTab.BATTERY -> SparkyExpression.EXCITED
            OverviewTab.RESISTOR -> SparkyExpression.SQUISHED
            else -> SparkyExpression.CALM
        }

        CharacterSprite.draw(
            drawScope = this,
            position = sparkyPos,
            motionProgress = 0.5f,
            expression = expr,
            speedFactor = 1.0f
        )
    }
}
