package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.UUID

@Entity(name = "refresh_token")
@Table(name = "auth_refresh_token")
data class RefreshToken(
    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    val uuid: UUID,
    @Column(name = "user_id", length = 20)
    val userID: String,
    @Column(name = "refresh_token", length = 100)
    val refreshToken: String,
    @Column(name = "expired_at", columnDefinition = "timestamptz")
    val expiredAt: ZonedDateTime,
    @Column(name = "created_at", columnDefinition = "timestamptz")
    val createdAt: ZonedDateTime,
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    val updatedAt: ZonedDateTime,
)
