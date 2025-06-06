package app.hyuabot.backend.security

data class JWTTokenInfo(
    val grantType: TokenGrantType = TokenGrantType.BEARER,
    val accessToken: String,
) {
    enum class TokenGrantType(
        val value: String,
    ) {
        BEARER("Bearer"),
    }
}
