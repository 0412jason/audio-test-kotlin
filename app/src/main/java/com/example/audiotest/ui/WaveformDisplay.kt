package com.example.audiotest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun WaveformDisplay(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Int = 60
) {
    val bgColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(height.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(bgColor)) {

        val width = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f

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

        val pointCount = amplitudes.size
        val gapRatio = 0.3f
        val barWidth = (width / pointCount) * (1 - gapRatio)
        val step = width / pointCount
        val minBarHalfHeight = 0.1.dp.toPx()

        for (i in amplitudes.indices) {
            val rawAmp = amplitudes[i].coerceIn(0f, 1f)
            // Use sqrt for better sensitivity to small fluctuations
            val amp = kotlin.math.sqrt(rawAmp)
            val halfHeight = (amp * centerY).coerceIn(minBarHalfHeight, centerY)
            val x = i * step + step / 2f

            drawLine(
                color = color,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
