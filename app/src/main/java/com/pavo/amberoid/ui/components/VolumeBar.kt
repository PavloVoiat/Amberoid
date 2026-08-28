package com.pavo.amberoid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pavo.amberoid.NerdFont

@Composable
fun VolumeDown(color: Color) {
    Box(
        modifier = Modifier
            .size(25.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uDB81\uDD80",

            color = Color.White,
            fontFamily = NerdFont
        )
    }
}

@Composable
fun VolumeUp(color: Color) {
    Box(
        modifier = Modifier
            .size(25.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uDB81\uDD7E",

            color = Color.White,
            fontFamily = NerdFont
        )
    }
}

@Composable
fun VolumeBar(color: Color) {
    Box(
        modifier = Modifier
            .size(200.dp, 15.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(50)
            )
    )
}