package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateShuttleRouteRequest(
    @get:Schema(description = "셔틀버스 노선 한글 설명", example = "기숙사 - 중앙도서관")
    val descriptionKorean: String,
    @get:Schema(description = "셔틀버스 노선 영어 설명", example = "Dormitory - Central Library")
    val descriptionEnglish: String,
    @get:Schema(description = "셔틀버스 노선 태그", example = "DH")
    val tag: String,
    @get:Schema(description = "셔틀버스 노선 시작 정류장 ID", example = "dormitory_o")
    val startStopID: String,
    @get:Schema(description = "셔틀버스 노선 종료 정류장 ID", example = "dormitory_i")
    val endStopID: String,
)
