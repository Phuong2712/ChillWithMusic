package com.example.listentomusic.epoxy

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import androidx.recyclerview.widget.SnapHelper
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.EpoxyController
import com.bumptech.glide.Glide
import com.example.listentomusic.R
import com.example.listentomusic.databinding.ItemFavoriteBinding
import com.example.listentomusic.databinding.ItemLoadingBinding
import com.example.listentomusic.databinding.ItemLoadingFullBinding
import com.example.listentomusic.databinding.ItemNoFileBinding
import com.example.listentomusic.model.MusicItem
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper

class FavoriteController private constructor() : EpoxyController(){
    private lateinit var listMusic : ArrayList<MusicItem>
    private lateinit var context : Context
    private lateinit var onClickItem : (MusicItem) -> Unit
    private lateinit var onClickFav : (MusicItem) -> Unit
    private var isDoneFavorite = false

    fun setListMusic(listMusic : ArrayList<MusicItem>){
        this.listMusic.clear()
        this.listMusic.addAll(listMusic)
        isDoneFavorite = true
    }

    override fun buildModels() {
        Carousel.setDefaultGlobalSnapHelperFactory(object : Carousel.SnapHelperFactory(){
            override fun buildSnapHelper(context: Context?): SnapHelper {
                return GravitySnapHelper(Gravity.CENTER)
            }
        })

        if(!isDoneFavorite && listMusic.isEmpty())
            LoadingItemModel().id("loading").addTo(this)
        else {
            for(item in listMusic){
                FavoriteItemModel(context, item, onClickItem, onClickFav).id(item.id).addTo(this)
            }
        }

        if(listMusic.isEmpty())
            NoFileItemModel().id("no_file").addTo(this)

        isDoneFavorite = false
    }

    data class FavoriteItemModel(
        val context: Context,
        val musicItem: MusicItem,
        val onClickFav: (MusicItem) -> Unit,
        val onClickItem: (MusicItem) -> Unit
    ) : ViewBindingKotlinModel<ItemFavoriteBinding>(R.layout.item_favorite) {
        override fun ItemFavoriteBinding.bind() {
            tvFavSongAuthor.text = musicItem.author
            tvFavSongName.text = musicItem.name
            tvFavSongName.isSelected = true
            tvFavSongAuthor.isSelected = true
            Glide.with(context).load(musicItem.image).error(R.drawable.ic_launcher_foreground).into(imgSongFav)
            root.setOnClickListener {
                onClickItem(musicItem)
            }
            iconFavInFragment.setOnClickListener {
                onClickFav(musicItem)
            }
        }
    }

    class LoadingItemModel(
    ) : ViewBindingKotlinModel<ItemLoadingFullBinding>(R.layout.item_loading_full){
        override fun ItemLoadingFullBinding.bind() {
        }
    }

    class NoFileItemModel(
    ) : ViewBindingKotlinModel<ItemNoFileBinding>(R.layout.item_no_file){
        override fun ItemNoFileBinding.bind() {
            tvNoFile.text = "Không có bài hát nào yêu thich"
        }
    }

    companion object{
        fun newInstance(context: Context, onClickItem:(MusicItem) -> Unit, onClickFav:(MusicItem) -> Unit):FavoriteController {
            val favoriteController = FavoriteController()
            favoriteController.context = context
            favoriteController.listMusic = ArrayList()
            favoriteController.onClickFav = onClickFav
            favoriteController.onClickItem = onClickItem
            return favoriteController
        }
    }
}