package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
    @Column(name = "user_id", length = 20, nullable = false)
    val userID: String,
    @Column(name = "refresh_token", length = 100, nullable = false)
    val refreshToken: String,
    @Column(name = "expired_at", columnDefinition = "timestamptz", nullable = false)
    val expiredAt: ZonedDateTime,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    val createdAt: ZonedDateTime,
    @Column(name = "updated_at", columnDefinition = "timestamptz", nullable = false)
    val updatedAt: ZonedDateTime,
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    val user: User,
)
