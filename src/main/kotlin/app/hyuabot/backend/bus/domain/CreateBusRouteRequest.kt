package app.hyuabot.backend.bus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateBusRouteRequest(
    @get:Schema(description = "버스 노선 ID", example = "100100001")
    val id: Int,
    @get:Schema(description = "버스 노선 이름", example = "100")
    val name: String,
    @get:Schema(description = "버스 노선 유형", example = "10")
    val typeCode: String,
    @get:Schema(description = "버스 노선 유형 이름", example = "공항버스")
    val typeName: String,
    @get:Schema(description = "버스 노선 시점 정류장 ID", example = "10010000101")
    val startStopID: Int,
    @get:Schema(description = "버스 노선 종점 정류장 ID", example = "10010000120")
    val endStopID: Int,
    @get:Schema(description = "버스 상행 첫차 시간", example = "05:30:00")
    val upFirstTime: String,
    @get:Schema(description = "버스 상행 막차 시간", example = "23:30:00")
    val upLastTime: String,
    @get:Schema(description = "버스 하행 첫차 시간", example = "07:00:00")
    val downFirstTime: String,
    @get:Schema(description = "버스 하행 막차 시간", example = "00:00:00")
    val downLastTime: String,
    @get:Schema(description = "관리 지역 코드", example = "11")
    val districtCode: Int,
    @get:Schema(description = "운수사 ID", example = "100000001")
    val companyID: Int,
    @get:Schema(description = "운수사 이름", example = "경원여객")
    val companyName: String,
    @get:Schema(description = "운수사 전화번호", example = "031-123-4567")
    val companyPhone: String,
)
