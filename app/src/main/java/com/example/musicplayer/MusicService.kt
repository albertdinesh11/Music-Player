package com.example.musicplayer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat

class MusicService:Service(), AudioManager.OnAudioFocusChangeListener {
    private var myBinder = MyBinder()
    var mediaPlayer:MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var runnable: Runnable
    lateinit var audioManager: AudioManager

    override fun onBind(p0: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext,"My Music")
        return myBinder
    }
    inner class MyBinder:Binder(){
        fun currentService():MusicService{
            return this@MusicService
        }
    }



    // for showing notification
    fun showNotification(playPauseBtn: Int, playbackSpeed: Float){
        val  notificationToMainIntent = Intent(baseContext,MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this,0,notificationToMainIntent,0)

        val prevIntent = Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(baseContext,0,prevIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val playIntent = Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(baseContext,0,playIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(baseContext,0,nextIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val exitIntent = Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(baseContext,0,exitIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val imgArt = getImgArt(PlayerActivity.musiclistPA[PlayerActivity.songPosition].path)
        val image = if (imgArt != null){
            BitmapFactory.decodeByteArray(imgArt,0,imgArt.size)
        }else{
            BitmapFactory.decodeResource(resources,R.drawable.ic_music)
        }

            val notification = NotificationCompat.Builder(baseContext,ApplicationClass.CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(PlayerActivity.musiclistPA[PlayerActivity.songPosition].title)
                .setContentText(PlayerActivity.musiclistPA[PlayerActivity.songPosition].artist)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(image)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_previous,"Previous",prevPendingIntent)
                .addAction(playPauseBtn,"Play",playPendingIntent)
                .addAction(R.drawable.ic_next,"next",nextPendingIntent)
                .addAction(R.drawable.ic_remove_circle,"exit",exitPendingIntent)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                mediaSession.setMetadata(MediaMetadataCompat.Builder()
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong())
                    .build())
                mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer!!.currentPosition.toLong(), playbackSpeed)
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                    .build())
            }
                startForeground(13, notification)
    }

    // for creating media player to play song in notifcation
    fun createMediaPlayer(){
        try {
            if (PlayerActivity.musicService!!.mediaPlayer == null) PlayerActivity.musicService!!.mediaPlayer = MediaPlayer()
            PlayerActivity.musicService!!.mediaPlayer!!.reset()
            PlayerActivity.musicService!!.mediaPlayer!!.setDataSource(PlayerActivity.musiclistPA[PlayerActivity.songPosition].path)
            PlayerActivity.musicService!!.mediaPlayer!!.prepare()
            PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
            PlayerActivity.musicService!!.showNotification(R.drawable.ic_pause, 0F)
            PlayerActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            PlayerActivity.binding.tvSeekBarEnd.text = formatDuration(mediaPlayer!!.duration.toLong())
            PlayerActivity.binding.seekBarPA.progress = 0
            PlayerActivity.binding.seekBarPA.max = mediaPlayer!!.duration
            PlayerActivity.nowPlayingId = PlayerActivity.musiclistPA[PlayerActivity.songPosition].id
        }catch (e: Exception){return}
    }

    // for adding seekbar in notification
    fun seekBarSetup(){
        runnable = Runnable {
            PlayerActivity.binding.tvSeekBarStart.text =
                formatDuration(mediaPlayer!!.currentPosition.toLong())
            PlayerActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable,0)
    }

    // for pausing while other source of audio is in active
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange){
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->{
                // pause music
                PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_play)
                NowPlayingFragment.binding.playPauseBtnNp.setImageResource(R.drawable.ic_play)
                showNotification(R.drawable.ic_play, 0F)
                PlayerActivity.isPlaying = false
                mediaPlayer!!.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS ->{
                // pause music
                PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_play)
                NowPlayingFragment.binding.playPauseBtnNp.setImageResource(R.drawable.ic_play)
                showNotification(R.drawable.ic_play, 0F)
                PlayerActivity.isPlaying = false
                mediaPlayer!!.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT ->{
                // play music
                PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
                NowPlayingFragment.binding.playPauseBtnNp.setImageResource(R.drawable.ic_pause)
                showNotification(R.drawable.ic_pause, 0F)
                PlayerActivity.isPlaying = true
                mediaPlayer!!.start()
            }
            AudioManager.AUDIOFOCUS_REQUEST_FAILED ->{
                mediaPlayer!!.stop()
            }
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED ->{
                // play music
                PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
                NowPlayingFragment.binding.playPauseBtnNp.setImageResource(R.drawable.ic_pause)
                showNotification(R.drawable.ic_pause, 0F)
                PlayerActivity.isPlaying = true
                mediaPlayer!!.start()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->{
                // pause music
                PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_play)
                NowPlayingFragment.binding.playPauseBtnNp.setImageResource(R.drawable.ic_play)
                showNotification(R.drawable.ic_play, 0F)
                PlayerActivity.isPlaying = false
                mediaPlayer!!.pause()
            }

        }

    }


}