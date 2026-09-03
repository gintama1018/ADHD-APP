package com.circuitsense.qa

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitsense.model.CircuitGraph
import com.circuitsense.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val modelBadge: String = "AI",
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender { USER, BOT }

/**
 * High-Precision Physics Intelligence Engine.
 * Formulates detailed pedagogical explanations with real math derivations ($P = I^2 R$,
 * electron count, drift velocity, thermal lattice scattering) when running on-device.
 */
object OfflinePhysicsBot {

    fun answerDoubt(question: String, graph: CircuitGraph): String {
        val q = question.lowercase()
        val v = graph.formula.V
        val r = graph.formula.R
        val i = graph.formula.I
        val power = v * i
        val electronsPerSec = i / 1.602e-19

        return when {
            q.contains("why") && (q.contains("struggle") || q.contains("squish") || q.contains("resistor")) -> {
                """
                🔥 Why Sparky Squishes in the Resistor:
                
                • Inside the ${r.toInt()}Ω resistor, conducting electrons encounter atomic lattice obstacles.
                • In the copper wire, free electrons drift almost freely. But the resistor is made of high-resistivity material (carbon/nichrome).
                • Electrons continuously collide with vibrating lattice ions, converting kinetic energy into heat (Joule Heating: P = I²R = ${String.format("%.3f", power)} Watts).
                • That's why Sparky looks squished and strained—he's literally getting bounced around by thermal vibrations!
                """.trimIndent()
            }
            q.contains("voltage") || q.contains("battery") || q.contains("potential") -> {
                """
                ⚡ The ${v.toInt()}V Battery's Role:
                
                • Think of the battery as an electric pump. Chemical reactions separate positive and negative charges, creating a potential difference of ${v.toInt()} Volts.
                • 1 Volt = 1 Joule of energy per Coulomb of charge (V = W / Q).
                • This voltage establishes an electric field through the wire that exerts an electrostatic force (F = qE) pushing electrons around the closed loop.
                """.trimIndent()
            }
            q.contains("calculate") || q.contains("current") || q.contains("formula") || q.contains("how much") -> {
                """
                📐 Live Ohm's Law Calculation:
                
                • Formula: I = V / R
                • Voltage (V) = ${v.toInt()} V
                • Resistance (R) = ${r.toInt()} Ω
                • Current (I) = ${v.toInt()} ÷ ${r.toInt()} = ${i} Amperes (A)
                
                🔬 Electron Flow Rate:
                ${String.format("%.2e", electronsPerSec)} electrons pass through every cross-section of the wire each second!
                """.trimIndent()
            }
            q.contains("increase") || q.contains("double") || q.contains("change") || q.contains("24v") -> {
                val newV = 24.0
                val newI = newV / r
                """
                🚀 What If You Increase Voltage to ${newV.toInt()}V?
                
                • Current is directly proportional to voltage: I ∝ V.
                • With ${newV.toInt()}V and ${r.toInt()}Ω, the new current becomes:
                  I = 24 / ${r.toInt()} = ${String.format("%.3f", newI)} A
                • Power increases quadratically (P = V²/R)! Heat rises from ${String.format("%.2f", power)}W to ${String.format("%.2f", (newV * newV) / r)}W!
                • Sparky's drift speed in the animation would speed up significantly!
                """.trimIndent()
            }
            q.contains("0 ohm") || q.contains("0ohm") || q.contains("short circuit") || q.contains("drops to 0") -> {
                """
                💥 DANGER! Short Circuit Cross-Examination:
                
                • Formula: I = V / R. If R → 0, Current I → ∞ (Infinity)!
                • In reality, current spikes to hundreds of Amperes, limited only by internal battery resistance.
                • Extreme Joule Heating (P = I²R) instantly melts wires, triggers sparks, or explodes the battery!
                • Sparky's Anime Reaction: 😱 Running for his life from extreme thermal overload!
                """.trimIndent()
            }
            q.contains("r doubles") || q.contains("double r") || (q.contains("cross") && q.contains("double")) -> {
                """
                🎯 Cross-Examination Answer:
                
                • Ohm's Law: I = V / R. Current is inversely proportional to resistance (I ∝ 1/R).
                • If R doubles from ${r.toInt()}Ω to ${(r * 2).toInt()}Ω while V stays at ${v.toInt()}V:
                  New Current = ${v.toInt()} ÷ ${(r * 2).toInt()} = ${String.format("%.3f", i / 2)} Amperes!
                • Result: Current is exactly HALVED! Sparky faces twice the atomic obstacles, slowing his pace!
                """.trimIndent()
            }
            q.contains("heat") || q.contains("power") || q.contains("watt") || q.contains("burn") -> {
                """
                ♨️ Heat & Power Dissipation:
                
                • Power = V × I = I² × R = ${String.format("%.3f", power)} Watts (Joules/sec).
                • In this circuit, ${String.format("%.3f", power)} Joules of electrical potential energy are transformed into thermal heat every single second.
                • If resistance is too low, current spikes (short circuit), causing extreme heat that can melt wires!
                """.trimIndent()
            }
            q.contains("drift") || q.contains("speed") || q.contains("fast") || q.contains("slow") -> {
                """
                🐢 Drift Velocity vs Speed of Light Paradox:
                
                • The electrical signal (electric field wave) travels through the wire at ~90% the speed of light (~270,000 km/s). That's why lights turn on instantly!
                • But the physical electrons (Sparky) actually drift agonizingly slow—only about 0.1 to 1 millimeter per second (v_d = I / n·A·e)!
                • Analogy: When you push a marble into a tube packed with marbles, one pops out the other end instantly, even though individual marbles moved only a millimeter!
                """.trimIndent()
            }
            q.contains("adhd") || q.contains("visual") || q.contains("learn") -> {
                """
                🧠 ADHD & Visual Learning Design:
                
                • Static formulas (V=IR) fail to stimulate spatial working memory.
                • CircuitSense replaces abstract symbols with a physical character narrative:
                  1. Chemical birth at battery
                  2. Friction-opposed struggle in resistor
                  3. Dynamic slider feedback
                • Real-time motion anchors attention and builds intuitive physics models before rote memorization!
                """.trimIndent()
            }
            else -> {
                """
                💡 Physics Tutor Insight for this Circuit:
                
                • Your active circuit has V = ${v.toInt()}V, R = ${r.toInt()}Ω, and Current I = ${i}A.
                • Governed strictly by Ohm's Law: I = V / R.
                • Power Dissipation: P = ${String.format("%.3f", power)} Watts.
                • Rate of charge: ${String.format("%.2e", electronsPerSec)} electrons/sec.
                
                Try asking:
                - "Why does Sparky struggle in the resistor?"
                - "What happens if I double the voltage?"
                - "Why is drift velocity so slow?"
                """.trimIndent()
            }
        }
    }
}

