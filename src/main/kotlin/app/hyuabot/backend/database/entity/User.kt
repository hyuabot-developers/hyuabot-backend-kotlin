package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "user")
@Table(name = "admin_user")
class User(
    @Id
    @Column(name = "user_id", length = 20, nullable = false)
    val userID: String,
    @Column(name = "password", columnDefinition = "bytea", nullable = false)
    var password: ByteArray,
    @Column(name = "name", length = 20, nullable = false)
    var name: String,
    @Column(name = "email", length = 50, nullable = false)
    var email: String,
    @Column(name = "phone", length = 15, nullable = false)
    var phone: String,
    @Column(name = "active", nullable = false)
    var active: Boolean,
    @OneToMany(mappedBy = "user")
    val refreshToken: MutableList<RefreshToken> = mutableListOf(),
    @OneToMany(mappedBy = "user")
    val notice: MutableList<Notice> = mutableListOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as User
        return userID == other.userID
    }

    override fun hashCode(): Int = userID.hashCode()
}
