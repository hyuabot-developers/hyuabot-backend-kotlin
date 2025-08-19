package app.hyuabot.backend.building.domain

import io.swagger.v3.oas.annotations.media.Schema

data class RoomResponse(
    @get:Schema(description = "강의실 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "소속 건물 이름", example = "제1공학관")
    val buildingName: String,
    @get:Schema(description = "강의실 번호", example = "101")
    val number: String,
    @get:Schema(description = "강의실 이름", example = "컴퓨터공학과 101호")
    val name: String,
)
