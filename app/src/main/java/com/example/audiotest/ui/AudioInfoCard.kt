package com.example.audiotest.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audiotest.audio.AudioInfo

@Composable
fun AudioInfoCard(audioInfo: AudioInfo?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (audioInfo == null) {
            Text(
                "No active audio track information.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Card
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Active Audio Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            InfoRow("ID", audioInfo.id.toString())
            InfoRow("Sample Rate", "${audioInfo.sampleRate} Hz")
            InfoRow("Channels", audioInfo.channelCount.toString())
            InfoRow("Format", audioInfo.audioFormat.toString())
            
            if (audioInfo.isOffloaded != null) {
                InfoRow("Offloaded", audioInfo.isOffloaded.toString(), isHighlight = audioInfo.isOffloaded)
            }
            if (audioInfo.audioSource != null) {
                InfoRow("Source", audioInfo.audioSource.toString())
            }
            if (audioInfo.usage != null) {
                InfoRow("Usage", audioInfo.usage.toString())
            }
            
            audioInfo.routedDevices?.let { devices ->
                Text(
                    "Routed Devices",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
                devices.forEach { device ->
                    Text(
                        "- ${device.name} [${device.type}]",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
