package com.example.listentomusic.model

data class User (
    val id:String? = "",
    val username :String? = "",
    val password:String? = "",
    val name:String = "",
    val image:String = "",
    val historyList: List<MusicItem>? = null,
    val favoriteList: List<MusicItem>? = null,
    val playList: List<PlayList>? = null
)