package com.pavo.amberoid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.pavo.amberoid.NerdFont

@Composable
fun SkipPrevious(
    onPreviousClick: () -> Unit,
    color: Color
) {
    IconButton(
        modifier = Modifier
            .size(50.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(25.dp)
            ),
        onClick = onPreviousClick
    ) {
        Text(
            text = "\uDB81\uDCAE",

            color = Color.White,
            fontFamily = NerdFont,
            fontSize = 5.em
        )
    }
}

@Composable
fun SkipNext(
    onNextClick: () -> Unit,
    color: Color
) {
    IconButton(
        modifier = Modifier
            .size(50.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(25.dp)
            ),
        onClick = onNextClick
    ) {
        Text(
            text = "\uDB81\uDCAD",

            color = Color.White,
            fontFamily = NerdFont,
            fontSize = 5.em
        )
    }
}

@Composable
fun PlayPause(
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    color: Color
) {
    IconButton(
        modifier = Modifier
            .size(75.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(50)
            ),
        onClick = onPlayToggle
    ) {
        Text(
            text = if (isPlaying) "\uDB80\uDFE4" else "\uF04B",

            color = Color.White,
            fontFamily = NerdFont,
            fontSize = if (isPlaying) 8.em else 6.em,
            modifier = Modifier
                .padding(start = if (isPlaying) 0.dp else 3.dp)
        )
    }
}