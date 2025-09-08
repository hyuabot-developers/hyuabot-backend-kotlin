package app.hyuabot.backend.commuteShuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleTimetableRequest(
    @get:Schema(description = "통학버스 정류장 ID", example = "수내역.롯데백화점공항버스정류장")
    val stopID: String,
    @get:Schema(description = "통학버스 노선 이름", example = "4")
    val routeName: String,
    @get:Schema(description = "경유 순서", example = "1")
    val order: Int,
    @get:Schema(description = "출발 시간 (HH:mm:ss)", example = "08:30:00")
    val time: String,
)
