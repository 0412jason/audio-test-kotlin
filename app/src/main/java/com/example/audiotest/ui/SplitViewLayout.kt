package com.example.audiotest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplitViewLayout(
    title: String,
    initialSplitCount: Int = 1,
    modifier: Modifier = Modifier,
    slotContent: @Composable (Int) -> Unit
) {
    var splitCount by remember { mutableIntStateOf(initialSplitCount) }

    fun increaseSplit() {
        if (splitCount == 1) {
            splitCount = 2
        } else if (splitCount == 2) {
            splitCount = 4
        }
    }

    fun decreaseSplit() {
        if (splitCount == 4) {
            splitCount = 2
        } else if (splitCount == 2) {
            splitCount = 1
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { decreaseSplit() },
                    enabled = splitCount > 1
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease Split")
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = { increaseSplit() },
                    enabled = splitCount < 4
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase Split")
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (splitCount) {
                    1 -> {
                        key(0) { slotContent(0) }
                    }
                    2 -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) { key(0) { slotContent(0) } }
                            VerticalDivider(thickness = 1.dp)
                            Box(modifier = Modifier.weight(1f)) { key(1) { slotContent(1) } }
                        }
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f)) { key(0) { slotContent(0) } }
                                VerticalDivider(thickness = 1.dp)
                                Box(modifier = Modifier.weight(1f)) { key(1) { slotContent(1) } }
                            }
                            HorizontalDivider(thickness = 1.dp)
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f)) { key(2) { slotContent(2) } }
                                VerticalDivider(thickness = 1.dp)
                                Box(modifier = Modifier.weight(1f)) { key(3) { slotContent(3) } }
                            }
                        }
                    }
                }
            }
        }
    }
}
