package com.pavo.amberoid.ui.player

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import com.google.common.util.concurrent.MoreExecutors
import com.pavo.amberoid.data.model.Song
import com.pavo.amberoid.data.repository.AudioRepository
import com.pavo.amberoid.service.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application

    private val repository = AudioRepository(appContext)

    private var mediaController: MediaController? = null

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val artworkCache = mutableMapOf<Uri, ByteArray?>()

    private val _mediaController = MutableStateFlow<MediaController?>(null)
    private val mediaControllerFlow: StateFlow<MediaController?> = _mediaController.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private val _volume = MutableStateFlow(getCurrentVolumeRatio())
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                _volume.value = getCurrentVolumeRatio()
            }
        }
    }

    init {
        setupMediaController()
        setupVolumeReceiver()
        observeInitialization()
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java)
        )

        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            _mediaController.value = controller

            updateStateFromController(controller)

            if (controller.isPlaying) {
                startProgressUpdate()
            }

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startProgressUpdate()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration.coerceAtLeast(0L)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    val newIndex = controller.currentMediaItemIndex
                    val songList = _songs.value

                    if (newIndex in songList.indices) {
                        _currentSong.value = songList[newIndex]
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    private fun setupVolumeReceiver() {
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(volumeReceiver, filter)
        }
    }

    private fun observeInitialization() {
        viewModelScope.launch {
            combine(songs, mediaControllerFlow) { songList, controller ->
                songList to controller
            }.collect { (songList, controller) ->
                if (songList.isNotEmpty() && controller != null) {
                    syncControllerWithSongs(controller, songList)
                    updateStateFromController(controller)
                }
            }
        }
    }

    private fun updateStateFromController(controller: MediaController) {
        _isPlaying.value = controller.isPlaying
        _duration.value = controller.duration.coerceAtLeast(0L)
        _isShuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode

        val currentIndex = controller.currentMediaItemIndex
        val currentList = _songs.value
        if (currentIndex in currentList.indices) {
            _currentSong.value = currentList[currentIndex]
        }
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (mediaController?.isPlaying == true) {
                _currentPosition.value = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                _duration.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                delay(500.milliseconds)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedSongs = repository.getAudioFiles()
            _songs.value = loadedSongs
            // observeInitialization will handle the sync once controller is available
        }
    }

    private fun syncControllerWithSongs(controller: MediaController, songs: List<Song>) {
        if (songs.isEmpty()) return

        // If the controller has no items or the counts don't match, re-populate
        if (controller.mediaItemCount == 0 || controller.mediaItemCount != songs.size) {
            val mediaItems = songs.map { MediaItem.fromUri(it.contentUri) }
            controller.setMediaItems(mediaItems)
            controller.prepare()
        }

        // Ensure current song is synced
        val currentIndex = controller.currentMediaItemIndex
        if (currentIndex in songs.indices) {
            _currentSong.value = songs[currentIndex]
        }
    }

    fun selectSong(song: Song) {
        val allSongs = _songs.value
        val index = allSongs.indexOf(song)
        if (index == -1) return

        _currentSong.value = song

        mediaController?.let { controller ->
            // Re-sync if necessary before seeking
            if (controller.mediaItemCount != allSongs.size) {
                val mediaItems = allSongs.map { MediaItem.fromUri(it.contentUri) }
                controller.setMediaItems(mediaItems)
                controller.prepare()
            }

            if (index in 0 until controller.mediaItemCount) {
                controller.seekTo(index, 0L)
                controller.prepare()
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        val current = _currentSong.value ?: return

        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE) {
                selectSong(current)
            }
            controller.play()
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun playPrevious() {
        mediaController?.let {
            if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            }
        }
    }

    fun playNext() {
        mediaController?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            }
        }
    }

    fun toggleShuffle() {
        mediaController?.let {
            val newState = !it.shuffleModeEnabled
            it.shuffleModeEnabled = newState
            _isShuffleEnabled.value = newState
        }
    }

    fun toggleRepeatMode() {
        mediaController?.let { controller ->
            val nextMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextMode
            _repeatMode.value = nextMode
        }
    }

    override fun onCleared() {
        mediaController?.release()
        try {
            appContext.unregisterReceiver(volumeReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val artworkBytes: StateFlow<ByteArray?> = _currentSong
        .mapLatest { song ->
            song?.let {
                withContext(Dispatchers.IO) {
                    getArtwork(appContext, it.contentUri)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun getArtwork(context: Context, uri: Uri): ByteArray? {
        if (artworkCache.containsKey(uri)) return artworkCache[uri]

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture
            artworkCache[uri] = bytes
            bytes
        } catch (e: Exception) {
            artworkCache[uri] = null
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
