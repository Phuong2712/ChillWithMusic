package com.example.listentomusic.model

data class PlayList(
    val id:Int,
    val playListName:String,
    val items:List<MusicItem>? = null,
)