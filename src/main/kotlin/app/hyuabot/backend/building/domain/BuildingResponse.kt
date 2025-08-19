package app.hyuabot.backend.building.domain

import io.swagger.v3.oas.annotations.media.Schema

data class BuildingResponse(
    @get:Schema(description = "건물 ID", example = "301")
    val id: String?,
    @get:Schema(description = "건물 이름", example = "제1공학관")
    val name: String,
    @get:Schema(description = "소속 캠퍼스 ID", example = "1")
    val campusID: Int,
    @get:Schema(description = "건물 위도", example = "37.123456")
    val latitude: Double,
    @get:Schema(description = "건물 경도", example = "127.123456")
    val longitude: Double,
    @get:Schema(description = "건물 설명 URL", example = "https://example.com/building/301")
    val url: String?,
)
