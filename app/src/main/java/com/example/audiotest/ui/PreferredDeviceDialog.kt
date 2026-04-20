package com.example.audiotest.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.audiotest.audio.AudioEngine

@Composable
fun PreferredDeviceDialog(
    strategyMap: Map<String, Int>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedStrategy by remember { mutableIntStateOf(-1) }
    var selectedDeviceId by remember { mutableStateOf<Int?>(null) }
    
    var outputDevices by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        outputDevices = AudioEngine.deviceManager.getAudioDevices(true)
    }

    val strategyEntries = buildMap {
        put("Select Strategy", -1)
        putAll(strategyMap.entries.sortedBy { it.value }.associate {
            "${it.key} (${it.value})" to it.value
        })
    }

    val deviceEntries = buildMap {
        put("Select Device", -1)
        outputDevices.forEach { device ->
            val id = device["id"] as Int
            val name = device["name"] as String
            val type = device["type"] as String
            put("$id - $name - $type", id)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Preferred Device Strategy") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DropdownSelector(
                    label = "Strategy",
                    options = strategyEntries,
                    selectedValue = selectedStrategy,
                    onValueChange = { selectedStrategy = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DropdownSelector(
                    label = "Audio Device",
                    options = deviceEntries,
                    selectedValue = selectedDeviceId ?: -1,
                    onValueChange = { selectedDeviceId = if (it == -1) null else it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStrategy != -1 && selectedDeviceId != null) {
                        val success = AudioEngine.deviceManager.setPreferredDeviceForStrategy(selectedStrategy, selectedDeviceId!!)
                        val msg = if (success) "Successfully set preferred device" else "Failed to set preferred device (Check Permissions or API level)"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Please select both strategy and device", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Set Preferred")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        if (selectedStrategy != -1) {
                            val success = AudioEngine.deviceManager.removePreferredDeviceForStrategy(selectedStrategy)
                            val msg = if (success) "Successfully removed preferred device" else "Failed to remove preferred device"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Please select a strategy first", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Remove")
                }
                TextButton(onClick = onDismissRequest) {
                    Text("Close")
                }
            }
        }
    )
}
