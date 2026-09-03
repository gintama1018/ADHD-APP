package com.circuitsense.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitsense.model.CircuitGraph
import com.circuitsense.ui.theme.*
import java.util.regex.Pattern

/**
 * Screen 4 — JSON Inspector Sheet (per DESIGN.md Section 6).
 * Simple modal bottom sheet, dark card background, monospace JSON text
 * with syntax coloring: keys in electric blue, values in amber/green.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonInspectorSheet(
    graph: CircuitGraph,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val jsonString = remember(graph) { graph.toJson() }
    val syntaxHighlighted = remember(jsonString) { highlightJson(jsonString) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CircuitGraph JSON Schema",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Circuit JSON", jsonString)
                        clipboard.setPrimaryClip(clip)
                        copied = true
                        Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Done else Icons.Default.ContentCopy,
                        contentDescription = "Copy JSON",
                        tint = if (copied) SuccessGreen else ElectricBlue
                    )
                }
            }

            Text(
                text = "Live structured graph feeding the generic Canvas renderer and 4-beat camera choreography.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Syntax-styled code box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .background(BackgroundDark, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = syntaxHighlighted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Applies syntax highlighting:
 * - Keys ("key":) -> ElectricBlue
 * - Strings ("value") -> SuccessGreen
 * - Numbers / Booleans -> WarmAmber
 * - Punctuation -> TextMuted
 */
private fun highlightJson(rawJson: String): AnnotatedString {
    val builder = AnnotatedString.Builder(rawJson)

    // Match JSON keys: "key":
    val keyPattern = Pattern.compile("\"([^\"]+)\"\\s*:")
    val keyMatcher = keyPattern.matcher(rawJson)
    while (keyMatcher.find()) {
        val start = keyMatcher.start(1) - 1
        val end = keyMatcher.end(1) + 1
        builder.addStyle(SpanStyle(color = ElectricBlue, fontWeight = FontWeight.SemiBold), start, end)
    }

    // Match numeric values: : 123.45
    val numPattern = Pattern.compile(":\\s*(-?\\d+(\\.\\d+)?)")
    val numMatcher = numPattern.matcher(rawJson)
    while (numMatcher.find()) {
        val start = numMatcher.start(1)
        val end = numMatcher.end(1)
        builder.addStyle(SpanStyle(color = WarmAmber, fontWeight = FontWeight.Bold), start, end)
    }

    // Match string values: : "value"
    val valStringPattern = Pattern.compile(":\\s*\"([^\"]*)\"")
    val valStringMatcher = valStringPattern.matcher(rawJson)
    while (valStringMatcher.find()) {
        val start = valStringMatcher.start(1) - 1
        val end = valStringMatcher.end(1) + 1
        builder.addStyle(SpanStyle(color = SuccessGreen), start, end)
    }

    return builder.toAnnotatedString()
}
