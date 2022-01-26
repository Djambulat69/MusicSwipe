package com.isaev.musicswipe

import android.app.Application
import com.isaev.musicswipe.di.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MusicSwipeApp : Application() {

    val applicationScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    val daggerComponent: AppComponent =
        DaggerAppComponent.builder()
            .authModule(AuthModule())
            .spotifyWebApiModule(SpotifyWebApiModule())
            .userModule(UserModule(this))
            .build()


    override fun onTerminate() {
        applicationScope.cancel("Application onTerminate")
        super.onTerminate()
    }
}
