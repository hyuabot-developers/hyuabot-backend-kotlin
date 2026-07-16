package app.hyuabot.backend.timetableimport.domain

import app.hyuabot.backend.subway.domain.BulkSubwayTimetableCreateRequest

data class TimetableImportIssue(
    val row: Int?,
    val code: String,
    val message: String,
)

data class TimetableImportChange(
    val type: String,
    val identifier: String,
    val before: String?,
    val after: String?,
)

data class TimetableImportPreviewResponse(
    val previewID: String?,
    val expiresAt: String?,
    val createCount: Int,
    val updateCount: Int,
    val deleteCount: Int,
    val unchangedCount: Int,
    val errors: List<TimetableImportIssue>,
    val warnings: List<TimetableImportIssue>,
    val sampleChanges: List<TimetableImportChange>,
)

data class TimetableImportApplyRequest(
    val previewID: String,
)

data class TimetableImportApplyResponse(
    val createCount: Int,
    val updateCount: Int,
    val deleteCount: Int,
    val unchangedCount: Int,
)

data class SubwayTimetableImportRequest(
    val stationIDs: List<String>,
    val entries: List<BulkSubwayTimetableCreateRequest>,
)

data class ShuttleTimetableImportEntry(
    val routeName: String,
    val period: String,
    val weekday: Boolean,
    val departureTime: String,
)

data class ShuttleTimetableImportRequest(
    val routeNames: List<String>,
    val entries: List<ShuttleTimetableImportEntry>,
)

data class SubwayTimetableImportSnapshot(
    val request: SubwayTimetableImportRequest,
    val fingerprint: String,
    val result: TimetableImportApplyResponse,
)

data class ShuttleTimetableImportSnapshot(
    val request: ShuttleTimetableImportRequest,
    val fingerprint: String,
    val result: TimetableImportApplyResponse,
)
