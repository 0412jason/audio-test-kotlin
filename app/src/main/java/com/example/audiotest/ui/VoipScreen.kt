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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.audiotest.audio.AudioEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoipScreen(viewModel: AudioViewModel, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // TX (Sender) State
    var txSampleRate by remember { mutableStateOf(48000) }
    var txChannelConfig by remember { mutableStateOf(AudioFormat.CHANNEL_IN_STEREO) }
    var txAudioFormat by remember { mutableStateOf(AudioFormat.ENCODING_PCM_24BIT_PACKED) }
    
    // RX (Receiver) State
    var rxSampleRate by remember { mutableStateOf(48000) }
    var rxChannelConfig by remember { mutableStateOf(AudioFormat.CHANNEL_OUT_STEREO) }
    var rxAudioFormat by remember { mutableStateOf(AudioFormat.ENCODING_PCM_24BIT_PACKED) }
    
    var isCalling by remember { mutableStateOf(false) }
    var isRinging by remember { mutableStateOf(false) }

    val instanceId = 3 // Tx ID, Rx ID is +1, Ringtone ID is +2

    val amplitudeData by viewModel.amplitudeData.collectAsState()
    val audioInfo by viewModel.audioInfoData.collectAsState()

    fun stopCall() {
        isCalling = false
        isRinging = false
        AudioEngine.recordingManager.stopRecording(instanceId)
        AudioEngine.playbackManager.stopPlayback(instanceId + 1)
        AudioEngine.playbackManager.stopPlayback(instanceId + 2)
        AudioEngine.deviceManager.setCommunicationDevice(null)
        AudioEngine.setAudioMode(context, 0) // MODE_NORMAL
    }

    fun startCall() {
        isCalling = true
        isRinging = true
        scope.launch {
            // 1. Ringtone Mode
            AudioEngine.setAudioMode(context, 1) // MODE_RINGTONE
            val ringtoneId = instanceId + 2
            
            AudioEngine.playbackManager.startPlayback(
                ringtoneId, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_24BIT_PACKED,
                6, 4, 0, null, null, false
            )
            
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
            
            // 2. VoIP Mode
            AudioEngine.setAudioMode(context, 3) // MODE_IN_COMMUNICATION
            
            // Start RX (Playback)
            AudioEngine.playbackManager.startPlayback(
                instanceId + 1, rxSampleRate, rxChannelConfig, rxAudioFormat,
                2 /* USAGE_VOICE_COMMUNICATION */, 1 /* CONTENT_TYPE_SPEECH */, 0, null, null, false
            )
            
            if (!isCalling) {
                AudioEngine.playbackManager.stopPlayback(instanceId + 1)
                return@launch
            }
            
            // Start TX (Recording)
            AudioEngine.recordingManager.startRecording(
                instanceId, txSampleRate, txChannelConfig, txAudioFormat,
                7 /* VOICE_COMMUNICATION */, false, null
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VoIP Configuration", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        // Waveform Visualizer
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            WaveformDisplay(
                amplitude = if (amplitudeData?.id == instanceId) amplitudeData?.amp ?: 0.0 else 0.0,
                color = MaterialTheme.colorScheme.error
            )
        }

        // RX Settings
        Text("--- RX (Receiver) ---", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        val sampleRates = mapOf("44100 Hz" to 44100, "48000 Hz" to 48000, "96000 Hz" to 96000)
        val rxChannels = mapOf("Mono" to AudioFormat.CHANNEL_OUT_MONO, "Stereo" to AudioFormat.CHANNEL_OUT_STEREO)
        val formats = mapOf("PCM 16-bit" to AudioFormat.ENCODING_PCM_16BIT, "PCM 24-bit" to AudioFormat.ENCODING_PCM_24BIT_PACKED)
        
        DropdownSelector("RX Sample Rate", sampleRates, rxSampleRate, { rxSampleRate = it })
        DropdownSelector("RX Channel Config", rxChannels, rxChannelConfig, { rxChannelConfig = it })
        DropdownSelector("RX Format", formats, rxAudioFormat, { rxAudioFormat = it })

        // TX Settings
        Text("--- TX (Sender) ---", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        val txChannels = mapOf("Mono" to AudioFormat.CHANNEL_IN_MONO, "Stereo" to AudioFormat.CHANNEL_IN_STEREO)
        
        DropdownSelector("TX Sample Rate", sampleRates, txSampleRate, { txSampleRate = it })
        DropdownSelector("TX Channel Config", txChannels, txChannelConfig, { txChannelConfig = it })
        DropdownSelector("TX Format", formats, txAudioFormat, { txAudioFormat = it })

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isCalling) stopCall() else startCall()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCalling) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(if (isCalling) if (isRinging) "Ringing... (Click to Cancel)" else "End Call" else "Start Call")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
