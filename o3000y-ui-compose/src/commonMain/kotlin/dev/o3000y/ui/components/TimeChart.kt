package dev.o3000y.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.o3000y.ui.explore.ChartSeries
import dev.o3000y.ui.theme.ChartColors

@Composable
fun TimeChart(series: List<ChartSeries>, label: String) {
    if (series.isEmpty() || series.all { it.data.isEmpty() }) {
        Box(
            Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        return
    }

    val allBuckets = remember(series) {
        series.flatMap { s -> s.data.map { it.bucket } }.distinct().sorted()
    }

    val yRange = remember(series) {
        var min = Double.MAX_VALUE
        var max = Double.MIN_VALUE
        series.forEach { s -> s.data.forEach { min = minOf(min, it.value); max = maxOf(max, it.value) } }
        val pad = (max - min) * 0.1
        (maxOf(0.0, min - pad)) to (max + pad)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        // Legend
        if (series.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Canvas(Modifier.size(10.dp)) {
                            drawRect(ChartColors[s.colorIndex % ChartColors.size])
                        }
                        Text(s.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val padL = 50f
            val padR = 10f
            val padT = 10f
            val padB = 30f
            val cw = size.width - padL - padR
            val ch = size.height - padT - padB
            val (yMin, yMax) = yRange
            val n = allBuckets.size

            fun xPos(i: Int) = padL + if (n <= 1) cw / 2 else (i.toFloat() / (n - 1)) * cw
            fun yPos(v: Double) = padT + ch - ((v - yMin) / (yMax - yMin)).toFloat() * ch

            // Grid lines
            for (tick in 0..4) {
                val v = yMin + (yMax - yMin) * tick / 4.0
                val y = yPos(v)
                drawLine(Color(0xFFE9ECEF), Offset(padL, y), Offset(padL + cw, y), strokeWidth = 1f)
            }

            // Series
            series.forEach { s ->
                val color = ChartColors[s.colorIndex % ChartColors.size]
                val bucketMap = s.data.associateBy { it.bucket }
                val path = Path()
                var started = false
                allBuckets.forEachIndexed { i, b ->
                    val pt = bucketMap[b] ?: return@forEachIndexed
                    val x = xPos(i)
                    val y = yPos(pt.value)
                    if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 2.5f))

                // Dots
                allBuckets.forEachIndexed { i, b ->
                    val pt = bucketMap[b] ?: return@forEachIndexed
                    drawCircle(color, radius = 3f, center = Offset(xPos(i), yPos(pt.value)))
                }
            }
        }
    }
}
