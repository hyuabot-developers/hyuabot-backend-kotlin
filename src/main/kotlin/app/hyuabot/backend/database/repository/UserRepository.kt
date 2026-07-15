package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, String> {
    fun findByUserID(userID: String): User?

    fun findByUserIDAndActiveIsTrue(userID: String): User?

    fun findByEmail(email: String): User?

    fun findAllByOrderByNameAscUserIDAsc(): List<User>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select user from user user order by user.name asc, user.userID asc
        """,
    )
    fun findAllForPermissionUpdate(): List<User>
}
