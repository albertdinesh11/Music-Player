package com.example.musicplayer

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.databinding.MusicViewBinding
import java.io.File


class MusicAdapter(private val context: Context, private var musicList: ArrayList<Music>) : RecyclerView.Adapter<MusicAdapter.MyHolder>(){
    class MyHolder(binding: MusicViewBinding): RecyclerView.ViewHolder(binding.root) {
        val title = binding.songName
        val album = binding.songAlbum
        val image = binding.songImage
        val root = binding.root
        val moreBtnMA = binding.moreBtnMA

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }
    override fun onBindViewHolder(holder:MyHolder,position: Int) {
        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album

        holder.moreBtnMA.setOnClickListener {
            val popupMenus = PopupMenu(context,holder.moreBtnMA)
                popupMenus.inflate(R.menu.more_menu_ma)
                popupMenus.setOnMenuItemClickListener {
                    when(it.itemId){
                        R.id.shareMA ->{
                            val shareIntent = Intent()
                            shareIntent.action = Intent.ACTION_SEND
                            shareIntent.type = "audio/*"
                            shareIntent.putExtra(Intent.EXTRA_STREAM,
                                Uri.parse(musicList[position].path))
                                context.startActivity(Intent.createChooser(shareIntent,"Share Music File!"))
                            true
                        }
                        R.id.deleteMA ->{

                            val dialogBinding = LayoutInflater.from(context).inflate(R.layout.delete_dialoge_sheet,null)
                            val myDialog  = Dialog(context)
                            myDialog.setContentView(dialogBinding)
                            dialogBinding.findViewById<TextView>(R.id.songNameDD).text = musicList[position].title

                            val yesBtnDD = dialogBinding.findViewById<TextView>(R.id.yesDD)
                            yesBtnDD.setOnClickListener {
                                if (PlayerActivity.isPlaying) {
                                    Toast.makeText(
                                        context,
                                        "we can't Delete the current Playing Song",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    myDialog.dismiss()
                                } else {
                                    val file = File(musicList[position].path)
                                    if (file.exists()){
                                        file.delete()
                                    }

                                    musicList.removeAt(position)
                                    notifyItemRemoved(position)
                                    notifyItemRemoved(MainActivity.MusicListMA.size)
                                    myDialog.dismiss()
                                    Toast.makeText(context, "Song Deleted", Toast.LENGTH_SHORT).show()

                                }
                            }
                            val noBtnDD = dialogBinding.findViewById<TextView>(R.id.noDD)
                            noBtnDD.setOnClickListener { myDialog.dismiss() }

                            myDialog.show()


                            true
                        }


                        R.id.propertiesMA ->{

                            val dialogBinding = LayoutInflater.from(context).inflate(R.layout.properties_dialog_sheet,null)
                            val myDialog  = Dialog(context)
                            myDialog.setContentView(dialogBinding)

                            dialogBinding.findViewById<TextView>(R.id.nameDS).text = musicList[position].title
                            dialogBinding.findViewById<TextView>(R.id.authorDS).text = musicList[position].artist
                            dialogBinding.findViewById<TextView>(R.id.durationDS).text = formatDuration(musicList[position].duration)
                            dialogBinding.findViewById<TextView>(R.id.pathDS).text = musicList[position].path
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

        // for loading image from the song album
        Glide.with(context).load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music).centerCrop())
            .into(holder.image)

        // to go to play activity from searching
        holder.root.setOnClickListener {
            when{
                MainActivity.search -> sendIntent(ref = "MusicAdapterSearch", pos = position)
                musicList[position].id == PlayerActivity.nowPlayingId ->
                    sendIntent(ref = "NowPlaying", pos = PlayerActivity.songPosition)
                else -> sendIntent("MusicAdapter", pos = position)
            }

        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun updateMusicList(searchList : ArrayList<Music>){
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }

    private fun sendIntent(ref: String, pos: Int){
        val intent = Intent(context,PlayerActivity::class.java)
        intent.putExtra("index",pos)
        intent.putExtra("class",ref)
        startActivity(context,intent,null)

    }
}