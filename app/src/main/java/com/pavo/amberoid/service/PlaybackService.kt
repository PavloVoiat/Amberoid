package com.pavo.amberoid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pavo.amberoid.data.local.PlaybackPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private lateinit var prefs: PlaybackPreferences
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressLogJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startProgressTracking()
            } else {
                stopProgressTracking()
                saveCurrentPosition()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.toLongOrNull()?.let { songId ->
                prefs.saveLastPosition(songId, 0L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PlaybackPreferences(applicationContext)
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

        player = exoPlayer
        exoPlayer.addListener(playerListener)
        
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    private fun startProgressTracking() {
        progressLogJob?.cancel()
        progressLogJob = serviceScope.launch {
            while (true) {
                saveCurrentPosition()
                delay(1000)
            }
        }
    }

    private fun stopProgressTracking() {
        progressLogJob?.cancel()
        progressLogJob = null
    }

    private fun saveCurrentPosition() {
        val p = player ?: return
        val currentMediaItem = p.currentMediaItem
        currentMediaItem?.mediaId?.toLongOrNull()?.let { songId ->
            val pos = p.currentPosition.coerceAtLeast(0L)
            prefs.saveLastPosition(songId, pos)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        stopProgressTracking()
        player?.removeListener(playerListener)
        player?.release()
        player = null

        mediaSession?.run {
            release()
        }
        mediaSession = null

        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "amberoid_playback_channel",
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for media playback"
                setShowBadge(true)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
