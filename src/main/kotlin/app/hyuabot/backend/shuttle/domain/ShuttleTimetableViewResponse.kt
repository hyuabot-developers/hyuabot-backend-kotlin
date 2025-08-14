package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleTimetableViewResponse(
    @get:Schema(description = "셔틀버스 시간표 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "셔틀버스 운행 기간 종류", example = "semester")
    val period: String,
    @get:Schema(description = "셔틀버스 시간표 평일/주말 구분", example = "true")
    val isWeekdays: Boolean,
    @get:Schema(description = "셔틀버스 노선 ID", example = "DHDD")
    val routeName: String,
    @get:Schema(description = "셔틀버스 정류장 ID", example = "dormitory_o")
    val stopName: String,
    @get:Schema(description = "셔틀버스 노선 태그", example = "DH")
    val routeTag: String,
    @get:Schema(description = "셔틀버스 출발 시간", example = "08:00:00")
    val departureTime: String,
    @get:Schema(description = "셔틀버스 출발 정류장 그룹", example = "STATION")
    val destinationGroup: String,
)
