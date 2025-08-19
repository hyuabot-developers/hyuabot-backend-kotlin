package app.hyuabot.backend.readingRoom.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateReadingRoomRequest(
    @get:Schema(description = "캠퍼스 ID", example = "1")
    val campusID: Int,
    @get:Schema(description = "열람실 ID", example = "101")
    val id: Int,
    @get:Schema(description = "열람실 이름", example = "1층 열람실")
    val name: String,
    @get:Schema(description = "전체 좌석 수", example = "100")
    val total: Int,
)
