package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.InquiryThread
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InquiryThreadRepository : JpaRepository<InquiryThread, UUID> {
    fun findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(
        installationId: UUID,
        status: Collection<String>,
    ): InquiryThread?

    fun findByStatusInAndLastMessageAtNotNullOrderByLastMessageAtDesc(status: Collection<String>): List<InquiryThread>

    fun findByAssignedAdminUserIdAndStatusInAndLastMessageAtNotNullOrderByLastMessageAtDesc(
        adminUserId: String,
        status: Collection<String>,
    ): List<InquiryThread>
}
