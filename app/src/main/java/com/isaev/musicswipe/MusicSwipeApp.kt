package com.isaev.musicswipe

import android.app.Application
import com.isaev.musicswipe.di.AppComponent
import com.isaev.musicswipe.di.DaggerAppComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MusicSwipeApp : Application() {

    val applicationScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    val daggerComponent: AppComponent = DaggerAppComponent.create()

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
