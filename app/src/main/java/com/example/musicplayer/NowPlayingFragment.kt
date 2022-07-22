package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.databinding.FragmentNowPlayingBinding

class NowPlayingFragment : Fragment() {

    companion object{
        lateinit var binding: FragmentNowPlayingBinding
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view)
        binding.root.visibility = View.INVISIBLE

        binding.playPauseBtnNp.setOnClickListener {
            if (PlayerActivity.isPlaying) pauseMusic() else playMusic()
        }
        // now playing next button clicked to play next song
        binding.nextBtnNP.setOnClickListener {
            setSongPosition(increment = true)
            PlayerActivity.musicService!!.createMediaPlayer()
            // for loading image from the song album
            Glide.with(this).load(PlayerActivity.musiclistPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
                .into(binding.songImageNP)
            binding.songNameNP.text = PlayerActivity.musiclistPA[PlayerActivity.songPosition].title
            PlayerActivity.musicService!!.showNotification(R.drawable.ic_pause, 1F)
            playMusic()
        }

        // for continuing the song while clicking the same song
        binding.root.setOnClickListener {
            val intent = Intent(requireContext(),PlayerActivity::class.java)
            intent.putExtra("index",PlayerActivity.songPosition)
            intent.putExtra("class","NowPlaying")
            ContextCompat.startActivity(requireContext(),intent,null)
        }
        return view
    }

    // continuing song from player activity to main activity
    override fun onResume() {
        super.onResume()
        if (PlayerActivity.musicService != null){
            binding.root.visibility = View.VISIBLE
            binding.songNameNP.isSelected = true
            // for loading image from the song album
            Glide.with(this).load(PlayerActivity.musiclistPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
                .into(binding.songImageNP)
            binding.songNameNP.text = PlayerActivity.musiclistPA[PlayerActivity.songPosition].title

            if (PlayerActivity.isPlaying) binding.playPauseBtnNp.setImageResource(R.drawable.ic_pause_now_playing)
            else binding.playPauseBtnNp.setImageResource(R.drawable.ic_play_now_playng)

        }
    }

    // play music on now playing
    private fun playMusic(){
        PlayerActivity.isPlaying = true
        PlayerActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNp.setImageResource(R.drawable.ic_pause_now_playing)
        PlayerActivity.musicService!!.showNotification(R.drawable.ic_pause, 1F)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
        PlayerActivity.isPlaying = true
    }

    // pause music on now playing
    private fun pauseMusic(){
        PlayerActivity.isPlaying = false
        PlayerActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNp.setImageResource(R.drawable.ic_play_now_playng)
        PlayerActivity.musicService!!.showNotification(R.drawable.ic_play, 0F)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.ic_play)
        PlayerActivity.isPlaying = false
    }

}