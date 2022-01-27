package com.isaev.musicswipe.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class UserModule {


    @Singleton
    @Named(USER_PREFS_NAME)
    @Provides
    fun provideUserSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        const val USER_PREFS_NAME = "auth"
    }
}
