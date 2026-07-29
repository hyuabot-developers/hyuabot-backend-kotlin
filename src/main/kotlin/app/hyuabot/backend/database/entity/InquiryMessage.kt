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

@Entity(name = "inquiry_message")
@Table(name = "inquiry_message")
class InquiryMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "thread_id", columnDefinition = "uuid", nullable = false)
    var threadId: UUID,
    @Column(name = "sender_type", length = 8, nullable = false)
    var senderType: String,
    @Column(name = "sender_admin_user_id", length = 20)
    var senderAdminUserId: String? = null,
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    var body: String,
    @Column(name = "read_at", columnDefinition = "timestamptz")
    var readAt: ZonedDateTime? = null,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    var createdAt: ZonedDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as InquiryMessage
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
