package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateShuttleRouteRequest(
    @get:Schema(description = "셔틀버스 노선 이름", example = "DHDD")
    val name: String,
    @get:Schema(description = "셔틀버스 노선 한글 설명", example = "기숙사~한대앞~기숙사 (직행)")
    val descriptionKorean: String,
    @get:Schema(description = "셔틀버스 노선 영어 설명", example = "Dormitory~Station~Dormitory (Direct)")
    val descriptionEnglish: String,
    @get:Schema(description = "셔틀버스 노선 태그", example = "DH")
    val tag: String,
    @get:Schema(description = "셔틀버스 노선 시작 정류장 ID", example = "dormitory_o")
    val startStopID: String,
    @get:Schema(description = "셔틀버스 노선 종료 정류장 ID", example = "dormitory_i")
    val endStopID: String,
)
