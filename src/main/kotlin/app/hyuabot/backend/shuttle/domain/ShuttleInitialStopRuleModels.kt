package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

data class ShuttleGeoPoint(
    @get:Schema(description = "위도", example = "37.2964")
    val latitude: Double,
    @get:Schema(description = "경도", example = "126.8352")
    val longitude: Double,
)

data class ShuttleInitialStopRuleRequest(
    @get:Schema(description = "관리자용 규칙 이름", example = "ERICA 캠퍼스 평일 오전")
    val name: String,
    @get:Schema(description = "학기 유형", example = "semester")
    val periodType: String,
    @get:Schema(description = "평일 시간표 적용 여부", example = "true")
    val weekday: Boolean,
    @get:Schema(description = "적용 시작 시간. 종료 시간과 함께 비우면 종일", example = "07:00:00")
    val startTime: LocalTime?,
    @get:Schema(description = "적용 종료 시간. 시작 시간보다 이르면 자정을 넘김", example = "10:00:00")
    val endTime: LocalTime?,
    @get:Schema(description = "초기 정류장 이름", example = "dormitory_o")
    val stopName: String,
    @get:Schema(description = "높을수록 먼저 평가하는 우선순위", example = "100")
    val priority: Int,
    @get:Schema(description = "규칙 활성 여부", example = "true")
    val enabled: Boolean,
    @get:Schema(description = "Geofence 다각형 꼭짓점")
    val polygon: List<ShuttleGeoPoint>,
)

data class ShuttleInitialStopRuleResponse(
    val seq: Int,
    val name: String,
    val periodType: String,
    val weekday: Boolean,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val stopName: String,
    val priority: Int,
    val enabled: Boolean,
    val polygon: List<ShuttleGeoPoint>,
)

data class ShuttleInitialStopRuleListResponse(
    val result: List<ShuttleInitialStopRuleResponse>,
)
