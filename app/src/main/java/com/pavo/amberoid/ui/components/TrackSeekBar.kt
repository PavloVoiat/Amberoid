package com.pavo.amberoid.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun WaveformSeekBar(
    progressFraction: Float,
    songId: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 50),
        label = "WaveformProgress"
    )

    val barHeights = remember(songId) {
        val random = Random(songId.toInt())
        val barCount = 45

        List(barCount) { index ->
            0.25f + random.nextFloat() * 0.75f
        }
    }

    val activeColor = Color.White
    val inactiveColor = Color(0x66FFFFFF)

    Canvas(
        modifier = modifier
            .size(300.dp, 50.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newFraction)
                }
            }
            .pointerInput(songId) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(newFraction)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val barCount = barHeights.size
        val gap = 6f
        val barWidth = (width - (gap * (barCount - 1))) / barCount

        barHeights.forEachIndexed { index, heightFactor ->
            val left = index * (barWidth + gap)
            val barHeight = height * heightFactor
            val top = (height - barHeight) / 2f

            val barCenterFraction = (left + barWidth / 2f) / width
            val isPlayed = barCenterFraction <= animatedProgress
            val color = if (isPlayed) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth /2)
            )
        }
    }
}

@Composable
fun TrackSeekBar() {
    Box(
        modifier = Modifier
            .size(300.dp, 50.dp)
            .background(
                color = Color(0xFF7C80B5),
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
fun PlayedTrackTime(positionMs: Long) {
    Text(
        text = formatTime(positionMs),

        fontSize = 3.em,
        fontWeight = FontWeight.Medium,
        color = Color.White
    )
}

@Composable
fun TrackLength(positionMs: Long, durationMs: Long) {
    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
    Text(
        text = "-${formatTime(remainingMs)}",

        fontSize = 3.em,
        fontWeight = FontWeight.Medium,
        color = Color.White
    )
}