package com.pavo.amberoid.ui.player

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import com.pavo.amberoid.data.model.Song
import com.pavo.amberoid.data.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

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

    private val audioManager = getApplication<Application>()
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private val _volume = MutableStateFlow(getCurrentVolumeRatio())
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val volumeReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                _volume.value = getCurrentVolumeRatio()
            }
        }
    }

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

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        getApplication<Application>().registerReceiver(volumeReceiver, filter)
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (player.isPlaying) {
                _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                _duration.value = player.duration.coerceAtLeast(0L)
                delay(500.milliseconds)
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
        player.release()
        try {
            getApplication<Application>().unregisterReceiver(volumeReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun playPrevious() {
        val songList = _songs.value
        val current = _currentSong.value ?: return
        if (songList.isEmpty()) return

        val currentIndex = songList.indexOf(current)
        val previousIndex = if (currentIndex - 1 < 0) songList.size - 1 else currentIndex - 1

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

    val artworkBytes: StateFlow<ByteArray?> = _currentSong
        .map { song ->
            song?.let {
                withContext(Dispatchers.IO) {
                    getArtwork(getApplication(), it.contentUri)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun getArtwork(context: Context, uri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    data class CoverPalette(
        val primary: Color,
        val secondary: Color,
        val backgroundTop: Color,
        val backgroundBottom: Color,
        val surface: Color,
        val textPrimary: Color,
        val textSecondary: Color,
        val accent: Color,
        val allSwatches: List<Color>
    )

    private suspend fun extractFullPalette(bytes: ByteArray?): CoverPalette {
        val defaultPrimary = Color(0xFFBB86FC)
        val defaultBackgroundTop = Color(0xFF1E1E2C)
        val defaultBackgroundBottom = Color(0xFF0F0F1A)

        if (bytes == null) {
            return CoverPalette(
                primary = defaultPrimary,
                secondary = Color(0xFF03DAC6),
                backgroundTop = defaultBackgroundTop,
                backgroundBottom = defaultBackgroundBottom,
                surface = Color(0xFF2D2D3F),
                textPrimary = Color.White,
                textSecondary = Color.LightGray,
                accent = defaultPrimary,
                allSwatches = emptyList()
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext extractFullPalette(null)

                val palette = Palette.from(bitmap)
                    .maximumColorCount(32)
                    .generate()

                val vibrant = palette.vibrantSwatch
                val darkVibrant = palette.darkVibrantSwatch
                val lightVibrant = palette.lightVibrantSwatch
                val muted = palette.mutedSwatch
                val darkMuted = palette.darkMutedSwatch
                val dominant = palette.dominantSwatch

                val primaryColor = vibrant?.rgb?.let { Color(it) }
                    ?: dominant?.rgb?.let { Color(it) }
                    ?: defaultPrimary

                val backgroundTop = darkVibrant?.rgb?.let { Color(it) }
                    ?: darkMuted?.rgb?.let { Color(it) }
                    ?: defaultBackgroundTop

                val backgroundBottom = dominant?.rgb?.let { Color(it) }
                    ?: defaultBackgroundBottom

                val bodyTextColor = dominant?.bodyTextColor?.let { Color(it) } ?: Color.White
                val titleTextColor = dominant?.titleTextColor?.let { Color(it) } ?: Color.White

                val extractedSwatches = palette.swatches
                    .sortedByDescending { it.population }
                    .take(10)
                    .map { Color(it.rgb) }

                CoverPalette(
                    primary = primaryColor,
                    secondary = lightVibrant?.rgb?.let { Color(it) } ?: primaryColor,
                    backgroundTop = backgroundTop,
                    backgroundBottom = backgroundBottom,
                    surface = darkMuted?.rgb?.let { Color(it) } ?: Color(0xFF252535),
                    textPrimary = titleTextColor,
                    textSecondary = bodyTextColor,
                    accent = muted?.rgb?.let { Color(it) } ?: primaryColor,
                    allSwatches = extractedSwatches
                )
            } catch (e: Exception) {
                extractFullPalette(null)
            }
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    val colorScheme: StateFlow<CoverPalette> = artworkBytes
        .mapLatest { bytes -> extractFullPalette(bytes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CoverPalette(
                primary = Color(0xFFBB86FC),
                secondary = Color(0xFF03DAC6),
                backgroundTop = Color(0xFF1E1E2C),
                backgroundBottom = Color(0xFF0F0F1A),
                surface = Color(0xFF2D2D3F),
                textPrimary = Color.White,
                textSecondary = Color.LightGray,
                accent = Color(0xFFBB86FC),
                allSwatches = emptyList()
            )
        )

    private fun getCurrentVolumeRatio(): Float {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) current.toFloat() / maxVolume else 0f
    }

    fun setVolume(newVolumeRatio: Float) {
        val clampedRatio = newVolumeRatio.coerceIn(0f, 1f)
        _volume.value = clampedRatio
        val targetVolume = (clampedRatio * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }

    fun increaseVolume(step: Float = 0.1f) {
        setVolume(_volume.value + step)
    }

    fun decreaseVolume(step: Float = 0.1f) {
        setVolume(_volume.value - step)
    }
}
