package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String> {
    fun findByUserID(userID: String): User?

    fun findByUserIDAndActiveIsTrue(userID: String): User?

    fun findByEmail(email: String): User?
}
