package com.isaev.musicswipe.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class UserModule(app: Application) {

    private val context: Context = app

    @Provides
    fun provideAppContext(): Context = context

    @Named(USER_PREFS_NAME)
    @Provides
    fun provideUserSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        const val USER_PREFS_NAME = "auth"
    }
}
