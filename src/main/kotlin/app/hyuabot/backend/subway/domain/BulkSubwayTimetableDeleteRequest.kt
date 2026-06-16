package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class BulkSubwayTimetableDeleteRequest(
    @get:Schema(description = "삭제할 시간표 SEQ 목록 (seqList 또는 stationIDs 중 하나 필수)")
    val seqList: List<Int>? = null,
    @get:Schema(description = "삭제할 역 ID 목록")
    val stationIDs: List<String>? = null,
    @get:Schema(description = "행선 필터 (up/down, stationIDs와 함께 사용)")
    val direction: String? = null,
    @get:Schema(description = "요일 필터 (weekdays/weekends, stationIDs와 함께 사용)")
    val weekday: String? = null,
)
