package com.isaev.musicswipe.di

import android.content.Context
import com.isaev.musicswipe.ui.MusicSwipeApp
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope

@Module
class AppModule(
    private val application: MusicSwipeApp
) {

    @Provides
    fun provideAppContext(): Context = application

    @Provides
    fun provideApplicationScope(): CoroutineScope {
        return application.applicationScope
    }

}
