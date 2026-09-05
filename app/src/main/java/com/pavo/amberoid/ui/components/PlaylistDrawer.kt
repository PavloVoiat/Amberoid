package com.pavo.amberoid.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pavo.amberoid.NerdFont
import com.pavo.amberoid.data.model.Song
import com.pavo.amberoid.ui.player.PlayerViewModel

@Composable
fun PlaylistDrawer(
    songs: List<Song>,
    currentSong: Song?,
    onSongClick: (Song) -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    viewModel: PlayerViewModel = viewModel()
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp),
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = secondaryColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 16.dp, top = 8.dp),

                fontSize = 5.em,
                fontWeight = FontWeight.Bold,
                color = Color.White,

                text = "Playlist"
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(songs) { song ->
                    val isSelected = song.id == currentSong?.id
                    PlaylistItem(
                        song = song,
                        isSelected = isSelected,
                        onClick = { onSongClick(song) },
                        primaryColor = primaryColor,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    viewModel: PlayerViewModel
) {
    val context = LocalContext.current

    val artworkBytes = remember(song.contentUri) {
        viewModel.getArtwork(context, song.contentUri)
    }

    val backgroundColor = if (isSelected) {
        primaryColor
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        Color.White
    } else {
        Color.White.copy(0.7f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (artworkBytes != null) {
                AsyncImage(
                    model = artworkBytes,
                    contentDescription = "Track Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "\uDB80\uDF84",
                    fontSize = 20.sp,
                    fontFamily = NerdFont,
                    color = Color.Blue,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                fontSize = 3.em,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                fontSize = 2.em,
                color = textColor.copy(0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}