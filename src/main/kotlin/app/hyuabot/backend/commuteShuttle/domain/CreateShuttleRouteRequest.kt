package app.hyuabot.backend.commuteShuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateShuttleRouteRequest(
    @get:Schema(description = "통학버스 노선 이름", example = "4")
    val name: String,
    @get:Schema(description = "통학버스 노선 한글 설명", example = "천호/잠실/성남/서현")
    val descriptionKorean: String,
    @get:Schema(description = "통학버스 노선 영어 설명", example = "Cheonho/Jamsil/Seongnam/Seohyeon")
    val descriptionEnglish: String,
)
