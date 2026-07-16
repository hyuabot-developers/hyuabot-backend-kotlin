package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.AdminUserInvitation
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AdminUserInvitationRepository : JpaRepository<AdminUserInvitation, UUID> {
    fun findAllByUserIDAndConsumedAtIsNullAndRevokedAtIsNull(userID: String): List<AdminUserInvitation>

    fun findAllByConsumedAtIsNullAndRevokedAtIsNull(): List<AdminUserInvitation>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select invitation from admin_user_invitation invitation
        where (invitation.userID = :userID or invitation.createdBy = :userID)
          and invitation.consumedAt is null
          and invitation.revokedAt is null
        """,
    )
    fun findAllActiveRelatedForUpdate(
        @Param("userID") userID: String,
    ): List<AdminUserInvitation>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select invitation from admin_user_invitation invitation
        where invitation.tokenHash = :tokenHash
          and invitation.consumedAt is null
          and invitation.revokedAt is null
        """,
    )
    fun findActiveForUpdate(
        @Param("tokenHash") tokenHash: String,
    ): AdminUserInvitation?

    fun findByTokenHashAndConsumedAtIsNullAndRevokedAtIsNull(tokenHash: String): AdminUserInvitation?
}
