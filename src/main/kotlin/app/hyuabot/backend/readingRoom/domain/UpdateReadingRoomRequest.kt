package app.hyuabot.backend.readingRoom.domain

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateReadingRoomRequest(
    @get:Schema(description = "캠퍼스 ID", example = "1")
    val campusID: Int,
    @get:Schema(description = "열람실 이름", example = "1층 열람실")
    val name: String,
    @get:Schema(description = "열람실 전체 좌석 수", example = "100")
    val total: Int,
    @get:Schema(description = "열람실 현재 활성 좌석 수", example = "50")
    val active: Int,
    @get:Schema(description = "열람실 활성화 상태", example = "true")
    val isActive: Boolean,
    @get:Schema(description = "열람실 예약 가능 여부", example = "true")
    val isReservable: Boolean,
)
