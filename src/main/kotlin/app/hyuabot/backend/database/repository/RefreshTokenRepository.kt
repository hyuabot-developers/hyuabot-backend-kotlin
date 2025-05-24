package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByUserID(userID: String): RefreshToken?
}
