package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.InquiryMessage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InquiryMessageRepository : JpaRepository<InquiryMessage, Long> {
    fun findByThreadIdOrderByCreatedAtAsc(threadId: UUID): List<InquiryMessage>

    fun findByThreadIdAndIdGreaterThanOrderByCreatedAtAsc(
        threadId: UUID,
        id: Long,
    ): List<InquiryMessage>

    fun findByThreadIdAndSenderTypeAndReadAtIsNull(
        threadId: UUID,
        senderType: String,
    ): List<InquiryMessage>
}
