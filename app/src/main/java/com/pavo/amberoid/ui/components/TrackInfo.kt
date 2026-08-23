package com.pavo.amberoid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@Composable
fun TrackImage() {
    Box(
        modifier = Modifier
            .size(300.dp)
            .background(
                color = Color(0xFF2D2784),
                shape = RoundedCornerShape(10)
            )
    )
}

@Composable
fun TrackTitle(title: String) {
    Text(
        text = title,

        fontSize = 7.em,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .width(300.dp)
            .basicMarquee()
    )
}

@Composable
fun ArtistName(artist: String) {
    Text(
        text = artist,

        fontSize = 5.em,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .width(300.dp)
            .basicMarquee()
    )
}

@Composable
fun TrackAlbum() {
    Text(
        text = "Track Album",

        fontSize = 4.em,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .width(300.dp)
            .basicMarquee()
    )
}