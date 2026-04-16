package com.example.audiotest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class TestType { None, Playback, Record, VoIP }


@Composable
fun TestSlot(viewModel: AudioViewModel, instanceId: Int) {
    var selectedType by remember { mutableStateOf(TestType.None) }

    val options = mapOf(
        "None" to TestType.None.ordinal,
        "Playback" to TestType.Playback.ordinal,
        "Record" to TestType.Record.ordinal,
        "VoIP" to TestType.VoIP.ordinal
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(4.dp)) {
            DropdownSelector(
                label = "Test Mode",
                options = options,
                selectedValue = selectedType.ordinal,
                onValueChange = { selectedType = TestType.values()[it] }
            )
        }
        HorizontalDivider(thickness = 1.dp)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedType) {
                TestType.Playback -> PlaybackScreen(viewModel = viewModel, instanceId = instanceId)
                TestType.Record -> RecordScreen(viewModel = viewModel, instanceId = instanceId)
                TestType.VoIP -> VoipScreen(viewModel = viewModel, instanceId = instanceId)
                TestType.None -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Select Test Mode", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
