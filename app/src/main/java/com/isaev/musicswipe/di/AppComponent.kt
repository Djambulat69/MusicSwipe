package com.isaev.musicswipe.di

import com.isaev.musicswipe.ui.LogOutDialogFragment
import com.isaev.musicswipe.ui.MainActivity
import com.isaev.musicswipe.ui.TracksFragment
import com.isaev.musicswipe.ui.WebViewFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, SpotifyWebApiModule::class, AuthModule::class, UserModule::class, MediaPlayerModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(tracksFragment: TracksFragment)
    fun inject(webViewFragment: WebViewFragment)
    fun inject(logOutDialogFragment: LogOutDialogFragment)
}
