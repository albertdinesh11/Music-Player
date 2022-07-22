@file:Suppress("DEPRECATION")

package com.example.musicplayer

import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.MainActivity.Companion.MusicListMA
import com.example.musicplayer.databinding.ActivityPlayerBinding
import kotlin.random.Random

class PlayerActivity : AppCompatActivity(),ServiceConnection, MediaPlayer.OnCompletionListener {


    companion object{
        lateinit var musiclistPA:ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var musicService: MusicService? = null
        lateinit var binding: ActivityPlayerBinding
        var repeat: Boolean = false
        var shuffle = false
        var nowPlayingId: String = ""
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // for status bar items light and dark theme
        WindowCompat.setDecorFitsSystemWindows(window,false)
        val wic = WindowCompat.getInsetsController(window, window.decorView)
        wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        wic.isAppearanceLightStatusBars = true
        window.statusBarColor = Color.TRANSPARENT


        if (intent.data?.scheme.contentEquals("content")){
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)
            musiclistPA = ArrayList()
            musiclistPA.add(getMusicDetails(intent.data!!))

            Glide.with(this).load(getImgArt(musiclistPA[songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
                .into(binding.songImgPA)
            binding.songNamePA.text = musiclistPA[songPosition].title
        }else initializeLayout()


        binding.playPauseBtn.setOnClickListener {
            if (isPlaying) pauseMusic()
            else playMusic()
        }
        binding.previousBtnPA.setOnClickListener { prevNextSong(increment = false) }
        binding.nextBtnPA.setOnClickListener { prevNextSong(increment = true) }

        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) musicService!!.mediaPlayer!!.seekTo(progress)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) = Unit
            override fun onStopTrackingTouch(p0: SeekBar?)  = Unit
        })

        // for go back
        binding.backBtnPA.setOnClickListener { finish() }

        // for setting song shuffle
        binding.shuffleBtn.setOnClickListener {
            if (!shuffle){
                shuffle = true
                binding.shuffleBtn.setColorFilter(ContextCompat.getColor(this,R.color.secondary_color))
                Toast.makeText(baseContext, "Shuffled", Toast.LENGTH_SHORT).show()

            }else{
                shuffle = false
                binding.shuffleBtn.setColorFilter(ContextCompat.getColor(this,R.color.off_color))
                Toast.makeText(baseContext, "Shuffle off", Toast.LENGTH_SHORT).show()
            }
        }


        // for setting song repeat on or Off
        binding.repeatBtnPA.setOnClickListener {
            if (!repeat){
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.secondary_color))
                Toast.makeText(baseContext, "Repeat on", Toast.LENGTH_SHORT).show()
            }else{
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.off_color))
                Toast.makeText(baseContext, "Repeat off", Toast.LENGTH_SHORT).show()
            }
        }

        //  more option
        binding.moreBtnPA.setOnClickListener { popupMenus(it) }

    }

    // for getting songs from local storage
    private fun getMusicDetails(contentUri: Uri ): Music {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION)
            cursor = this.contentResolver.query(contentUri,projection,null,null,null)
            val dataColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            cursor!!.moveToFirst()
            val path = dataColumn?.let { cursor.getString(it) }
            val duration = durationColumn?.let { cursor.getLong(it) }!!
            return Music(id = "Unknown", title = path.toString(), album = "Unknown", artist = "Unknown", duration = duration,
            artUri = "Unknown", path = path.toString())
        }finally {
            cursor?.close()
        }
    }

    // dialog for more itemes
    private fun popupMenus(view: View){
        val popupMenus = PopupMenu(baseContext,view)
        popupMenus.inflate(R.menu.more_menu_pa)
        popupMenus.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.share ->{
                   val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.type = "audio/*"
                    shareIntent.putExtra(Intent.EXTRA_STREAM,Uri.parse(musiclistPA[songPosition].path))
                    startActivity(Intent.createChooser(shareIntent,"Share Music File!"))
                    true
                }
                // Equalizer
                R.id.equalizer ->{
                 try {
                     val equIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                     equIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
                     equIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME,baseContext.packageName)
                     equIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE,AudioEffect.CONTENT_TYPE_MUSIC)
                     startActivityForResult(equIntent,13)
                 }catch (e:Exception){
                     Toast.makeText(this, "Equalizer Feature Not Supported!", Toast.LENGTH_SHORT).show()}
                    true
                }

                R.id.properties ->{

                    val dialogBinding = LayoutInflater.from(this).inflate(R.layout.properties_dialog_sheet,null)
                    val myDialog  = Dialog(this)
                    myDialog.setContentView(dialogBinding)

                    dialogBinding.findViewById<TextView>(R.id.nameDS).text = musiclistPA[songPosition].title
                    dialogBinding.findViewById<TextView>(R.id.authorDS).text = musiclistPA[songPosition].artist
                    dialogBinding.findViewById<TextView>(R.id.durationDS).text = formatDuration(
                        musiclistPA[songPosition].duration)
                    dialogBinding.findViewById<TextView>(R.id.pathDS).text = musiclistPA[songPosition].path

                    val okBtn = dialogBinding.findViewById<TextView>(R.id.okDs)
                    okBtn.setOnClickListener { myDialog.dismiss() }

                    myDialog.show()
                    true
                }
                else -> true
            }
        }
        popupMenus.show()
        val popup = PopupMenu::class.java.getDeclaredField("mPopup")
        popup.isAccessible = true
        val menu = popup.get(popupMenus)
        menu.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
            .invoke(menu,true)
    }

    // for setting song informations to ui layout
    private fun setLayout(){
        // for loading image from the song album
        Glide.with(applicationContext).load(musiclistPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musiclistPA[songPosition].title
        binding.songAuthorPA.text = musiclistPA[songPosition].artist

        if (repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.secondary_color))

    }

    // for creating media player to play songs
    private fun createMediaPlayer(){
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musiclistPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            musicService!!.mediaPlayer!!.start()
            isPlaying = true
            binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
            musicService!!.showNotification(R.drawable.ic_pause, 1F)
            binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musiclistPA[songPosition].id
        }catch (e: Exception){return}
    }

    // for initializing layout to render songs
    private fun initializeLayout(){
        songPosition = intent.getIntExtra("index",0)
        when(intent.getStringExtra("class")) {
            "NowPlaying" ->{
                setLayout()
                binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if (isPlaying) binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
                else binding.playPauseBtn.setImageResource(R.drawable.ic_play)
                if (shuffle){
                    binding.shuffleBtn.setColorFilter(ContextCompat.getColor(this,R.color.secondary_color))
                }else{
                    binding.shuffleBtn.setColorFilter(ContextCompat.getColor(this,R.color.off_color))
                }
            }
            "MusicAdapterSearch" ->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musiclistPA = ArrayList()
                musiclistPA.addAll(MainActivity.musicListSearch)
                setLayout()
            }
            "MusicAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musiclistPA = ArrayList()
                musiclistPA.addAll(MusicListMA)
                setLayout()
            }

        }
    }

    /// for playing music
    private fun playMusic(){
        binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
        musicService!!.showNotification(R.drawable.ic_pause,1F)
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
    }

    // for pausing music
    private fun pauseMusic(){
        binding.playPauseBtn.setImageResource(R.drawable.ic_play)
        musicService!!.showNotification(R.drawable.ic_play, 0F)
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
    }

    // for previous and next song
    private fun prevNextSong(increment:Boolean){
        if (increment){
            if (shuffle){
                songPosition = getRandom(musiclistPA.size - 1)
            }
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        }else{
            if (shuffle){
                songPosition = getRandom(musiclistPA.size - 1)
            }
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }

    // for geting random numbers for shuffle
    private fun getRandom(i: Int): Int {
            return Random.nextInt(i + 1)
    }


    // for connecting music services
    override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
        if (musicService == null){
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(musicService, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        }
        createMediaPlayer()
        musicService!!.seekBarSetup()

    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        musicService = null
    }

    // for playing next song after completion
    override fun onCompletion(p0: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        // for change image & song name in now playing after completion
        NowPlayingFragment.binding.songNameNP.isSelected = true
        Glide.with(applicationContext).load(musiclistPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
            .into(NowPlayingFragment.binding.songImageNP)
            NowPlayingFragment.binding.songNameNP.text = musiclistPA[songPosition].title
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    override fun onDestroy() {
        super.onDestroy()
        if (musiclistPA[songPosition].id == "unknown" && !isPlaying) exitApplication()
    }
}