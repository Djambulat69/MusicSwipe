package com.isaev.musicswipe

import android.app.Application

class MusicSwipeApp : Application() {

    init {
        instance = this
    }

    companion object {
        lateinit var instance: MusicSwipeApp
    }
}
