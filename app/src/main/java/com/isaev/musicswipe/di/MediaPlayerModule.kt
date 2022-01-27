package com.isaev.musicswipe.di

import android.media.AudioAttributes
import android.media.MediaPlayer
import dagger.Module
import dagger.Provides

@Module
class MediaPlayerModule {

    @Provides
    fun provideMediaPlayer(): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }
}
