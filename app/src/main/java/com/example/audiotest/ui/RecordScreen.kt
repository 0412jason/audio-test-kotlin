package com.example.audiotest.ui

import android.media.AudioFormat
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audiotest.audio.AudioEngine
import com.example.audiotest.audio.AudioInfo
import com.example.audiotest.audio.AudioInfoHelper

private const val MAX_AMPLITUDES = 100

@Composable
fun RecordScreen(viewModel: AudioViewModel, modifier: Modifier = Modifier, instanceId: Int = 2) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // State variables — defaults match Flutter
    var sampleRateText by remember { mutableStateOf("48000") }
    var channelConfig by remember { mutableIntStateOf(AudioFormat.CHANNEL_IN_STEREO) }
    var audioFormat by remember { mutableIntStateOf(AudioFormat.ENCODING_PCM_24BIT_PACKED) }
    var selectedSource by remember { mutableIntStateOf(1) } // MIC
    var selectedMode by remember { mutableIntStateOf(-3) } // BYPASS default
    var saveToFile by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var savedFilePath by remember { mutableStateOf<String?>(null) }

    // Device selection
    var inputDevices by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedDeviceId by remember { mutableStateOf<Int?>(null) }

    // Audio info
    var actualAudioInfo by remember { mutableStateOf<AudioInfo?>(null) }
    var originalMode by remember { mutableStateOf<Int?>(null) }

    // Independent amplitude buffer for this instance
    val amplitudes = remember { mutableStateListOf<Float>().apply { repeat(MAX_AMPLITUDES) { add(0f) } } }

    // Dynamic maps
    var audioSources by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var audioModes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val deviceChange by viewModel.deviceChangeData.collectAsState()

    // Load options
    LaunchedEffect(Unit) {
        audioSources = AudioInfoHelper.getAudioSourceOptions().entries.sortedBy { it.value }.associate { it.key to it.value }
        audioModes = buildMap {
            put("BYPASS", -3)
            putAll(AudioInfoHelper.getAudioModeOptions().entries.sortedBy { it.value }.associate { it.key to it.value })
        }
        inputDevices = AudioEngine.deviceManager.getAudioDevices(false)
    }

    // Listen for device changes
    LaunchedEffect(deviceChange) {
        inputDevices = AudioEngine.deviceManager.getAudioDevices(false)
    }

    // Collect amplitude + saved path from raw SharedFlow — independent per instance
    LaunchedEffect(instanceId) {
        AudioEngine.amplitudeFlow.collect { data ->
            if (data.id == instanceId) {
                if (data.path != null) {
                    savedFilePath = data.path
                } else if (isRecording) {
                    amplitudes.add(data.amp.toFloat())
                    if (amplitudes.size > MAX_AMPLITUDES) {
                        amplitudes.removeAt(0)
                    }
                }
            }
        }
    }

    // Collect audio info from raw SharedFlow
    LaunchedEffect(instanceId) {
        AudioEngine.audioInfoFlow.collect { info ->
            if (info.id == instanceId) {
                actualAudioInfo = info
            }
        }
    }

    // Cleanup on dispose — stop recording when leaving composition
    DisposableEffect(instanceId) {
        onDispose {
            AudioEngine.recordingManager.stopRecording(instanceId)
        }
    }

    // Build Audio Source entries
    val sourceEntries = if (audioSources.isEmpty()) {
        mapOf("DEFAULT (0)" to 0, "MIC (1)" to 1, "CAMCORDER (5)" to 5, "VOICE_RECOGNITION (6)" to 6, "VOICE_COMMUNICATION (7)" to 7)
    } else {
        audioSources.entries.associate { "${it.key} (${it.value})" to it.value }
    }

    // Build Audio Mode entries
    val modeEntries = if (audioModes.isEmpty()) {
        mapOf("BYPASS (-3)" to -3, "MODE_NORMAL (0)" to 0, "MODE_IN_COMMUNICATION (3)" to 3)
    } else {
        audioModes.entries.associate { "${it.key} (${it.value})" to it.value }
    }

    // Build Device entries
    val deviceEntries = buildMap {
        put("Default Routing", -1)
        inputDevices.forEach { device ->
            val id = device["id"] as Int
            val name = device["name"] as String
            val type = device["type"] as String
            put("$id - $name - $type", id)
        }
    }

    // Channel config maps
    val inputChannelMap = mapOf("Mono (In)" to AudioFormat.CHANNEL_IN_MONO, "Stereo (In)" to AudioFormat.CHANNEL_IN_STEREO)
    val audioFormatMapDropdown = mapOf(
        "8-bit PCM" to AudioFormat.ENCODING_PCM_8BIT,
        "16-bit PCM" to AudioFormat.ENCODING_PCM_16BIT,
        "24-bit PCM" to AudioFormat.ENCODING_PCM_24BIT_PACKED,
        "Float PCM" to AudioFormat.ENCODING_PCM_FLOAT
    )

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp)
    ) {
        // ── Scrollable config area ──────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Sample Rate (free text)
            OutlinedTextField(
                value = sampleRateText,
                onValueChange = { sampleRateText = it },
                label = { Text("Sample Rate (Hz)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Channel Config
            DropdownSelector(
                "Channel Config", inputChannelMap, channelConfig,
                { channelConfig = it },
                enabled = !isRecording
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Audio Format
            DropdownSelector(
                "Audio Format", audioFormatMapDropdown, audioFormat,
                { audioFormat = it },
                enabled = !isRecording
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Audio Source
            DropdownSelector("Audio Source", sourceEntries, selectedSource, { selectedSource = it }, enabled = !isRecording)
            Spacer(modifier = Modifier.height(8.dp))

            // Audio Mode
            DropdownSelector("Audio Mode", modeEntries, selectedMode, { selectedMode = it }, enabled = !isRecording)
            Spacer(modifier = Modifier.height(8.dp))

            // Input Device
            DropdownSelector(
                "Input Device (Port ID - Name - Type)", deviceEntries,
                selectedDeviceId ?: -1,
                { selectedDeviceId = if (it == -1) null else it },
                enabled = !isRecording
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Save to WAV File checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = saveToFile,
                    onCheckedChange = { saveToFile = it },
                    enabled = !isRecording
                )
                Text("Save to WAV File", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Saved file path
            if (savedFilePath != null) {
                Text(
                    "Saved: $savedFilePath",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF4CAF50) // Green
                )
            }

            // AudioInfoCard when recording
            if (isRecording && actualAudioInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AudioInfoCard(title = "Actual AudioRecord Info", audioInfo = actualAudioInfo)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pinned Waveform ─────────────────────────────────────────────
        WaveformDisplay(
            amplitudes = amplitudes,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pinned Action Button ────────────────────────────────────────
        ElevatedButton(
            onClick = {
                if (isRecording) {
                    // Stop
                    isRecording = false
                    actualAudioInfo = null
                    amplitudes.clear()
                    repeat(MAX_AMPLITUDES) { amplitudes.add(0f) }
                    AudioEngine.recordingManager.stopRecording(instanceId)
                    if (selectedMode != -3 && originalMode != null) {
                        AudioEngine.setAudioMode(context, originalMode!!)
                        originalMode = null
                    }
                } else {
                    // Start
                    val sampleRate = sampleRateText.toIntOrNull() ?: 48000
                    if (selectedMode != -3) {
                        val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
                        originalMode = am.mode
                        AudioEngine.setAudioMode(context, selectedMode)
                    }
                    savedFilePath = null
                    AudioEngine.recordingManager.startRecording(
                        instanceId, sampleRate, channelConfig, audioFormat,
                        selectedSource, saveToFile, selectedDeviceId
                    )
                    isRecording = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null)
        }
    }
}
