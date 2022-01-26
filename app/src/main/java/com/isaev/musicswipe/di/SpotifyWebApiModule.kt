package com.isaev.musicswipe.di

import android.util.Log
import com.isaev.musicswipe.data.AuthInterceptor
import com.isaev.musicswipe.data.SpotifyRepository
import com.isaev.musicswipe.data.SpotifyWebApi
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
class SpotifyWebApiModule {

    @Provides
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @ExperimentalSerializationApi
    @Named("SpotifyWebApiRetrofit")
    @Provides
    fun provideSpotifyWebApiRetrofit(json: Json, authInterceptor: AuthInterceptor): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(
                        HttpLoggingInterceptor { message ->
                            Log.i(SpotifyRepository.TAG, message)
                        }.apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                    .build()
            )
            .addConverterFactory(json.asConverterFactory(("application/json").toMediaType()))
            .build()
    }

    @Singleton
    @Provides
    fun provideSpotifyWebApi(@Named("SpotifyWebApiRetrofit") retrofit: Retrofit): SpotifyWebApi {
        return retrofit.create()
    }

    companion object {
        private const val BASE_URL = "https://api.spotify.com/v1/"
    }

}
