package com.circuitsense.qa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitsense.model.CircuitGraph

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender { USER, BOT }

/**
 * Isolated Offline Physics Q&A Engine ("Ask a Doubt").
 * Decoupled from core animation so it never blocks or breaks the main demo.
 */
object OfflinePhysicsBot {

    /**
     * Answers user physics questions based on current circuit parameters.
     */
    fun answerDoubt(question: String, graph: CircuitGraph): String {
        val q = question.lowercase()
        val v = graph.formula.V
        val r = graph.formula.R
        val i = graph.formula.I

        return when {
            q.contains("decrease") || q.contains("slow") || q.contains("why slow") -> {
                "In your circuit, current is I = V / R = ${v}V / ${r}Ω = ${i}A. When resistance (R) increases, electrons experience more inelastic scattering with lattice ions, converting kinetic energy into heat and decreasing drift velocity!"
            }
            q.contains("voltage") || q.contains("potential") || q.contains("what is v") -> {
                "Voltage (currently ${v}V) is electrical potential difference—the work required per unit charge to move electrons between two points: V = W / Q. It acts as the electrical 'pressure' propelling the charge through the circuit."
            }
            q.contains("resistance") || q.contains("resistor") || q.contains("what does resistor do") -> {
                "The ${r}Ω resistor opposes current flow. At the microscopic level, free electrons collide with vibrating lattice atoms, producing thermal heat (Joule heating: P = I²R = ${(i * i * r).toFloat()} Watts)."
            }
            q.contains("increase voltage") || q.contains("double voltage") || q.contains("more volts") -> {
                val doubledV = v * 2
                val newI = doubledV / r
                "If you double voltage from ${v}V to ${doubledV}V, the electric field doubles! By Ohm's law (I = V/R), current doubles to ${Math.round(newI * 100.0) / 100.0}A, making Sparky the electron flow twice as fast!"
            }
            q.contains("power") || q.contains("energy") || q.contains("watt") -> {
                val power = v * i
                "Power dissipated in this circuit is P = V × I = ${v}V × ${i}A = ${Math.round(power * 100.0) / 100.0} Watts. This is the rate at which electrical energy is transformed into thermal energy."
            }
            q.contains("adhd") || q.contains("motion") || q.contains("cartoon") -> {
                "Motion graphics and zoom transitions anchor dopamine and spatial attention in ADHD students, transforming abstract mathematical symbols (V=IR) into dynamic, memorable physical narratives!"
            }
            else -> {
                "According to Ohm's Law (V = I × R), current is directly proportional to voltage (${v}V) and inversely proportional to resistance (${r}Ω). For your circuit, I = ${v} / ${r} = ${i} Amperes."
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskDoubtBottomSheet(
    graph: CircuitGraph,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "1",
                sender = MessageSender.BOT,
                text = "Hello! I am your offline physics tutor. Ask me any question about this ${graph.formula.V.toInt()}V, ${graph.formula.R.toInt()}Ω circuit!"
            )
        )
    }

    val sampleQueries = listOf(
        "Why did the current slow down?",
        "What does the ${graph.formula.V.toInt()}V battery actually do?",
        "What happens if I increase voltage?",
        "How does resistance produce heat?"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141824),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF90A4AE)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFAB00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ask a Doubt (On-Device Physics Q&A)",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick suggestion chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(sampleQueries) { sampleQ ->
                    SuggestionChip(
                        onClick = {
                            messages.add(ChatMessage(System.currentTimeMillis().toString(), MessageSender.USER, sampleQ))
                            val reply = OfflinePhysicsBot.answerDoubt(sampleQ, graph)
                            messages.add(ChatMessage((System.currentTimeMillis() + 1).toString(), MessageSender.BOT, reply))
                        },
                        label = { Text(sampleQ, fontSize = 12.sp, color = Color(0xFFB0BEC5)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1C2230)
                        )
                    )
                }
            }

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("Ask about current, resistance, heat...", color = Color(0xFF78909C), fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E2433),
                        unfocusedContainerColor = Color(0xFF1E2433),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            val userQ = queryText.trim()
                            queryText = ""
                            messages.add(ChatMessage(System.currentTimeMillis().toString(), MessageSender.USER, userQ))
                            val reply = OfflinePhysicsBot.answerDoubt(userQ, graph)
                            messages.add(ChatMessage((System.currentTimeMillis() + 1).toString(), MessageSender.BOT, reply))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF00E5FF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF0D1B2A)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) Color(0xFF00E5FF) else Color(0xFF222B3D),
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
                color = if (isUser) Color(0xFF0D1B2A) else Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
