package com.example.audiotest.ui

import android.media.AudioFormat
import android.media.MediaRecorder
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
fun RecordScreen(viewModel: AudioViewModel, modifier: Modifier = Modifier, instanceId: Int = 2) {
    val scrollState = rememberScrollState()
    
    // State variables
    var sampleRate by remember { mutableStateOf(48000) }
    var channelConfig by remember { mutableStateOf(AudioFormat.CHANNEL_IN_MONO) }
    var audioFormat by remember { mutableStateOf(AudioFormat.ENCODING_PCM_16BIT) }
    var audioSource by remember { mutableStateOf(MediaRecorder.AudioSource.MIC) }
    var saveToFile by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    val amplitudeData by viewModel.amplitudeData.collectAsState()
    val audioInfo by viewModel.audioInfoData.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Record Testing", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Waveform Visualizer
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            WaveformDisplay(
                amplitude = if (amplitudeData?.id == instanceId) amplitudeData?.amp ?: 0.0 else 0.0,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // Configuration
        val sampleRates = mapOf("44100 Hz" to 44100, "48000 Hz" to 48000, "96000 Hz" to 96000, "192000 Hz" to 192000)
        val channels = mapOf("Mono" to AudioFormat.CHANNEL_IN_MONO, "Stereo" to AudioFormat.CHANNEL_IN_STEREO)
        val formats = mapOf(
            "PCM 8-bit" to AudioFormat.ENCODING_PCM_8BIT,
            "PCM 16-bit" to AudioFormat.ENCODING_PCM_16BIT,
            "PCM 24-bit" to AudioFormat.ENCODING_PCM_24BIT_PACKED,
            "PCM Float" to AudioFormat.ENCODING_PCM_FLOAT
        )

        DropdownSelector("Sample Rate", sampleRates, sampleRate, { sampleRate = it })
        DropdownSelector("Channel Config", channels, channelConfig, { channelConfig = it })
        DropdownSelector("Audio Format", formats, audioFormat, { audioFormat = it })
        
        DropdownSelector("Audio Source", AudioEngine.cachedAudioSourcesMap, audioSource, { audioSource = it })

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Save to WAV File")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = saveToFile, onCheckedChange = { saveToFile = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (!isRecording) {
                        AudioEngine.recordingManager.startRecording(
                            instanceId, sampleRate, channelConfig, audioFormat, audioSource, saveToFile, null
                        )
                        isRecording = true
                    } else {
                        AudioEngine.recordingManager.stopRecording(instanceId)
                        isRecording = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRecording) "Stop Recording" else "Start Recording")
            }
        }
        
        amplitudeData?.path?.let {
            if (!isRecording && it.isNotEmpty()) {
                Text("Saved to: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Info card
        if (isRecording && audioInfo?.id == instanceId) {
            AudioInfoCard(audioInfo)
        }
    }
}
