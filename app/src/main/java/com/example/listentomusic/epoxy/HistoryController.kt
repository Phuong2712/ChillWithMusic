package com.example.listentomusic.epoxy

import android.content.Context
import android.view.Gravity
import androidx.recyclerview.widget.SnapHelper
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.EpoxyController
import com.bumptech.glide.Glide
import com.example.listentomusic.R
import com.example.listentomusic.databinding.ItemFavoriteBinding
import com.example.listentomusic.databinding.ItemHistoryBinding
import com.example.listentomusic.databinding.ItemLoadingFullBinding
import com.example.listentomusic.databinding.ItemNoFileBinding
import com.example.listentomusic.model.MusicItem
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper

class HistoryController private constructor() : EpoxyController(){
    private lateinit var listMusic : ArrayList<MusicItem>
    private lateinit var context : Context
    private var isDoneMusic = false
    private lateinit var onClickItem:(MusicItem) -> Unit
    private lateinit var onClickFav:(MusicItem) -> Unit

    fun setOnClickFav(onClickItem:(MusicItem) -> Unit){
        this.onClickFav = onClickItem
    }

    fun setListMusic(listMusic : ArrayList<MusicItem>){
        this.listMusic.clear()
        this.listMusic.addAll(listMusic)
        isDoneMusic = true
    }

    override fun buildModels() {
        Carousel.setDefaultGlobalSnapHelperFactory(object : Carousel.SnapHelperFactory(){
            override fun buildSnapHelper(context: Context?): SnapHelper {
                return GravitySnapHelper(Gravity.CENTER)
            }
        })

        if(!isDoneMusic)
            LoadingItemModel().id("loading").addTo(this)
        else{
            for(item in listMusic){
                HistoryItemModel(context, item, onClickItem, onClickFav).id(item.id).addTo(this)
            }
        }

        if(listMusic.isEmpty())
            NoFileItemModel().id("no_file").addTo(this)
    }

    data class HistoryItemModel(
        val context: Context,
        val musicItem: MusicItem,
        val onClickItem:(MusicItem) -> Unit,
        val onClickFav:(MusicItem) -> Unit
    ) : ViewBindingKotlinModel<ItemHistoryBinding>(R.layout.item_history) {
        override fun ItemHistoryBinding.bind() {
            tvFavSongName.text = musicItem.name
            tvFavSongAuthor.text = musicItem.author
            tvFavSongName.isSelected = true
            tvFavSongAuthor.isSelected = true
            iconFavInFragment.setImageResource(if (!musicItem.isFavorite) R.drawable.ic_fav_white else R.drawable.ic_favorite_active)
            Glide.with(context).load(musicItem.image).error(R.drawable.img_error).into(imgSongFav)
            root.setOnClickListener {
                onClickItem(musicItem)
            }
            iconFavInFragment.setOnClickListener {
                onClickFav(musicItem)
            }
        }
    }

    class LoadingItemModel(
    ) : ViewBindingKotlinModel<ItemLoadingFullBinding>(R.layout.item_loading_full) {
        override fun ItemLoadingFullBinding.bind() {}
    }

    class NoFileItemModel(
    ) : ViewBindingKotlinModel<ItemNoFileBinding>(R.layout.item_no_file) {
        override fun ItemNoFileBinding.bind() {
            tvNoFile.text = "Không có lịch sử"
        }
    }

    companion object{
        fun newInstance(context: Context, onClickItem: (MusicItem) -> Unit, onClickFav:(MusicItem) -> Unit) : HistoryController{
            val historyController = HistoryController()
            historyController.context = context
            historyController.listMusic = ArrayList()
            historyController.onClickFav = onClickFav
            historyController.onClickItem = onClickItem
            return historyController
        }
    }
}












