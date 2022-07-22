package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlin.random.Random
import kotlin.system.exitProcess

class NotificationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            ApplicationClass.PREVIOUS -> prevNextSong(false, context = context!!)
            ApplicationClass.PLAY -> if (PlayerActivity.isPlaying) pauseMusic() else playMusic()
            ApplicationClass.NEXT -> prevNextSong(true, context = context!!)
            ApplicationClass.EXIT -> {
                PlayerActivity.musicService!!.stopForeground(true)
                PlayerActivity.musicService!!.mediaPlayer!!.release()
                PlayerActivity.musicService = null
                exitProcess(1)
            }
        }
    }

    // play song on notification
    private fun playMusic(){
        PlayerActivity.isPlaying = true
        PlayerActivity.musicService!!.mediaPlayer!!.start()
        PlayerActivity.musicService!!.showNotification(R.drawable.ic_pause, 1F)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
        NowPlayingFragment.binding.playPauseBtnNp.setImageResource(R.drawable.ic_pause_now_playing)
    }

    // pause song on notification
    private fun pauseMusic(){
        PlayerActivity.isPlaying = false
        PlayerActivity.musicService!!.mediaPlayer!!.pause()
        PlayerActivity.musicService!!.showNotification(R.drawable.ic_play, 0F)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_play)
        NowPlayingFragment.binding.playPauseBtnNp.setImageResource(R.drawable.ic_play_now_playng)
    }

    //  previous & next song on notification
    private fun prevNextSong(increment:Boolean,context: Context){
        if (PlayerActivity.shuffle){
            PlayerActivity.songPosition = getRandom(PlayerActivity.musiclistPA.size - 1)
        }
        setSongPosition(increment = increment)
        PlayerActivity.musicService!!.createMediaPlayer()
        // for loading image from the song album
        Glide.with(context).load(PlayerActivity.musiclistPA[PlayerActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
            .into(PlayerActivity.binding.songImgPA)
        PlayerActivity.binding.songNamePA.text = PlayerActivity.musiclistPA[PlayerActivity.songPosition].title
        PlayerActivity.binding.songAuthorPA.text = PlayerActivity.musiclistPA[PlayerActivity.songPosition].artist

        // for loading image from the song album
        Glide.with(context).load(PlayerActivity.musiclistPA[PlayerActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
            .into(NowPlayingFragment.binding.songImageNP)
        NowPlayingFragment.binding.songNameNP.text = PlayerActivity.musiclistPA[PlayerActivity.songPosition].title
        playMusic()
    }

    // for getting random number for shuffle in notification
    private fun getRandom(i: Int): Int {
        return Random.nextInt(i + 1)
    }

}