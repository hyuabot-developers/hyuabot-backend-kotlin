package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.ZonedDateTime
import java.util.UUID

@Entity(name = "inquiry_thread")
@Table(name = "inquiry_thread")
class InquiryThread(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    val id: UUID,
    @Column(name = "installation_id", columnDefinition = "uuid", nullable = false)
    var installationId: UUID,
    @Column(name = "platform", length = 16, nullable = false)
    var platform: String,
    @Column(name = "app_version", length = 32)
    var appVersion: String? = null,
    @Column(name = "status", length = 16, nullable = false)
    var status: String = "OPEN",
    @Column(name = "subject", length = 200)
    var subject: String? = null,
    @Column(name = "contact_email", length = 255)
    var contactEmail: String? = null,
    @Column(name = "entry_screen", length = 64)
    var entryScreen: String? = null,
    @Column(name = "entry_screen_name", length = 120)
    var entryScreenName: String? = null,
    @Column(name = "assigned_admin_user_id", length = 20)
    var assignedAdminUserId: String? = null,
    @Column(name = "last_message_at", columnDefinition = "timestamptz")
    var lastMessageAt: ZonedDateTime? = null,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    var createdAt: ZonedDateTime,
    @Column(name = "updated_at", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as InquiryThread
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
