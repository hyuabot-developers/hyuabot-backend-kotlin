package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class SubwayRealtimeResponse(
    @get:Schema(description = "역 ID", example = "K251")
    val stationID: String,
    @get:Schema(description = "상행/하행 구분", example = "up")
    val direction: String,
    @get:Schema(description = "도착 순서", example = "1")
    val order: Int,
    @get:Schema(description = "현재 위치", example = "서울역")
    val location: String,
    @get:Schema(description = "남은 역 수", example = "3")
    val stop: Int,
    @get:Schema(description = "남은 시간", example = "00:05:00")
    val time: String,
    @get:Schema(description = "종점 ID", example = "K271")
    val terminalStationID: String,
    @get:Schema(description = "열차 번호", example = "1234")
    val trainNumber: String,
    @get:Schema(description = "업데이트 시간", example = "2023-10-01 12:34:56")
    val updateTime: String,
    @get:Schema(description = "급행 열차 여부", example = "false")
    val isExpress: Boolean,
    @get:Schema(description = "막차 여부", example = "false")
    val isLast: Boolean,
    @get:Schema(description = "운행 상태", example = "99")
    val status: Int,
)
