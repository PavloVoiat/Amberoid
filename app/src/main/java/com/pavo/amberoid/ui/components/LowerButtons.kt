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
fun PlaylistButton() {
    Box(
        modifier = Modifier
            .size(35.dp)
            .background(
                color = Color(0xFF5D5A94),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uDB80\uDF5C",

            color = Color.White,
            fontFamily = NerdFont
        )
    }
}

@Composable
fun ShufflePlaylistButton() {
    Box(
        modifier = Modifier
            .size(35.dp)
            .background(
                color = Color(0xFF5D5A94),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uDB81\uDC9F",

            color = Color.White,
            fontFamily = NerdFont
        )
    }
}

@Composable
fun RepeatPlaylistButton() {
    Box(
        modifier = Modifier
            .size(35.dp)
            .background(
                color = Color(0xFF5D5A94),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uDB81\uDD47",

            color = Color.White,
            fontFamily = NerdFont
        )
    }
}

@Composable
fun SettingsButton() {
    Box(
        modifier = Modifier
            .size(35.dp)
            .background(
                color = Color(0xFF5D5A94),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uEB51",

            color = Color.White,
            fontFamily = NerdFont
        )
    }
}