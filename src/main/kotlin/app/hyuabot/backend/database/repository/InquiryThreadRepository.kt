package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.InquiryThread
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InquiryThreadRepository : JpaRepository<InquiryThread, UUID> {
    fun findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(
        installationId: UUID,
        status: Collection<String>,
    ): InquiryThread?

    fun findByStatusInOrderByLastMessageAtDesc(status: Collection<String>): List<InquiryThread>

    fun findByAssignedAdminUserIdAndStatusInOrderByLastMessageAtDesc(
        adminUserId: String,
        status: Collection<String>,
    ): List<InquiryThread>
}
