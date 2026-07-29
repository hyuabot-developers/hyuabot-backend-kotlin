package app.hyuabot.backend.inquiry

import app.hyuabot.backend.database.entity.InquiryMessage
import app.hyuabot.backend.database.entity.InquiryThread
import app.hyuabot.backend.database.repository.InquiryMessageRepository
import app.hyuabot.backend.database.repository.InquiryThreadRepository
import app.hyuabot.backend.inquiry.exception.EmptyInquiryMessageException
import app.hyuabot.backend.inquiry.exception.InquiryThreadForbiddenException
import app.hyuabot.backend.inquiry.exception.InquiryThreadNotFoundException
import app.hyuabot.backend.inquiry.exception.InvalidInquiryStatusException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class InquiryService(
    private val threadRepository: InquiryThreadRepository,
    private val messageRepository: InquiryMessageRepository,
) {
    private fun now(): ZonedDateTime = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)

    private fun getThreadOrThrow(threadId: UUID): InquiryThread =
        threadRepository.findById(threadId).orElseThrow { InquiryThreadNotFoundException() }

    private fun getOwnedThreadOrThrow(
        threadId: UUID,
        installationId: UUID,
    ): InquiryThread {
        val thread = getThreadOrThrow(threadId)
        if (thread.installationId != installationId) {
            throw InquiryThreadForbiddenException()
        }
        return thread
    }

    @Transactional
    fun openOrGetActiveThread(
        installationId: UUID,
        platform: String,
        appVersion: String?,
        subject: String?,
        contactEmail: String?,
        entryScreen: String?,
        entryScreenName: String?,
    ): InquiryThread {
        val timestamp = now()
        threadRepository
            .findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, ACTIVE_STATUSES)
            ?.let { existing ->
                // 다른 화면에서 다시 문의를 시작하면 진입 화면을 갱신하고 타임라인에 SYSTEM 메시지를 남긴다.
                if (entryScreen != null && entryScreen != existing.entryScreen) {
                    existing.entryScreen = entryScreen
                    existing.entryScreenName = entryScreenName
                    existing.updatedAt = timestamp
                    threadRepository.save(existing)
                    messageRepository.save(
                        InquiryMessage(
                            threadId = existing.id,
                            senderType = "SYSTEM",
                            body = entryScreenSystemMessage(entryScreenName ?: entryScreen),
                            createdAt = timestamp,
                        ),
                    )
                }
                return existing
            }
        return threadRepository.save(
            InquiryThread(
                id = UUID.randomUUID(),
                installationId = installationId,
                platform = platform,
                appVersion = appVersion,
                status = "OPEN",
                subject = subject,
                contactEmail = contactEmail,
                entryScreen = entryScreen,
                entryScreenName = entryScreenName,
                assignedAdminUserId = null,
                lastMessageAt = null,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    fun getActiveThread(installationId: UUID): InquiryThread? =
        threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, ACTIVE_STATUSES)

    fun getMessages(
        threadId: UUID,
        installationId: UUID,
        after: Long?,
    ): List<InquiryMessage> {
        getOwnedThreadOrThrow(threadId, installationId)
        return if (after != null) {
            messageRepository.findByThreadIdAndIdGreaterThanOrderByCreatedAtAsc(threadId, after)
        } else {
            messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)
        }
    }

    @Transactional
    fun sendUserMessage(
        threadId: UUID,
        installationId: UUID,
        body: String,
    ): InquiryMessage {
        if (body.isBlank()) {
            throw EmptyInquiryMessageException()
        }
        val thread = getOwnedThreadOrThrow(threadId, installationId)
        val timestamp = now()
        val message =
            messageRepository.save(
                InquiryMessage(
                    threadId = threadId,
                    senderType = "USER",
                    body = body,
                    createdAt = timestamp,
                ),
            )
        thread.lastMessageAt = timestamp
        thread.updatedAt = timestamp
        threadRepository.save(thread)
        return message
    }

    @Transactional
    fun markReadByUser(
        threadId: UUID,
        installationId: UUID,
    ) {
        getOwnedThreadOrThrow(threadId, installationId)
        val timestamp = now()
        messageRepository.findByThreadIdAndSenderTypeAndReadAtIsNull(threadId, "ADMIN").forEach {
            it.readAt = timestamp
        }
    }

    fun adminListThreads(assignedAdminUserId: String?): List<InquiryThread> =
        if (assignedAdminUserId != null) {
            threadRepository.findByAssignedAdminUserIdAndStatusInOrderByLastMessageAtDesc(assignedAdminUserId, ACTIVE_STATUSES)
        } else {
            threadRepository.findByStatusInOrderByLastMessageAtDesc(ACTIVE_STATUSES)
        }

    fun adminGetThread(threadId: UUID): InquiryThread = getThreadOrThrow(threadId)

    fun getMessagesForAdmin(threadId: UUID): List<InquiryMessage> = messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)

    @Transactional
    fun adminReply(
        threadId: UUID,
        adminUserId: String,
        body: String,
    ): InquiryMessage {
        if (body.isBlank()) {
            throw EmptyInquiryMessageException()
        }
        val thread = getThreadOrThrow(threadId)
        val timestamp = now()
        val message =
            messageRepository.save(
                InquiryMessage(
                    threadId = threadId,
                    senderType = "ADMIN",
                    senderAdminUserId = adminUserId,
                    body = body,
                    createdAt = timestamp,
                ),
            )
        thread.lastMessageAt = timestamp
        thread.updatedAt = timestamp
        threadRepository.save(thread)
        return message
    }

    @Transactional
    fun adminMarkRead(threadId: UUID) {
        getThreadOrThrow(threadId)
        val timestamp = now()
        messageRepository.findByThreadIdAndSenderTypeAndReadAtIsNull(threadId, "USER").forEach {
            it.readAt = timestamp
        }
    }

    @Transactional
    fun adminUpdateThread(
        threadId: UUID,
        status: String?,
        assignedAdminUserId: String?,
    ): InquiryThread {
        val thread = getThreadOrThrow(threadId)
        if (status != null) {
            if (!ACTIVE_STATUSES.contains(status)) {
                throw InvalidInquiryStatusException()
            }
            thread.status = status
        }
        if (assignedAdminUserId != null) {
            thread.assignedAdminUserId = assignedAdminUserId
        }
        thread.updatedAt = now()
        return threadRepository.save(thread)
    }

    fun adminCloseThread(threadId: UUID) {
        val thread = getThreadOrThrow(threadId)
        threadRepository.delete(thread)
    }

    companion object {
        val ACTIVE_STATUSES = listOf("OPEN", "PENDING")

        fun entryScreenSystemMessage(screen: String): String = "사용자가 '$screen' 화면에서 문의를 시작했습니다."
    }
}
