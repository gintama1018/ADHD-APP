package com.circuitsense.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.circuitsense.model.CircuitGraph
import com.circuitsense.renderer.StoryPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages Text-To-Speech (TTS) narration synchronized with the motion graphics beats.
 * Uses Android built-in offline TextToSpeech engine.
 * Emits visual subtitles for neurodivergent/ADHD learners.
 */
class NarrationManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _currentSubtitle = MutableStateFlow("")
    val currentSubtitle: StateFlow<String> = _currentSubtitle.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var isMuted: Boolean = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.95f) // slightly slower for optimal comprehension
            tts?.setPitch(1.0f)
            isInitialized = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    /**
     * Speaks the narration corresponding to the given story phase and circuit parameters.
     */
    fun narratePhase(phase: StoryPhase, graph: CircuitGraph) {
        val script = getNarrationScript(phase, graph)
        _currentSubtitle.value = script

        if (!isMuted && isInitialized) {
            tts?.stop()
            tts?.speak(script, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_${phase.name}")
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentSubtitle.value = ""
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun getNarrationScript(phase: StoryPhase, graph: CircuitGraph): String {
            val v = graph.formula.V.toInt()
            val r = graph.formula.R.toInt()
            val i = graph.formula.I

            return when (phase) {
                StoryPhase.OVERVIEW -> {
                    "Circuit detected! A $v volt battery powering a $r ohm resistor in a single closed loop."
                }
                StoryPhase.BATTERY_FOCUS -> {
                    "Current is born here! The $v volt battery separates charges, generating a potential difference that pushes electrons into the wire."
                }
                StoryPhase.WIRE_TRANSIT -> {
                    "Electrons travel freely through the copper conductor, propelled by the electric field."
                }
                StoryPhase.RESISTOR_FOCUS -> {
                    "Now electrons enter the $r ohm resistor! Repeated collisions with the atomic lattice slow the flow, dissipating energy as heat."
                }
                StoryPhase.FULL_LOOP -> {
                    "The loop reaches steady equilibrium! By Ohm's Law, current I equals V over R: $v volts divided by $r ohms yields $i amperes of continuous flow."
                }
            }
        }
    }
}
