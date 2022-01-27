package com.isaev.musicswipe

sealed class Token {

    data class AccessToken(
        val token: String
    ) : Token()

    object EmptyToken: Token()
}
