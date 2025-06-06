package app.hyuabot.backend.security

import org.springframework.security.core.userdetails.User

class JWTUser(
    username: String,
    password: String,
) : User(username, password, listOf())
