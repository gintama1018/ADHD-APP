package com.circuitsense.data

import android.content.Context
import com.circuitsense.model.CircuitGraph
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class SavedCircuitItem(
    val id: String,
    val title: String,
    val subject: String = "Physics (Electricity)",
    val voltage: Double,
    val resistance: Double,
    val current: Double,
    val dateFormatted: String,
    val graphJson: String
)

/**
 * Manages study history and saved simulations (Maths / Physics / Science).
 * Allows users to review past scanned diagrams, calculations, and animations.
 */
object CircuitHistoryManager {

    private const val PREFS_NAME = "circuitsense_study_history"
    private const val KEY_HISTORY_LIST = "saved_circuits_history"
    private val json = Json { ignoreUnknownKeys = true }

    fun getHistory(context: Context): List<SavedCircuitItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY_LIST, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCircuit(context: Context, graph: CircuitGraph, customTitle: String? = null): SavedCircuitItem {
        val currentList = getHistory(context).toMutableList()
        val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date())
        val title = customTitle ?: "${graph.formula.V.toInt()}V / ${graph.formula.R.toInt()}Ω Circuit"

        val item = SavedCircuitItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            subject = "Physics: Ohm's Law",
            voltage = graph.formula.V,
            resistance = graph.formula.R,
            current = graph.formula.I,
            dateFormatted = dateStr,
            graphJson = graph.toJson()
        )

        // Prepend new item, keep last 20
        currentList.add(0, item)
        val trimmed = currentList.take(20)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY_LIST, json.encodeToString(trimmed)).apply()
        return item
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY_LIST).apply()
    }
}
