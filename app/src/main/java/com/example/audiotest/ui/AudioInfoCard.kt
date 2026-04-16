package com.example.audiotest.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audiotest.audio.AudioEngine
import com.example.audiotest.audio.AudioInfo

// ─────────────────────────────────────────────────────────────────────────────
// Audio format map (mirrors Flutter's AudioConfigFields.audioFormatMap)
// ─────────────────────────────────────────────────────────────────────────────

private val audioFormatMap = mapOf(
    3 to "8-bit PCM",
    2 to "16-bit PCM",
    21 to "24-bit PCM",
    4 to "Float PCM"
)

// ─────────────────────────────────────────────────────────────────────────────
// Helper: resolve a map key label like Flutter's _mapKeyLabel
// ─────────────────────────────────────────────────────────────────────────────

private fun mapKeyLabel(map: Map<String, Int>, value: Int?): String {
    if (value == null) return "Unknown"
    if (map.isEmpty()) return "Loading... ($value)"
    for ((key, v) in map) {
        if (v == value) return "$key ($v)"
    }
    return "Unknown ($value)"
}

// ─────────────────────────────────────────────────────────────────────────────
// AudioInfoCard — matches Flutter's AudioInfoCard widget
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AudioInfoCard(title: String, audioInfo: AudioInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        if (audioInfo == null) {
            Text(
                "No active audio track information.",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Card
        }

        Column(modifier = Modifier.padding(12.dp)) {
            // Title
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ── Basic ─────────────────────────────────────────────────────────
            val formatName = audioFormatMap[audioInfo.audioFormat]
            val formatLabel = if (formatName != null) {
                "$formatName (${audioInfo.audioFormat})"
            } else {
                "Unknown (${audioInfo.audioFormat})"
            }

            InfoRow("Sample Rate", "${audioInfo.sampleRate} Hz")
            InfoRow("Channel Count", audioInfo.channelCount.toString())
            InfoRow("Format", formatLabel)

            if (audioInfo.isOffloaded != null) {
                InfoRow("Offloaded", if (audioInfo.isOffloaded) "Yes" else "No")
            }

            // ── Attributes ────────────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionLabel("Attributes")

            if (audioInfo.usage != null) {
                InfoRow("Usage", mapKeyLabel(AudioEngine.cachedUsagesMap, audioInfo.usage))
            }
            if (audioInfo.contentType != null) {
                InfoRow("Content Type", mapKeyLabel(AudioEngine.cachedContentTypesMap, audioInfo.contentType))
            }
            if (audioInfo.flags != null) {
                InfoRow("Flags", mapKeyLabel(AudioEngine.cachedFlagsMap, audioInfo.flags))
            }
            if (audioInfo.audioSource != null) {
                InfoRow("Audio Source", mapKeyLabel(AudioEngine.cachedAudioSourcesMap, audioInfo.audioSource))
            }

            // ── Routed Devices ────────────────────────────────────────────────
            audioInfo.routedDevices?.let { devices ->
                if (devices.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SectionLabel("Routed Devices")
                    Spacer(modifier = Modifier.height(4.dp))

                    devices.forEachIndexed { index, device ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (devices.size > 1) {
                            Text(
                                "Device ${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        InfoRow("Name", device.name)
                        InfoRow("Type", device.type)
                        InfoRow("ID", device.id.toString())
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InfoRow — matches Flutter's InfoRow widget
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            label,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.weight(3f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionLabel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold
    )
}