/**
 * Screen 5 / Doubt Assistant:
 * Combines Google Gemini 1.5 Flash (real LLM) with the on-device Neural Physics Engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskDoubtBottomSheet(
    graph: CircuitGraph,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var queryText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var userApiKeyInput by remember { mutableStateOf(GeminiApiClient.getApiKey(context)) }
    var isGeminiActive by remember { mutableStateOf(GeminiApiClient.hasApiKey(context)) }

    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "0",
                sender = MessageSender.BOT,
                text = "Hello! I am CircuitSense AI Tutor. Ask me any physics question about your active ${graph.formula.V.toInt()}V, ${graph.formula.R.toInt()}Ω circuit!",
                modelBadge = if (isGeminiActive) "Gemini 1.5 Flash" else "Physics Engine"
            )
        )
    }

    val sampleQueries = listOf(
        "Why does Sparky struggle in the resistor?",
        "What happens if I increase voltage to 24V?",
        "How much heat (Watts) does this produce?",
        "Explain drift velocity vs light speed",
        "🎯 Cross-Question: If R doubles, what happens to I?",
        "🎯 Cross-Question: What if resistance drops to 0Ω?"
    )

    fun sendQuestion(question: String) {
        if (question.isBlank() || isLoading) return
        val userQ = question.trim()
        queryText = ""

        messages.add(
            ChatMessage(
                id = System.currentTimeMillis().toString(),
                sender = MessageSender.USER,
                text = userQ
            )
        )

        coroutineScope.launch {
            isLoading = true
            val hasKey = GeminiApiClient.hasApiKey(context)

            if (hasKey) {
                // Real Google Gemini 1.5 Flash API call
                val geminiResult = GeminiApiClient.queryGemini(userQ, graph, context)
                if (geminiResult.isSuccess) {
                    messages.add(
                        ChatMessage(
                            id = (System.currentTimeMillis() + 1).toString(),
                            sender = MessageSender.BOT,
                            text = geminiResult.getOrThrow(),
                            modelBadge = "Gemini 1.5 Flash"
                        )
                    )
                } else {
                    // Fallback to local physics engine with notice
                    val fallback = OfflinePhysicsBot.answerDoubt(userQ, graph)
                    messages.add(
                        ChatMessage(
                            id = (System.currentTimeMillis() + 1).toString(),
                            sender = MessageSender.BOT,
                            text = "$fallback\n\n(Note: Gemini API returned: ${geminiResult.exceptionOrNull()?.message ?: "error"}. Showing on-device answer.)",
                            modelBadge = "Physics Engine"
                        )
                    )
                }
            } else {
                // High-precision on-device physics engine
                val answer = OfflinePhysicsBot.answerDoubt(userQ, graph)
                messages.add(
                    ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        sender = MessageSender.BOT,
                        text = answer,
                        modelBadge = "Physics Engine"
                    )
                )
            }
            isLoading = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Speech-to-Text Voice Recognition Launcher (User Voice Questions)
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                sendQuestion(spokenText)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header Bar with AI Mode Badge & Settings Gear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Physics Tutor",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isGeminiActive) SuccessGreen else WarmAmber, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGeminiActive) "Google Gemini 1.5 Flash (Live AI)" else "On-Device Physics Intelligence",
                                color = if (isGeminiActive) SuccessGreen else WarmAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // API Key Settings Button
                OutlinedButton(
                    onClick = { showApiKeyDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isGeminiActive) SuccessGreen.copy(alpha = 0.5f) else ElectricBlue.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Key",
                        tint = if (isGeminiActive) SuccessGreen else ElectricBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isGeminiActive) "Gemini Key ✓" else "+ Add Gemini Key",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGeminiActive) SuccessGreen else ElectricBlue
                    )
                }
            }

            // Quick suggestion chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                items(sampleQueries) { sampleQ ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = BackgroundDark,
                        border = BorderStroke(1.dp, CardElevated),
                        modifier = Modifier.clickable { sendQuestion(sampleQ) }
                    ) {
                        Text(
                            text = sampleQ,
                            fontSize = 11.sp,
                            color = ElectricBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }
                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ElectricBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGeminiActive) "Gemini 1.5 Flash is thinking..." else "Calculating physics model...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            text = "Ask any doubt about V, I, R, drift, heat...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Voice Speech-to-Text Microphone Button
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask any question about this circuit...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice input service unavailable on this device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(CardElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak Question",
                        tint = ElectricBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { sendQuestion(queryText) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ElectricBlue, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = BackgroundDark
                    )
                }
            }
        }
    }

    // Gemini API Key Config Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = CardDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Gemini API Setup", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter a free Google Gemini API Key from Google AI Studio to unlock live real-time Gemini 1.5 Flash generative reasoning:",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = userApiKeyInput,
                        onValueChange = { userApiKeyInput = it },
                        placeholder = { Text("Paste AIzaSy... key", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = CardElevated
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Stored safely on-device in SharedPreferences.\n• If left blank, CircuitSense uses the high-precision offline physics engine.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        GeminiApiClient.setApiKey(context, userApiKeyInput)
                        isGeminiActive = GeminiApiClient.hasApiKey(context)
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = BackgroundDark)
                ) {
                    Text("Save & Activate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Text(
                text = message.modelBadge,
                color = ElectricBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = if (isUser) ElectricBlue else BackgroundDark,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) BackgroundDark else TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
