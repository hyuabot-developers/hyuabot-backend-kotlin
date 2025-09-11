package app.hyuabot.backend.bus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateBusStopRequest(
    @get:Schema(description = "버스 정류장 ID", example = "123456")
    val id: Int,
    @get:Schema(description = "버스 정류장 이름", example = "강남역")
    val name: String,
    @get:Schema(description = "정류장 지역 코드", example = "11")
    val districtCode: Int,
    @get:Schema(description = "정류장 지역 이름", example = "서울")
    val regionName: String,
    @get:Schema(description = "정류장 검색 ID", example = "12345")
    val mobileNumber: String,
    @get:Schema(description = "정류장 경도", example = "127.02758")
    val longitude: Double,
    @get:Schema(description = "정류장 위도", example = "37.498095")
    val latitude: Double,
)
