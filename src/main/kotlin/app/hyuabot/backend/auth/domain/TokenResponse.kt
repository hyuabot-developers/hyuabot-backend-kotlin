package app.hyuabot.backend.auth.domain

data class TokenResponse(
    val grantType: TokenGrantType = TokenGrantType.BEARER,
    val accessToken: String,
) {
    enum class TokenGrantType(
        val value: String,
    ) {
        BEARER("Bearer"),
    }
}
