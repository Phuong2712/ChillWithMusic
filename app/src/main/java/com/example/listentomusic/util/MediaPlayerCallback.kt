package com.example.listentomusic.util

interface MediaPlayerCallback {
    fun onPrepared ()
    fun onFailed()
    fun onDone()
}