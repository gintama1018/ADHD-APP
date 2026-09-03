package com.circuitsense.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitsense.model.CircuitGraph
import com.circuitsense.renderer.CharacterSprite
import com.circuitsense.renderer.ComponentAnimationRegistry
import com.circuitsense.renderer.SparkyExpression
import com.circuitsense.ui.components.JsonInspectorSheet
import com.circuitsense.ui.theme.*

enum class OverviewTab(val title: String, val icon: String) {
    OVERVIEW("Overview", "🔄"),
    BATTERY("Battery", "⚡"),
    WIRE("Wire", "🔵"),
    RESISTOR("Resistor", "🔥")
}

/**
 * Screen 2 — Detection / Overview Screen (per DESIGN.md).
 * Displays the newly recognized circuit in a friendly cartoon layout,
 * verification banner (green on detection, yellow on fallback), Ohm's law badge,
 * interactive tab highlights, and a primary CTA to launch the motion tutor.
 */
@Composable
fun DetectionOverviewScreen(
    graph: CircuitGraph,
    isFallback: Boolean = false,
    fallbackReason: String? = null,
    onStartTutor: () -> Unit,
    onRescanClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(OverviewTab.OVERVIEW) }
    var showJsonSheet by remember { mutableStateOf(false) }

    val v = graph.formula.V.toInt()
    val r = graph.formula.R.toInt()
    val i = graph.formula.I

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                            text = "Circuit Overview",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
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
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        text = "Start Motion Tutor",
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
                    .padding(vertical = 8.dp)
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
                            text = if (isFallback) "Reference Circuit Active" else "Circuit Detected Successfully",
                            color = if (isFallback) WarningYellow else SuccessGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFallback) {
                                fallbackReason ?: "Couldn't read values clearly — showing reference circuit (${v}V, ${r}Ω)"
                            } else {
                                "Recognized ${v}V battery & ${r}Ω resistor (Current I = ${i}A)"
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
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricPill(label = "VOLTAGE (V)", value = "${v}V", color = ElectricBlue)
                    Text("÷", color = TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    MetricPill(label = "RESISTANCE (R)", value = "${r}Ω", color = WarmAmber)
                    Text("=", color = TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    MetricPill(label = "CURRENT (I)", value = "${i}A", color = SuccessGreen)
                }
            }

            // 3. Tab Navigation
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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

            // 4. Cartoon Circuit Diagram Canvas (Round, friendly, not technical)
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

                    // Tab Explanatory Tooltip Bubble
                    val tabTip = when (selectedTab) {
                        OverviewTab.OVERVIEW -> "Closed single loop: Voltage pushes electrons through resistance."
                        OverviewTab.BATTERY -> "⚡ Battery: Chemical separation creates potential difference (${v}V)."
                        OverviewTab.WIRE -> "🔵 Conductor: Metallic copper wire offering free electron path."
                        OverviewTab.RESISTOR -> "🔥 Resistor: Lattice friction opposes flow (${r}Ω), creating heat."
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xD90D1321),
                        border = BorderStroke(1.dp, Color(0x332EC5FF)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = tabTip,
                            color = TextPrimary,
                            fontSize = 12.sp,
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

        val bX = w * 0.25f
        val bY = h * 0.50f
        val rX = w * 0.75f
        val rY = h * 0.50f

        val topY = h * 0.28f
        val bottomY = h * 0.72f

        // 1. Gently curved cartoon wire path
        val wirePath = Path().apply {
            moveTo(bX, bY - 30f)
            quadraticBezierTo(bX, topY, (bX + rX) / 2f, topY)
            quadraticBezierTo(rX, topY, rX, rY - 30f)

            moveTo(rX, rY + 30f)
            quadraticBezierTo(rX, bottomY, (bX + rX) / 2f, bottomY)
            quadraticBezierTo(bX, bottomY, bX, bY + 30f)
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

        // 2. Render Battery & Resistor using generic registry
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

        // 3. Static friendly Sparky waiting at the battery
        val sparkyPos = when (selectedTab) {
            OverviewTab.BATTERY -> Offset(bX, topY)
            OverviewTab.WIRE -> Offset((bX + rX) / 2f, topY)
            OverviewTab.RESISTOR -> Offset(rX, topY)
            OverviewTab.OVERVIEW -> Offset(bX, topY)
        }

        CharacterSprite.draw(
            drawScope = this,
            position = sparkyPos,
            motionProgress = 0.5f,
            expression = if (selectedTab == OverviewTab.BATTERY) SparkyExpression.EXCITED else SparkyExpression.CALM,
            speedFactor = 1.0f
        )
    }
}
