package app.hyuabot.backend.commuteShuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleStopResponse(
    @get:Schema(description = "통학버스 정류장 ID", example = "수내역.롯데백화점공항버스정류장")
    val name: String,
    @get:Schema(description = "통학버스 정류장 한글 이름", example = "분당선 수내역 1.2번 출구 앞")
    val description: String,
    @get:Schema(description = "셔틀버스 정류장 위도", example = "37.123456")
    val latitude: Double,
    @get:Schema(description = "셔틀버스 정류장 경도", example = "127.123456")
    val longitude: Double,
)
