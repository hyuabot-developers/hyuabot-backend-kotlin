package app.hyuabot.backend.inquiry.domain

import io.swagger.v3.oas.annotations.media.Schema

data class OpenThreadRequest(
    @get:Schema(description = "문의 제목", example = "버스 도착 정보 오류")
    val subject: String? = null,
    @get:Schema(description = "답변 받을 이메일", example = "user@example.com")
    val contactEmail: String? = null,
    @get:Schema(description = "문의를 시작한 화면 라우트 id", example = "shuttle.realtime")
    val entryScreen: String? = null,
    @get:Schema(description = "문의를 시작한 화면 표시명", example = "셔틀 실시간")
    val entryScreenName: String? = null,
)

data class SendMessageRequest(
    @get:Schema(description = "메시지 본문", example = "안녕하세요, 문의드립니다.")
    val body: String,
)

data class MessageResponse(
    @get:Schema(description = "메시지 ID", example = "1")
    val id: Long,
    @get:Schema(description = "발신자 유형", example = "USER")
    val senderType: String,
    @get:Schema(description = "메시지 본문", example = "안녕하세요, 문의드립니다.")
    val body: String,
    @get:Schema(description = "읽은 시각(ISO-8601)", example = "2026-07-29T12:00:00+09:00")
    val readAt: String?,
    @get:Schema(description = "생성 시각(ISO-8601)", example = "2026-07-29T12:00:00+09:00")
    val createdAt: String,
)

data class MessageListResponse(
    @get:Schema(description = "메시지 목록")
    val result: List<MessageResponse>,
)

data class ThreadResponse(
    @get:Schema(description = "스레드 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,
    @get:Schema(description = "스레드 상태", example = "OPEN")
    val status: String,
    @get:Schema(description = "문의 제목", example = "버스 도착 정보 오류")
    val subject: String?,
    @get:Schema(description = "답변 받을 이메일", example = "user@example.com")
    val contactEmail: String?,
    @get:Schema(description = "문의를 시작한 화면 라우트 id", example = "shuttle.realtime")
    val entryScreen: String?,
    @get:Schema(description = "문의를 시작한 화면 표시명", example = "셔틀 실시간")
    val entryScreenName: String?,
    @get:Schema(description = "마지막 메시지 시각(ISO-8601)", example = "2026-07-29T12:00:00+09:00")
    val lastMessageAt: String?,
    @get:Schema(description = "생성 시각(ISO-8601)", example = "2026-07-29T12:00:00+09:00")
    val createdAt: String,
)

data class AdminThreadResponse(
    @get:Schema(description = "스레드 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,
    @get:Schema(description = "설치 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val installationId: String,
    @get:Schema(description = "플랫폼", example = "iOS")
    val platform: String,
    @get:Schema(description = "스레드 상태", example = "OPEN")
    val status: String,
    @get:Schema(description = "문의 제목", example = "버스 도착 정보 오류")
    val subject: String?,
    @get:Schema(description = "답변 받을 이메일", example = "user@example.com")
    val contactEmail: String?,
    @get:Schema(description = "배정된 관리자 ID", example = "admin")
    val assignedAdminUserId: String?,
    @get:Schema(description = "문의를 시작한 화면 라우트 id", example = "shuttle.realtime")
    val entryScreen: String?,
    @get:Schema(description = "문의를 시작한 화면 표시명", example = "셔틀 실시간")
    val entryScreenName: String?,
    @get:Schema(description = "마지막 메시지 시각(ISO-8601)", example = "2026-07-29T12:00:00+09:00")
    val lastMessageAt: String?,
    @get:Schema(description = "생성 시각(ISO-8601)", example = "2026-07-29T12:00:00+09:00")
    val createdAt: String,
)

data class AdminThreadListResponse(
    @get:Schema(description = "스레드 목록")
    val result: List<AdminThreadResponse>,
)

data class PatchThreadRequest(
    @get:Schema(description = "변경할 상태(OPEN/PENDING)", example = "PENDING")
    val status: String? = null,
    @get:Schema(description = "배정할 관리자 ID", example = "admin")
    val assignedAdminUserId: String? = null,
)
