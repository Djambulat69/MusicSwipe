package com.isaev.musicswipe.di

import com.isaev.musicswipe.ui.MainActivity
import com.isaev.musicswipe.ui.TracksFragment
import com.isaev.musicswipe.ui.WebViewFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [SpotifyWebApiModule::class, AuthModule::class, UserModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(tracksFragment: TracksFragment)
    fun inject(webViewFragment: WebViewFragment)
}
