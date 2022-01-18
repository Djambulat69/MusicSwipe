package com.isaev.musicswipe

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MusicSwipeApp : Application() {

    val applicationScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        instance = this
    }

    override fun onTerminate() {
        applicationScope.cancel("Application onTerminate")
        super.onTerminate()
    }

    companion object {
        lateinit var instance: MusicSwipeApp
    }
}
