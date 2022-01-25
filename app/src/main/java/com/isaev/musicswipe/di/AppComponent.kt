package com.isaev.musicswipe.di

import com.isaev.musicswipe.MainActivity
import com.isaev.musicswipe.TracksFragment
import com.isaev.musicswipe.WebViewFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [SpotifyWebApiModule::class, AuthModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(tracksFragment: TracksFragment)
    fun inject(webViewFragment: WebViewFragment)
}
