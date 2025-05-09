package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "user")
@Table(name = "admin_user")
data class User(
    @Id
    @Column(name = "user_id", length = 20)
    val userID: String,
    @Column(name = "password", columnDefinition = "bytea")
    val password: ByteArray,
    @Column(name = "name", length = 20)
    val name: String,
    @Column(name = "email", length = 50)
    val email: String,
    @Column(name = "phone", length = 15)
    val phone: String,
    @Column(name = "active")
    val active: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (active != other.active) return false
        if (userID != other.userID) return false
        if (!password.contentEquals(other.password)) return false
        if (name != other.name) return false
        if (email != other.email) return false
        if (phone != other.phone) return false

        return true
    }

    override fun hashCode(): Int {
        var result = active.hashCode()
        result = 31 * result + userID.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + phone.hashCode()
        return result
    }
}
