package com.example.listentomusic.epoxy

import android.content.Context
import android.view.Gravity
import androidx.recyclerview.widget.SnapHelper
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.CarouselModel_
import com.airbnb.epoxy.EpoxyController
import com.bumptech.glide.Glide
import com.example.listentomusic.R
import com.example.listentomusic.databinding.ItemLoadingBinding
import com.example.listentomusic.databinding.ItemMusicBinding
import com.example.listentomusic.databinding.ItemPlaylistBinding
import com.example.listentomusic.model.MusicItem
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper

class MyEpoxyController private constructor() : EpoxyController() {
    private lateinit var listMusic :ArrayList<MusicItem>
    private lateinit var context : Context
    private lateinit var onClickItem:(MusicItem) -> Unit
    private var isFiltering = false

    fun setIsFiltering(isFiltering : Boolean){
        this.isFiltering = isFiltering
    }

    fun setListMusic(listMusic:ArrayList<MusicItem>){
        this.listMusic = listMusic
    }

    override fun buildModels() {
        Carousel.setDefaultGlobalSnapHelperFactory(object : Carousel.SnapHelperFactory(){
            override fun buildSnapHelper(context: Context?) : SnapHelper {
                return GravitySnapHelper(Gravity.CENTER)
            }
        })

        if(isFiltering){
            for(item in listMusic){
                MusicItemModel(context, item, onClickItem).id(item.id).addTo(this)
            }
            return
        }

        val listChild = ArrayList<MusicItem>()
        val listLove = ArrayList<MusicItem>()
        val listRemix = ArrayList<MusicItem>()
        for(item in listMusic){
            if(item.playList == "Lofi chill") listChild.add(item)
            if(item.playList == "Love") listLove.add(item)
            if(item.playList == "Remix") listRemix.add(item)
        }

        PlayListItemModel(context, "Lofi chill").id("title_1").addTo(this)
        if(listChild.isEmpty()){
            LoadingItemModel().id("loading_chill").addTo(this)
        }else{
            CarouselModel_()
                .id("list_1")
                .models(listChild.map {
                    MusicItemModel(context, it, onClickItem).id(it.id)
                })
                .addTo(this)
        }

        PlayListItemModel(context, "Love").id("title_2").addTo(this)
        if(listLove.isEmpty())
            LoadingItemModel().id("loading_love").addTo(this)
        else{
            CarouselModel_()
                .id("list_2")
                .models(listLove.map{
                    MusicItemModel(context, it, onClickItem).id(it.id)
                })
                .addTo(this)
        }

        PlayListItemModel(context, "Remix").id("title_3").addTo(this)
        if(listRemix.isEmpty())
            LoadingItemModel().id("loading_remix").addTo(this)
        else{
            CarouselModel_()
                .id("list_3")
                .models(listLove.map{
                    MusicItemModel(context, it, onClickItem).id(it.id)
                })
                .addTo(this)
        }
    }

    data class MusicItemModel(
        val context: Context,
        val musicItem: MusicItem,
        val onClickItem: (MusicItem) -> Unit
    ) : ViewBindingKotlinModel<ItemMusicBinding>(R.layout.item_music){
        override fun ItemMusicBinding.bind() {
            tvName.text = musicItem.name
            tvAuthor.text = musicItem.author
            tvName.isSelected = true
            tvAuthor.isSelected = true
            Glide.with(context).load(musicItem.image).error(R.drawable.img_error).into(imgSong)

            root.setOnClickListener {
                onClickItem(musicItem)
            }
        }
    }

    data class PlayListItemModel(
        val context: Context,
        val playListName : String
    ) : ViewBindingKotlinModel<ItemPlaylistBinding>(R.layout.item_playlist){
        override fun ItemPlaylistBinding.bind() {
            tvPlaylistName.text = playListName
        }
    }
    
    class LoadingItemModel(
    ) : ViewBindingKotlinModel<ItemLoadingBinding>(R.layout.item_loading){
        override fun ItemLoadingBinding.bind() { }
    }

    // tạo 1 phiên bản mới của lớp đó vs cac tham số khởi tạo
    companion object{
        fun newInstance(context: Context, onClickItem: (MusicItem) -> Unit):MyEpoxyController {
            val myEpoxyController = MyEpoxyController()
            myEpoxyController.context = context
            myEpoxyController.listMusic = ArrayList()
            myEpoxyController.onClickItem = onClickItem
            return myEpoxyController
        }
    }
}











