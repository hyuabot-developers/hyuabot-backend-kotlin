package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.ZonedDateTime
import java.util.UUID

@Entity(name = "admin_user_invitation")
@Table(name = "admin_user_invitation")
class AdminUserInvitation(
    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    val uuid: UUID,
    @Column(name = "user_id", length = 20, nullable = false)
    val userID: String,
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", length = 64, columnDefinition = "char(64)", nullable = false, unique = true)
    val tokenHash: String,
    @Column(name = "created_by", length = 20, nullable = false)
    val createdBy: String,
    @Column(name = "expires_at", columnDefinition = "timestamptz", nullable = false)
    val expiresAt: ZonedDateTime,
    @Column(name = "consumed_at", columnDefinition = "timestamptz")
    var consumedAt: ZonedDateTime? = null,
    @Column(name = "revoked_at", columnDefinition = "timestamptz")
    var revokedAt: ZonedDateTime? = null,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    val createdAt: ZonedDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AdminUserInvitation
        return uuid == other.uuid
    }

    override fun hashCode(): Int = uuid.hashCode()
}
