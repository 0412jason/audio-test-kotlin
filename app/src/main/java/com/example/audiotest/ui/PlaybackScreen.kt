package com.example.audiotest.ui

import android.media.AudioFormat
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.audiotest.audio.AudioEngine
import com.example.audiotest.audio.AudioInfo
import com.example.audiotest.audio.AudioInfoHelper

private const val MAX_AMPLITUDES = 100

@Composable
fun PlaybackScreen(viewModel: AudioViewModel, modifier: Modifier = Modifier, instanceId: Int = 1) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // State variables — defaults match Flutter
    var sampleRateText by remember { mutableStateOf("48000") }
    var channelConfig by remember { mutableIntStateOf(AudioFormat.CHANNEL_OUT_STEREO) }
    var audioFormat by remember { mutableIntStateOf(AudioFormat.ENCODING_PCM_24BIT_PACKED) }
    var usage by remember { mutableIntStateOf(1) } // USAGE_MEDIA
    var contentType by remember { mutableIntStateOf(2) } // CONTENT_TYPE_MUSIC
    var flags by remember { mutableIntStateOf(0) }
    var strategy by remember { mutableIntStateOf(-1) }
    var selectedMode by remember { mutableIntStateOf(-3) } // BYPASS default
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showPreferredDeviceDialog by remember { mutableStateOf(false) }

    // Playback source
    var playbackSource by remember { mutableIntStateOf(0) } // 0=Sine, 1=Local File
    var localFilePath by remember { mutableStateOf<String?>(null) }
    var enableOffload by remember { mutableStateOf(false) }
    var detectedFileInfo by remember { mutableStateOf<Map<String, Int>?>(null) }

    // Device selection
    var outputDevices by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedDeviceId by remember { mutableStateOf<Int?>(null) }

    // Audio info
    var actualAudioInfo by remember { mutableStateOf<AudioInfo?>(null) }
    var originalMode by remember { mutableStateOf<Int?>(null) }

    // Independent amplitude buffer for this instance
    val amplitudes = remember { mutableStateListOf<Float>().apply { repeat(MAX_AMPLITUDES) { add(0f) } } }

    // Dynamic maps
    var usagesMap by remember { mutableStateOf(AudioEngine.cachedUsagesMap) }
    var contentTypesMap by remember { mutableStateOf(AudioEngine.cachedContentTypesMap) }
    var flagsMap by remember { mutableStateOf(AudioEngine.cachedFlagsMap) }
    var strategyMap by remember { mutableStateOf(AudioEngine.cachedStreamTypesMap) }
    var audioModes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val deviceChange by viewModel.deviceChangeData.collectAsState()

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val cr = context.contentResolver
            try {
                val inputStream = cr.openInputStream(uri)
                val tempFile = java.io.File(context.cacheDir, "picked_audio_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                localFilePath = tempFile.absolutePath
                val info = AudioInfoHelper.getFileAudioInfo(tempFile.absolutePath)
                if (info != null) {
                    detectedFileInfo = info
                    sampleRateText = info["sampleRate"]?.toString() ?: "48000"
                    channelConfig = info["channelConfig"] ?: AudioFormat.CHANNEL_OUT_STEREO
                    audioFormat = info["audioFormat"] ?: AudioFormat.ENCODING_PCM_24BIT_PACKED
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackScreen", "File pick failed: $e")
            }
        }
    }

    // Load options on first composition
    LaunchedEffect(Unit) {
        usagesMap = AudioEngine.cachedUsagesMap
        contentTypesMap = AudioEngine.cachedContentTypesMap
        flagsMap = AudioEngine.cachedFlagsMap
        strategyMap = AudioEngine.cachedStreamTypesMap
        audioModes = buildMap {
            put("BYPASS", -3)
            putAll(AudioInfoHelper.getAudioModeOptions().entries.sortedBy { it.value }.associate { it.key to it.value })
        }
        outputDevices = AudioEngine.deviceManager.getAudioDevices(true)
    }

    // Listen for device changes
    LaunchedEffect(deviceChange) {
        outputDevices = AudioEngine.deviceManager.getAudioDevices(true)
    }

    // Collect amplitude from raw SharedFlow — independent per instance
    LaunchedEffect(instanceId) {
        AudioEngine.amplitudeFlow.collect { data ->
            if (data.id == instanceId && isPlaying) {
                amplitudes.add(data.amp.toFloat())
                if (amplitudes.size > MAX_AMPLITUDES) {
                    amplitudes.removeAt(0)
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

    // Cleanup on dispose — stop playback when leaving composition
    DisposableEffect(instanceId) {
        onDispose {
            AudioEngine.playbackManager.stopPlayback(instanceId)
        }
    }

    // Build Usage entries
    val usageEntries = if (usagesMap.isEmpty()) {
        mapOf("Media (1)" to 1, "Voice Comm (2)" to 2, "Alarm (4)" to 4, "Notification (5)" to 5, "Navigation (12)" to 12)
    } else {
        usagesMap.entries.sortedBy { it.value }.associate {
            "${it.key.removePrefix("USAGE_")} (${it.value})" to it.value
        }
    }

    // Build Content Type entries
    val contentTypeEntries = if (contentTypesMap.isEmpty()) {
        mapOf("Unknown (0)" to 0, "Speech (1)" to 1, "Music (2)" to 2, "Movie (3)" to 3, "Sonification (4)" to 4)
    } else {
        contentTypesMap.entries.sortedBy { it.value }.associate {
            "${it.key.removePrefix("CONTENT_TYPE_")} (${it.value})" to it.value
        }
    }

    // Build Flags entries
    val flagEntries = if (flagsMap.isEmpty()) {
        mapOf("None (0)" to 0, "Audibility Enforced (0x1)" to 0x1, "HW AV Sync (0x10)" to 0x10, "Low Latency (0x100)" to 0x100)
    } else {
        buildMap {
            put("None (0)", 0)
            putAll(flagsMap.entries.sortedBy { it.value }.associate {
                "${it.key.removePrefix("FLAG_")} (0x${it.value.toString(16).uppercase()})" to it.value
            })
        }
    }

    val strategyEntries = if (strategyMap.isEmpty()) {
        mapOf(
            "Manual (-1)" to -1,
        )
    } else {
        buildMap {
            put("Manual (-1)", -1)
            putAll(strategyMap.entries.sortedBy { it.value }.associate {
                "${it.key} (${it.value})" to it.value
            })
        }
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
        outputDevices.forEach { device ->
            val id = device["id"] as Int
            val name = device["name"] as String
            val type = device["type"] as String
            put("$id - $name - $type", id)
        }
    }

    // Channel config maps
    val outputChannelMap = mapOf("Mono (Out)" to AudioFormat.CHANNEL_OUT_MONO, "Stereo (Out)" to AudioFormat.CHANNEL_OUT_STEREO)
    val audioFormatMapDropdown = mapOf(
        "8-bit PCM" to AudioFormat.ENCODING_PCM_8BIT,
        "16-bit PCM" to AudioFormat.ENCODING_PCM_16BIT,
        "24-bit PCM" to AudioFormat.ENCODING_PCM_24BIT_PACKED,
        "Float PCM" to AudioFormat.ENCODING_PCM_FLOAT
    )

    // Source dropdown entries
    val sourceEntries = mapOf("Sine Wave" to 0, "Local File" to 1)

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp)
    ) {
        // ── Scrollable config area ──────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Playback Source
            DropdownSelector("Playback Source", sourceEntries, playbackSource, {
                playbackSource = it
                if (it == 0) {
                    detectedFileInfo = null
                    localFilePath = null
                    sampleRateText = "48000"
                    channelConfig = AudioFormat.CHANNEL_OUT_MONO
                    audioFormat = AudioFormat.ENCODING_PCM_24BIT_PACKED
                }
            }, enabled = !isPlaying)

            // File picker (only for local file mode)
            if (playbackSource == 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        localFilePath?.substringAfterLast('/') ?: "No file selected",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    ElevatedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                        enabled = !isPlaying
                    ) {
                        Text("Pick File")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = enableOffload,
                        onCheckedChange = { enableOffload = it },
                        enabled = !isPlaying && false
                    )
                    Text("Enable Compress Offload", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sample Rate (free text)
            OutlinedTextField(
                value = sampleRateText,
                onValueChange = { sampleRateText = it },
                label = { Text("Sample Rate (Hz)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPlaying && (playbackSource == 0 || detectedFileInfo == null),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Channel Config
            DropdownSelector(
                "Channel Config", outputChannelMap, channelConfig,
                { channelConfig = it },
                enabled = !isPlaying && (playbackSource == 0 || detectedFileInfo == null)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Audio Format
            DropdownSelector(
                "Audio Format", audioFormatMapDropdown, audioFormat,
                { audioFormat = it },
                enabled = !isPlaying && (playbackSource == 0 || detectedFileInfo == null)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Strategy
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    DropdownSelector("Strategy", strategyEntries, strategy, { strategy = it }, enabled = !isPlaying)
                }
                IconButton(onClick = { showPreferredDeviceDialog = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Preferred Device Settings")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Usage
            DropdownSelector("Usage", usageEntries, usage, { usage = it }, enabled = !isPlaying && strategy == -1)
            Spacer(modifier = Modifier.height(4.dp))

            // Content Type
            DropdownSelector("Content Type", contentTypeEntries, contentType, { contentType = it }, enabled = !isPlaying && strategy == -1)
            Spacer(modifier = Modifier.height(4.dp))

            // Flags
            DropdownSelector("Flags", flagEntries, flags, { flags = it }, enabled = !isPlaying && strategy == -1)
            Spacer(modifier = Modifier.height(4.dp))

            // Audio Mode
            DropdownSelector("Audio Mode", modeEntries, selectedMode, { selectedMode = it }, enabled = !isPlaying)
            Spacer(modifier = Modifier.height(4.dp))

            // Output Device
            DropdownSelector(
                "Output Device (Port ID - Name - Type)", deviceEntries,
                selectedDeviceId ?: -1,
                { selectedDeviceId = if (it == -1) null else it },
                enabled = !isPlaying
            )

            // AudioInfoCard when playing
            if (isPlaying && actualAudioInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AudioInfoCard(title = "Actual AudioTrack Info", audioInfo = actualAudioInfo)
            }
            
            if (showPreferredDeviceDialog) {
                PreferredDeviceDialog(
                    strategyMap = strategyMap,
                    onDismissRequest = { showPreferredDeviceDialog = false }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pinned Waveform ─────────────────────────────────────────────
        WaveformDisplay(
            amplitudes = amplitudes,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pinned Action Buttons ───────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            ElevatedButton(
                onClick = {
                    if (isPlaying) {
                        // Stop
                        isPlaying = false
                        isPaused = false
                        actualAudioInfo = null
                        amplitudes.clear()
                        repeat(MAX_AMPLITUDES) { amplitudes.add(0f) }
                        AudioEngine.playbackManager.stopPlayback(instanceId)
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
                        AudioEngine.playbackManager.startPlayback(
                            instanceId, sampleRate, channelConfig, audioFormat,
                            usage, contentType, flags, strategy, selectedDeviceId,
                            if (playbackSource == 1) localFilePath else null,
                            enableOffload
                        )
                        isPlaying = true
                        isPaused = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Icon(if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
            }

            if (isPlaying) {
                Spacer(modifier = Modifier.width(8.dp))
                ElevatedButton(
                    onClick = {
                        if (isPaused) {
                            AudioEngine.playbackManager.resumePlayback(instanceId)
                            isPaused = false
                        } else {
                            AudioEngine.playbackManager.pausePlayback(instanceId)
                            isPaused = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = null)
                }
            }
        }
    }
}
