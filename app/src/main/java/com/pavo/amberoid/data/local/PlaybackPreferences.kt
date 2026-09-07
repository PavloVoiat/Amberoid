package com.pavo.amberoid.data.local

import android.content.Context

class PlaybackPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("player_preferences", Context.MODE_PRIVATE)

    fun saveLastPosition(songId: Long, positionMs: Long) {
        preferences.edit()
            .putLong("last_song_id", songId)
            .putLong("last_position", positionMs)
            .apply()
    }

    fun getLastSongId(): Long = preferences.getLong("last_song_id", -1L)
    fun getLastPosition(): Long = preferences.getLong("last_position", 0L)
}