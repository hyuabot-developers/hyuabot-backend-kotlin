package app.hyuabot.backend.database.entity

import app.hyuabot.backend.security.AdminPermission
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.ZonedDateTime

@Entity(name = "user")
@Table(name = "admin_user")
class User(
    @Id
    @Column(name = "user_id", length = 20, nullable = false)
    val userID: String,
    @Column(name = "password", columnDefinition = "bytea")
    var password: ByteArray?,
    @Column(name = "name", length = 20, nullable = false)
    var name: String,
    @Column(name = "email", length = 50, nullable = false)
    var email: String,
    @Column(name = "phone", length = 15, nullable = false)
    var phone: String,
    @Column(name = "active", nullable = false)
    var active: Boolean,
    @Column(name = "auth_version", nullable = false)
    var authVersion: Int = 0,
    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    var deletedAt: ZonedDateTime? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "admin_user_permission",
        joinColumns = [JoinColumn(name = "user_id")],
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 30, nullable = false)
    val permissions: MutableSet<AdminPermission> = mutableSetOf(),
    @OneToMany(mappedBy = "user")
    val refreshToken: MutableList<RefreshToken> = mutableListOf(),
    @OneToMany(mappedBy = "user")
    val notice: MutableList<Notice> = mutableListOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as User
        return userID == other.userID
    }

    override fun hashCode(): Int = userID.hashCode()
}
