package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String> {
    fun findByEmail(email: String): User?
}
