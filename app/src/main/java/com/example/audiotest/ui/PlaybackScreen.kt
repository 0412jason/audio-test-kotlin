package com.example.audiotest.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.audiotest.audio.AudioEngine

@Composable
fun PlaybackScreen(viewModel: AudioViewModel, modifier: Modifier = Modifier, instanceId: Int = 1) {
    val scrollState = rememberScrollState()
    
    // State variables
    var sampleRate by remember { mutableStateOf(48000) }
    var channelConfig by remember { mutableStateOf(AudioFormat.CHANNEL_OUT_MONO) }
    var audioFormat by remember { mutableStateOf(AudioFormat.ENCODING_PCM_16BIT) }
    var usage by remember { mutableStateOf(AudioAttributes.USAGE_MEDIA) }
    var contentType by remember { mutableStateOf(AudioAttributes.CONTENT_TYPE_MUSIC) }
    var flags by remember { mutableStateOf(0) }
    var isOffload by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val amplitudeData by viewModel.amplitudeData.collectAsState()
    val audioInfo by viewModel.audioInfoData.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Playback Testing", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Waveform Visualizer
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            WaveformDisplay(
                amplitude = if (amplitudeData?.id == instanceId) amplitudeData?.amp ?: 0.0 else 0.0,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Configuration
        val sampleRates = mapOf("44100 Hz" to 44100, "48000 Hz" to 48000, "96000 Hz" to 96000, "192000 Hz" to 192000)
        val channels = mapOf("Mono" to AudioFormat.CHANNEL_OUT_MONO, "Stereo" to AudioFormat.CHANNEL_OUT_STEREO)
        val formats = mapOf(
            "PCM 8-bit" to AudioFormat.ENCODING_PCM_8BIT,
            "PCM 16-bit" to AudioFormat.ENCODING_PCM_16BIT,
            "PCM 24-bit" to AudioFormat.ENCODING_PCM_24BIT_PACKED,
            "PCM Float" to AudioFormat.ENCODING_PCM_FLOAT
        )

        DropdownSelector("Sample Rate", sampleRates, sampleRate, { sampleRate = it })
        DropdownSelector("Channel Config", channels, channelConfig, { channelConfig = it })
        DropdownSelector("Audio Format", formats, audioFormat, { audioFormat = it })
        
        DropdownSelector("Usage", AudioEngine.cachedUsagesMap, usage, { usage = it })
        DropdownSelector("Content Type", AudioEngine.cachedContentTypesMap, contentType, { contentType = it })
        DropdownSelector("Flags", AudioEngine.cachedFlagsMap, flags, { flags = it })

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Compress Offload Playback (Requires File)")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = isOffload, onCheckedChange = { isOffload = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                if (!isPlaying) {
                    AudioEngine.playbackManager.startPlayback(
                        instanceId, sampleRate, channelConfig, audioFormat, usage, contentType, flags, null, null, isOffload
                    )
                    isPlaying = true
                } else {
                    AudioEngine.playbackManager.stopPlayback(instanceId)
                    isPlaying = false
                }
            }) {
                Text(if (isPlaying) "Stop" else "Play Sine Wave")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Info card
        if (isPlaying && audioInfo?.id == instanceId) {
            AudioInfoCard(audioInfo)
        }
    }
}
