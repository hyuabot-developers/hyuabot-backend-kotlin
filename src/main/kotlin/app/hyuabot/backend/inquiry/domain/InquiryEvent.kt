package app.hyuabot.backend.inquiry.domain

import app.hyuabot.backend.database.entity.InquiryMessage

data class InquiryEvent(
    val kind: String,
    val threadId: String,
    val installationId: String,
    val message: MessageResponse? = null,
    val reader: String? = null,
    val status: String? = null,
)

fun InquiryMessage.toMessageResponse(): MessageResponse =
    MessageResponse(
        id = id!!,
        senderType = senderType,
        body = body,
        readAt = readAt?.toString(),
        createdAt = createdAt.toString(),
    )
