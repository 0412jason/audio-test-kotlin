package com.example.audiotest.ui

import android.media.AudioFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audiotest.audio.AudioEngine
import com.example.audiotest.audio.AudioInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_AMPLITUDES = 100

@Composable
fun VoipScreen(viewModel: AudioViewModel, modifier: Modifier = Modifier, instanceId: Int = 30) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // TX (Sender) State — defaults match Flutter
    var txSampleRateText by remember { mutableStateOf("48000") }
    var txChannelConfig by remember { mutableIntStateOf(AudioFormat.CHANNEL_IN_STEREO) }
    var txAudioFormat by remember { mutableIntStateOf(AudioFormat.ENCODING_PCM_24BIT_PACKED) }

    // RX (Receiver) State
    var rxSampleRateText by remember { mutableStateOf("48000") }
    var rxChannelConfig by remember { mutableIntStateOf(AudioFormat.CHANNEL_OUT_STEREO) }
    var rxAudioFormat by remember { mutableIntStateOf(AudioFormat.ENCODING_PCM_24BIT_PACKED) }

    var isCalling by remember { mutableStateOf(false) }
    var isRinging by remember { mutableStateOf(false) }
    var saveToFile by remember { mutableStateOf(false) }
    var savedFilePath by remember { mutableStateOf<String?>(null) }

    // Device selection
    var inputDevices by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var outputDevices by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedInputDeviceId by remember { mutableStateOf<Int?>(null) }
    var selectedOutputDeviceId by remember { mutableStateOf<Int?>(null) }

    // Audio info
    var actualRxAudioInfo by remember { mutableStateOf<AudioInfo?>(null) }
    var actualTxAudioInfo by remember { mutableStateOf<AudioInfo?>(null) }
    var originalMode by remember { mutableStateOf<Int?>(null) }

    // Independent amplitude buffer for this instance
    val amplitudes = remember { mutableStateListOf<Float>().apply { repeat(MAX_AMPLITUDES) { add(0f) } } }

    val deviceChange by viewModel.deviceChangeData.collectAsState()

    // Fixed VoIP values (matching Flutter)
    val selectedSource = 7 // VOICE_COMMUNICATION
    val selectedMode = 3   // MODE_IN_COMMUNICATION

    // Tx ID uses instanceId, Rx ID is instanceId + 1, Ringtone ID is instanceId + 2

    // Load devices
    LaunchedEffect(Unit) {
        inputDevices = AudioEngine.deviceManager.getAudioDevices(false)
        outputDevices = AudioEngine.deviceManager.getAudioDevices(true)
    }

    // Listen for device changes
    LaunchedEffect(deviceChange) {
        inputDevices = AudioEngine.deviceManager.getAudioDevices(false)
        outputDevices = AudioEngine.deviceManager.getAudioDevices(true)
    }

    // Collect amplitude + saved path from raw SharedFlow — independent per instance
    LaunchedEffect(instanceId) {
        AudioEngine.amplitudeFlow.collect { data ->
            if (data.id == instanceId) {
                if (data.path != null) {
                    savedFilePath = data.path
                } else if (isCalling) {
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
            if (info.id == instanceId + 1) {
                actualRxAudioInfo = info
            } else if (info.id == instanceId) {
                actualTxAudioInfo = info
            }
        }
    }

    // Cleanup on dispose — stop all audio when leaving composition
    DisposableEffect(instanceId) {
        onDispose {
            AudioEngine.recordingManager.stopRecording(instanceId)
            AudioEngine.playbackManager.stopPlayback(instanceId + 1)
            AudioEngine.playbackManager.stopPlayback(instanceId + 2)
            AudioEngine.deviceManager.setCommunicationDevice(null)
        }
    }

    fun stopCall() {
        isCalling = false
        isRinging = false
        actualRxAudioInfo = null
        actualTxAudioInfo = null
        amplitudes.clear()
        repeat(MAX_AMPLITUDES) { amplitudes.add(0f) }
        AudioEngine.recordingManager.stopRecording(instanceId)
        AudioEngine.playbackManager.stopPlayback(instanceId + 1)
        AudioEngine.playbackManager.stopPlayback(instanceId + 2)
        AudioEngine.deviceManager.setCommunicationDevice(null)
        if (originalMode != null) {
            AudioEngine.setAudioMode(context, originalMode!!)
            originalMode = null
        }
    }

    fun startCall() {
        isCalling = true
        isRinging = true
        savedFilePath = null

        scope.launch {
            originalMode = AudioEngine.getAudioMode(context)

            // 1. Set mode to ringtone (1 = MODE_RINGTONE)
            AudioEngine.setAudioMode(context, 1)
            val ringtoneId = instanceId + 2

            // 2. Play 2 seconds of beep beep sound as ringtone
            AudioEngine.playbackManager.startPlayback(
                ringtoneId, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_24BIT_PACKED,
                6 /* USAGE_NOTIFICATION_RINGTONE */, 4 /* CONTENT_TYPE_SONIFICATION */, 0, -1, null, null, false
            )

            // 2 seconds of beeps (400ms on, 100ms off, 4 times)
            for (i in 0 until 4) {
                if (!isCalling) {
                    AudioEngine.playbackManager.stopPlayback(ringtoneId)
                    return@launch
                }
                if (i > 0) AudioEngine.playbackManager.resumePlayback(ringtoneId)
                delay(400)
                if (!isCalling) {
                    AudioEngine.playbackManager.stopPlayback(ringtoneId)
                    return@launch
                }
                AudioEngine.playbackManager.pausePlayback(ringtoneId)
                delay(100)
            }
            AudioEngine.playbackManager.stopPlayback(ringtoneId)
            if (!isCalling) return@launch

            isRinging = false

            // 3. Normal VoIP setup
            AudioEngine.setAudioMode(context, selectedMode) // MODE_IN_COMMUNICATION
            AudioEngine.deviceManager.setCommunicationDevice(selectedOutputDeviceId)

            val txSampleRate = txSampleRateText.toIntOrNull() ?: 48000
            val rxSampleRate = rxSampleRateText.toIntOrNull() ?: 48000

            // Start RX (Playback)
            AudioEngine.playbackManager.startPlayback(
                instanceId + 1, rxSampleRate, rxChannelConfig, rxAudioFormat,
                2 /* USAGE_VOICE_COMMUNICATION */, 1 /* CONTENT_TYPE_SPEECH */, 0, -1,
                selectedOutputDeviceId, null, false
            )

            if (!isCalling) {
                AudioEngine.playbackManager.stopPlayback(instanceId + 1)
                return@launch
            }

            // Start TX (Recording)
            AudioEngine.recordingManager.startRecording(
                instanceId, txSampleRate, txChannelConfig, txAudioFormat,
                selectedSource, saveToFile, selectedInputDeviceId
            )
        }
    }

    // Channel config entries (dynamic)
    val outputChannelEntries = if (AudioEngine.cachedOutputChannelMap.isEmpty()) {
        mapOf("CHANNEL_OUT_MONO" to AudioFormat.CHANNEL_OUT_MONO, "CHANNEL_OUT_STEREO" to AudioFormat.CHANNEL_OUT_STEREO)
    } else {
        AudioEngine.cachedOutputChannelMap
    }.entries.sortedBy { it.value }.associate {
        "${it.key.removePrefix("CHANNEL_OUT_")} (0x${it.value.toString(16).uppercase()})" to it.value
    }

    val inputChannelEntries = if (AudioEngine.cachedInputChannelMap.isEmpty()) {
        mapOf("CHANNEL_IN_MONO" to AudioFormat.CHANNEL_IN_MONO, "CHANNEL_IN_STEREO" to AudioFormat.CHANNEL_IN_STEREO)
    } else {
        AudioEngine.cachedInputChannelMap
    }.entries.sortedBy { it.value }.associate {
        "${it.key.removePrefix("CHANNEL_IN_")} (0x${it.value.toString(16).uppercase()})" to it.value
    }

    // Audio format (hardcoded)
    val audioFormatMapDropdown = mapOf(
        "8-bit PCM" to AudioFormat.ENCODING_PCM_8BIT,
        "16-bit PCM" to AudioFormat.ENCODING_PCM_16BIT,
        "24-bit PCM" to AudioFormat.ENCODING_PCM_24BIT_PACKED,
        "Float PCM" to AudioFormat.ENCODING_PCM_FLOAT
    )

    // Build Device entries
    val outputDeviceEntries = buildMap {
        put("Default Output", -1)
        outputDevices.forEach { device ->
            val id = device["id"] as Int
            val name = device["name"] as String
            val type = device["type"] as String
            put("$id - $name - $type", id)
        }
    }
    val inputDeviceEntries = buildMap {
        put("Default Input", -1)
        inputDevices.forEach { device ->
            val id = device["id"] as Int
            val name = device["name"] as String
            val type = device["type"] as String
            put("$id - $name - $type", id)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp)
    ) {
        // ── Scrollable config area ──────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // === RX Section ===
            Text(
                "--- RX (Receive / Receiver) ---",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // RX Sample Rate
            OutlinedTextField(
                value = rxSampleRateText,
                onValueChange = { rxSampleRateText = it },
                label = { Text("RX Sample Rate (Hz)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCalling,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // RX Channel Config
            DropdownSelector(
                "RX Channel Config", outputChannelEntries, rxChannelConfig,
                { rxChannelConfig = it },
                enabled = !isCalling
            )
            Spacer(modifier = Modifier.height(8.dp))

            // RX Audio Format
            DropdownSelector(
                "RX Audio Format", audioFormatMapDropdown, rxAudioFormat,
                { rxAudioFormat = it },
                enabled = !isCalling
            )
            Spacer(modifier = Modifier.height(8.dp))

            // RX Output Device — live-switchable during call
            DropdownSelector(
                "RX Output Device (Speaker/Earpiece)", outputDeviceEntries,
                selectedOutputDeviceId ?: -1,
                { newId ->
                    selectedOutputDeviceId = if (newId == -1) null else newId
                    if (isCalling && !isRinging) {
                        AudioEngine.deviceManager.setCommunicationDevice(if (newId == -1) null else newId)
                    }
                },
                enabled = !isRinging
            )
            Spacer(modifier = Modifier.height(24.dp))

            // === TX Section ===
            Text(
                "--- TX (Transmit / Sender) ---",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // TX Sample Rate
            OutlinedTextField(
                value = txSampleRateText,
                onValueChange = { txSampleRateText = it },
                label = { Text("TX Sample Rate (Hz)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCalling,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // TX Channel Config
            DropdownSelector(
                "TX Channel Config", inputChannelEntries, txChannelConfig,
                { txChannelConfig = it },
                enabled = !isCalling
            )
            Spacer(modifier = Modifier.height(8.dp))

            // TX Audio Format
            DropdownSelector(
                "TX Audio Format", audioFormatMapDropdown, txAudioFormat,
                { txAudioFormat = it },
                enabled = !isCalling
            )
            Spacer(modifier = Modifier.height(8.dp))

            // TX Input Device
            DropdownSelector(
                "TX Input Device (Microphone)", inputDeviceEntries,
                selectedInputDeviceId ?: -1,
                { selectedInputDeviceId = if (it == -1) null else it },
                enabled = !isCalling
            )
            Spacer(modifier = Modifier.height(16.dp))

            // TX Save to WAV File checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = saveToFile,
                    onCheckedChange = { saveToFile = it },
                    enabled = !isCalling
                )
                Text("TX Save to WAV File", style = MaterialTheme.typography.bodySmall)
            }

            // Saved file path
            if (savedFilePath != null) {
                Text(
                    "Saved: $savedFilePath",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // RX AudioInfoCard
            if (isCalling && !isRinging && actualRxAudioInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AudioInfoCard(title = "Actual RX AudioTrack Info", audioInfo = actualRxAudioInfo)
            }

            // TX AudioInfoCard
            if (isCalling && !isRinging && actualTxAudioInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AudioInfoCard(title = "Actual TX AudioRecord Info", audioInfo = actualTxAudioInfo)
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
                if (isCalling) stopCall() else startCall()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = if (isCalling) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Icon(
                if (isCalling) Icons.Filled.CallEnd else Icons.Filled.Call,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(if (isCalling) "End Call" else "Start Call")
        }
    }
}
