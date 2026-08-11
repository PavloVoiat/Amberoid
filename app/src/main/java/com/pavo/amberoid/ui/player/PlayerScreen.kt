package com.pavo.amberoid.ui.player

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pavo.amberoid.gradientBrush
import com.pavo.amberoid.ui.components.ArtistName
import com.pavo.amberoid.ui.components.PlayPause
import com.pavo.amberoid.ui.components.PlayedTrackTime
import com.pavo.amberoid.ui.components.PlaylistButton
import com.pavo.amberoid.ui.components.RepeatPlaylistButton
import com.pavo.amberoid.ui.components.SettingsButton
import com.pavo.amberoid.ui.components.ShufflePlaylistButton
import com.pavo.amberoid.ui.components.SkipNext
import com.pavo.amberoid.ui.components.SkipPrevious
import com.pavo.amberoid.ui.components.TrackAlbum
import com.pavo.amberoid.ui.components.TrackImage
import com.pavo.amberoid.ui.components.TrackLength
import com.pavo.amberoid.ui.components.TrackSeekBar
import com.pavo.amberoid.ui.components.TrackTitle
import com.pavo.amberoid.ui.components.VolumeBar
import com.pavo.amberoid.ui.components.VolumeDown
import com.pavo.amberoid.ui.components.VolumeUp
import com.pavo.amberoid.ui.components.WaveformSeekBar
import kotlinx.coroutines.flow.compose

@Composable
fun AmberoidUI(
    viewModel: PlayerViewModel = viewModel()
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    val progressFraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(top = 40.dp, bottom = 25.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackImage()


        Spacer(modifier = Modifier.height(50.dp))


        TrackTitle(title = currentSong?.title ?: "No Track")

        ArtistName(artist = currentSong?.artist ?: "Unknown Artist")

        TrackAlbum()


        Spacer(modifier = Modifier.height(50.dp))


        WaveformSeekBar(
            progressFraction = progressFraction,
            songId = currentSong?.id ?: 0L,
            onSeek = { fraction ->
                val targetMs = (fraction * duration).toLong()
                viewModel.seekTo(targetMs)
            },
            modifier = Modifier.padding(horizontal = 30.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.size(300.dp, 17.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PlayedTrackTime(positionMs = currentPosition)

            TrackLength(positionMs = currentPosition, durationMs = duration)
        }


        Spacer(modifier = Modifier.height(20.dp))


        Row(
            modifier = Modifier.size(300.dp, 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VolumeDown()

            VolumeBar()

            VolumeUp()
        }

        Row(
            modifier = Modifier.size(250.dp, 100.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkipPrevious(onPreviousClick = { viewModel.playPrevious() })

            PlayPause(
                isPlaying = isPlaying,
                onPlayToggle = { viewModel.togglePlayPause() }
            )

            SkipNext(onNextClick = { viewModel.playNext() })
        }

        Row(
            modifier = Modifier.size(300.dp, 35.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistButton()

            ShufflePlaylistButton()

            Spacer(modifier = Modifier.width(100.dp))

            RepeatPlaylistButton()

            SettingsButton()
        }
    }
}