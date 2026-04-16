package com.example.audiotest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun WaveformDisplay(
    amplitude: Double,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    capacity: Int = 100
) {
    // Keep a rolling buffer of amplitudes
    val amplitudes = remember { mutableStateListOf<Float>() }

    LaunchedEffect(amplitude) {
        amplitudes.add(amplitude.toFloat())
        if (amplitudes.size > capacity) {
            amplitudes.removeAt(0)
        }
    }

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(120.dp)) {
        
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        if (amplitudes.isEmpty()) {
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            return@Canvas
        }
        
        val pointWidth = width / capacity.toFloat()
        val path = Path()
        
        var currentX = width - (amplitudes.size * pointWidth)
        if (currentX < 0f) currentX = 0f
        
        path.moveTo(currentX, centerY)
        
        for (i in amplitudes.indices) {
            val amp = amplitudes[i].coerceIn(0f, 1f)
            val yOffset = (amp * height / 2f)
            
            // Draw a bar for each point or a continuous line
            // We'll draw continuous curve connecting the points
            val x = currentX + (i * pointWidth)
            val y = centerY - yOffset
            
            val nextX = currentX + ((i+1) * pointWidth)
            val nextY = if (i + 1 < amplitudes.size) centerY - (amplitudes[i+1].coerceIn(0f, 1f) * height / 2f) else y
            
            // Mirror vertically
            drawLine(
                color = color,
                start = Offset(x, centerY - yOffset),
                end = Offset(x, centerY + yOffset),
                strokeWidth = pointWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
        
        // Draw baseline
        drawLine(
            color = color.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
