package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.ZonedDateTime
import java.util.UUID

@Entity(name = "device_push_token")
@Table(name = "device_push_token")
class DevicePushToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "installation_id", columnDefinition = "uuid", nullable = false)
    var installationId: UUID,
    @Column(name = "platform", length = 16, nullable = false)
    var platform: String,
    @Column(name = "provider", length = 16, nullable = false)
    var provider: String,
    @Column(name = "token", length = 512, nullable = false)
    var token: String,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    var createdAt: ZonedDateTime,
    @Column(name = "updated_at", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as DevicePushToken
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
