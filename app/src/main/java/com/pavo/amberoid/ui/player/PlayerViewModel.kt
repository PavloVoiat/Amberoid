package com.pavo.amberoid.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pavo.amberoid.data.model.Song
import com.pavo.amberoid.data.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioRepository(application)

    private val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdate()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
            }
        })
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (player.isPlaying) {
                _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                _duration.value = player.duration.coerceAtLeast(0L)
                delay(500)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedSongs = repository.getAudioFiles()
            _songs.value = loadedSongs
            if (loadedSongs.isNotEmpty() && _currentSong.value == null) {
                withContext(Dispatchers.Main) {
                    selectSong(loadedSongs.first())
                }
            }
        }
    }

    fun selectSong(song: Song) {
        _currentSong.value = song
        val mediaItem = MediaItem.fromUri(song.contentUri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun togglePlayPause() {
        val current = _currentSong.value ?: return

        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) {
                selectSong(current)
            }
            player.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }

    fun playPrevious() {
        val songList = _songs.value
        val current = _currentSong.value ?: return
        if (songList.isEmpty()) return

        val currentIndex = songList.indexOf(current)
        val previousIndex = if (currentIndex  - 1 < 0) songList.size - 1 else currentIndex - 1

        selectSong(songList[previousIndex])
        player.play()
    }

    fun playNext() {
        val songList = _songs.value
        val current = _currentSong.value ?: return
        if (songList.isEmpty()) return

        val currentIndex = songList.indexOf(current)
        val nextIndex = (currentIndex + 1) % songList.size

        selectSong(songList[nextIndex])
        player.play()
    }
}