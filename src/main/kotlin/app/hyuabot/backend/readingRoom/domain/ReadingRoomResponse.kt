package app.hyuabot.backend.readingRoom.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ReadingRoomResponse(
    @get:Schema(description = "열람실 ID", example = "101")
    val seq: Int,
    @get:Schema(description = "열람실 이름", example = "1층 열람실")
    val name: String,
    @get:Schema(description = "캠퍼스 ID", example = "1")
    val campusID: Int,
    @get:Schema(description = "열람실 활성화 상태", example = "true")
    val isActive: Boolean,
    @get:Schema(description = "열람실 예약 가능 여부", example = "true")
    val isReservable: Boolean,
    @get:Schema(description = "열람실 전체 좌석 수", example = "100")
    val total: Int,
    @get:Schema(description = "열람실 현재 활성 좌석 수", example = "50")
    val active: Int,
    @get:Schema(description = "열람실 현재 점유 좌석 수", example = "30")
    val occupied: Int,
    @get:Schema(description = "열람실 현재 빈 좌석 수", example = "20")
    val available: Int,
    @get:Schema(description = "업데이트 시간", example = "2023-10-01T12:00:00Z")
    val updatedAt: String,
)
