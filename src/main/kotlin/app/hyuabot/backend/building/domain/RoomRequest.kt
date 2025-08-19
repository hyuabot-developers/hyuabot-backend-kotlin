package app.hyuabot.backend.building.domain

import io.swagger.v3.oas.annotations.media.Schema

data class RoomRequest(
    @get:Schema(description = "강의실 번호", example = "101")
    val number: String,
    @get:Schema(description = "강의실 이름", example = "컴퓨터공학과 101호")
    val name: String,
)
