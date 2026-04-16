package com.example.audiotest.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class WindowConfig(val id: Int, var type: String)

@Composable
fun MultiTestScreen(viewModel: AudioViewModel, modifier: Modifier = Modifier) {
    val windows = remember { mutableStateListOf<WindowConfig>() }
    var nextId by remember { mutableStateOf(100) }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Button(onClick = { windows.add(WindowConfig(nextId++, "Playback")) }, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Add Playback")
                Spacer(Modifier.width(4.dp))
                Text("Add Playback")
            }
            Button(onClick = { windows.add(WindowConfig(nextId++, "Record")) }, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Add Record")
                Spacer(Modifier.width(4.dp))
                Text("Add Record")
            }
            Button(onClick = { windows.clear() }, modifier = Modifier.padding(4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear All")
                Spacer(Modifier.width(4.dp))
                Text("Clear All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (windows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Add a module to begin testing multiple audio streams.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                windows.forEachIndexed { index, window ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { windows.removeAt(index) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            
                            Box(modifier = Modifier.heightIn(max = 600.dp)) {
                                if (window.type == "Playback") {
                                    PlaybackScreen(viewModel = viewModel, instanceId = window.id)
                                } else {
                                    RecordScreen(viewModel = viewModel, instanceId = window.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
