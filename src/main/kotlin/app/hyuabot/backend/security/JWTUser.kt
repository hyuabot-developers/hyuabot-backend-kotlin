package app.hyuabot.backend.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User

class JWTUser(
    username: String,
    password: String,
    permissions: Set<AdminPermission> = emptySet(),
) : User(
        username,
        password,
        permissions.effectivePermissions().map { SimpleGrantedAuthority(it.name) },
    )
