package com.isaev.musicswipe

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyLoginActivityResultContract : ActivityResultContract<AuthorizationRequest, AuthorizationResponse>() {

    override fun createIntent(context: Context, request: AuthorizationRequest): Intent {
        return AuthorizationClient.createLoginActivityIntent(context as? Activity, request)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AuthorizationResponse? {
        return intent?.let { AuthorizationClient.getResponse(resultCode, it) }
    }

}
