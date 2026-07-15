package app.hyuabot.backend.auth.exception

class InvalidUserInputException(
    val code: String,
) : RuntimeException(code)
