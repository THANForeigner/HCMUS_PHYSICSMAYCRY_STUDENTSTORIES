package com.example.afinal.logic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.example.afinal.R

class AudioPlayerService : Service() {

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private var mediaPlayer: MediaPlayer? = null
    var isPlaying = false
        private set
    private lateinit var mediaSession: MediaSessionCompat

    var currentAudioUrl: String? = null
    var currentStoryId: String? = null
    private var currentTitle: String = "Select a story"
    private var currentUser: String = "Student Stories"
    private var currentLocationName: String = ""

    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"

        const val EXTRA_AUDIO_URL = "EXTRA_AUDIO_URL"
        const val EXTRA_STORY_ID = "EXTRA_STORY_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_USER = "EXTRA_USER"
        const val EXTRA_LOCATION = "EXTRA_LOCATION"

        const val CHANNEL_ID = "audio_playback_channel"
        const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "AudioPlayerService")
        mediaSession.isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        intent?.getStringExtra(EXTRA_TITLE)?.let { currentTitle = it }
        intent?.getStringExtra(EXTRA_USER)?.let { currentUser = it }
        intent?.getStringExtra(EXTRA_LOCATION)?.let { currentLocationName = it }
        intent?.getStringExtra(EXTRA_STORY_ID)?.let { currentStoryId = it }

        when (action) {
            ACTION_PLAY -> {
                val url = intent?.getStringExtra(EXTRA_AUDIO_URL)
                if (url != null) playAudio(url) else resumeAudio()
            }
            ACTION_PAUSE -> pauseAudio()
            ACTION_RESUME -> resumeAudio()
        }
        return START_STICKY
    }

    fun playAudio(url: String) {
        if (url == currentAudioUrl && mediaPlayer != null) {
            resumeAudio()
            return
        }

        currentAudioUrl = url
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                start()
                this@AudioPlayerService.isPlaying = true
                showPlayerNotification(true)
            }
            setOnCompletionListener {
                this@AudioPlayerService.isPlaying = false
                showPlayerNotification(false)
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.start()
        isPlaying = true
        showPlayerNotification(true)
    }

    fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        showPlayerNotification(false)
    }

    private fun showPlayerNotification(isPlaying: Boolean) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannel(manager)

        currentStoryId?.let { id ->
            manager.cancel(id.hashCode())
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_story_id", currentStoryId)
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, AudioPlayerService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_RESUME
        }
        val pendingToggle = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (currentLocationName.isNotEmpty()) "$currentUser • $currentLocationName" else currentUser

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(currentTitle)
            .setContentText(content)
            .setContentIntent(pendingOpenApp)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setMediaSession(mediaSession.sessionToken))
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                pendingToggle
            )
            .setOngoing(isPlaying)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Audio Playback", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    fun seekTo(position: Int) = mediaPlayer?.seekTo(position)
    fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L
    fun getDuration(): Long = mediaPlayer?.duration?.toLong() ?: 0L

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaSession.release()
    }
}