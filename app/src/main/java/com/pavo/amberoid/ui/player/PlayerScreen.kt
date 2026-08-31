package com.pavo.amberoid.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.pavo.amberoid.ui.components.TrackTitle
import com.pavo.amberoid.ui.components.VolumeBar
import com.pavo.amberoid.ui.components.VolumeDown
import com.pavo.amberoid.ui.components.VolumeUp
import com.pavo.amberoid.ui.components.WaveformSeekBar

@Composable
fun AmberoidUI(
    viewModel: PlayerViewModel = viewModel()
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val artworkBytes by viewModel.artworkBytes.collectAsStateWithLifecycle()
    val colorScheme by viewModel.colorScheme.collectAsStateWithLifecycle()
    val topColor = colorScheme.backgroundTop
    val bottomColor = colorScheme.backgroundBottom
    val primaryButtonColor = colorScheme.textPrimary
    val secondaryButtonColor = colorScheme.textSecondary
    val volume by viewModel.volume.collectAsStateWithLifecycle()

    val animatedTopColor by animateColorAsState(
        targetValue = topColor,
        animationSpec = tween(800),
        label = "TopColorAnimation"
    )
    val animatedBottomColor by animateColorAsState(
        targetValue = bottomColor,
        animationSpec = tween(800),
        label = "BottomColorAnimation"
    )

    val progressFraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedTopColor,
                        animatedBottomColor
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 25.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrackImage(artworkBytes = artworkBytes)


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
                VolumeDown(
                    color = primaryButtonColor,
                    onClick = { viewModel.decreaseVolume() }
                )

                VolumeBar(
                    color = secondaryButtonColor,
                    onVolumeChange = { newVolume -> viewModel.setVolume(newVolume) },
                    volume = volume
                )

                VolumeUp(
                    color = primaryButtonColor,
                    onClick = { viewModel.increaseVolume() }
                )
            }

            Row(
                modifier = Modifier.size(250.dp, 100.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkipPrevious(
                    onPreviousClick = { viewModel.playPrevious() },
                    color = secondaryButtonColor
                )

                PlayPause(
                    isPlaying = isPlaying,
                    onPlayToggle = { viewModel.togglePlayPause() },
                    color = primaryButtonColor
                )

                SkipNext(
                    onNextClick = { viewModel.playNext() },
                    color = secondaryButtonColor
                )
            }

            Row(
                modifier = Modifier.size(300.dp, 35.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistButton(color = secondaryButtonColor)

                ShufflePlaylistButton(
		    color = secondaryButtonColor,
		    onShuffle = viewModel.shuffle()
		)

                Spacer(modifier = Modifier.width(100.dp))

                RepeatPlaylistButton(
		    color = secondaryButtonColor,
		    onRepeat = viewModel.repeat()
		)

                SettingsButton(color = secondaryButtonColor)
            }
        }
    }
}
