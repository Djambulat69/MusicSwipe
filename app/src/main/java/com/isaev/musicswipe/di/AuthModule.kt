package com.isaev.musicswipe.di

import android.util.Log
import com.isaev.musicswipe.data.SpotifyAuthApi
import com.isaev.musicswipe.data.SpotifyAuthService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

@Module
class AuthModule {

    @ExperimentalSerializationApi
    @Named("AuthRetrofit")
    @Provides
    fun provideAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor { message -> Log.i(SpotifyAuthService.TAG, message) }
                            .apply { level = HttpLoggingInterceptor.Level.BODY }
                    )
                    .build()
            )
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Singleton
    @Provides
    fun provideAuthService(@Named("AuthRetrofit") retrofit: Retrofit): SpotifyAuthApi {
        return retrofit.create()
    }

    companion object {
        private const val BASE_URL = "https://accounts.spotify.com/"
    }
}
