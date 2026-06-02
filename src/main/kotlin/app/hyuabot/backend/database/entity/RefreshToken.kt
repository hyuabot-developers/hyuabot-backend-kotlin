package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.UUID

@Entity(name = "refresh_token")
@Table(name = "auth_refresh_token")
class RefreshToken(
    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    val uuid: UUID,
    @Column(name = "user_id", length = 20, nullable = false)
    var userID: String,
    @Column(name = "refresh_token", length = 200, nullable = false)
    var refreshToken: String,
    @Column(name = "expired_at", columnDefinition = "timestamptz", nullable = false)
    var expiredAt: ZonedDateTime,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    var createdAt: ZonedDateTime,
    @Column(name = "updated_at", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    val user: User?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as RefreshToken
        return uuid == other.uuid
    }

    override fun hashCode(): Int = uuid.hashCode()
}
