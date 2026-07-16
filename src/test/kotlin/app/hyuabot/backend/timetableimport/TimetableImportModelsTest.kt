package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.subway.domain.BulkSubwayTimetableCreateRequest
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportEntry
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportSnapshot
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportSnapshot
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyRequest
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyResponse
import app.hyuabot.backend.timetableimport.domain.TimetableImportChange
import app.hyuabot.backend.timetableimport.domain.TimetableImportIssue
import app.hyuabot.backend.timetableimport.domain.TimetableImportPreviewResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TimetableImportModelsTest {
    @Test
    fun `import response models expose value semantics`() {
        val issue = TimetableImportIssue(1, "CODE", "message")
        val change = TimetableImportChange("CREATE", "item", null, "after")
        val preview = TimetableImportPreviewResponse("id", "expires", 1, 2, 3, 4, listOf(issue), emptyList(), listOf(change))
        val applyRequest = TimetableImportApplyRequest("id")
        val result = TimetableImportApplyResponse(1, 2, 3, 4)

        val (row, code, message) = issue
        val (type, identifier, before, after) = change
        val (previewID, expiresAt, createCount, updateCount, deleteCount, unchangedCount, errors, warnings, changes) = preview
        assertEquals(listOf(1, "CODE", "message"), listOf(row, code, message))
        assertEquals(listOf("CREATE", "item", null, "after"), listOf(type, identifier, before, after))
        assertEquals(
            listOf("id", "expires", 1, 2, 3, 4, listOf(issue), emptyList<Any>(), listOf(change)),
            listOf(previewID, expiresAt, createCount, updateCount, deleteCount, unchangedCount, errors, warnings, changes),
        )
        assertEquals(1, issue.row)
        assertEquals("message", issue.message)
        assertEquals("CREATE", change.type)
        assertEquals("item", change.identifier)
        assertEquals(null, change.before)
        assertEquals("after", change.after)
        assertEquals("expires", preview.expiresAt)
        assertEquals("id", applyRequest.component1())
        assertEquals(listOf(1, 2, 3, 4), listOf(result.component1(), result.component2(), result.component3(), result.component4()))
        assertEquals(issue, issue.copy())
        assertEquals(change, change.copy())
        assertEquals(preview, preview.copy())
        assertEquals(applyRequest, applyRequest.copy())
        assertEquals(result, result.copy())
        exerciseValue(issue, issue.copy(row = 2, code = "OTHER", message = "other"))
        exerciseValue(change, change.copy(type = "DELETE", identifier = "other", before = "before", after = null))
        exerciseValue(
            preview,
            preview.copy(
                previewID = "other",
                expiresAt = null,
                createCount = 0,
                updateCount = 0,
                deleteCount = 0,
                unchangedCount = 0,
                errors = emptyList(),
                warnings = listOf(issue),
                sampleChanges = emptyList(),
            ),
        )
        exerciseValue(applyRequest, applyRequest.copy(previewID = "other"))
        exerciseValue(result, result.copy(createCount = 0, updateCount = 0, deleteCount = 0, unchangedCount = 0))
    }

    @Test
    fun `import request and snapshot models expose value semantics`() {
        val subwayEntry = BulkSubwayTimetableCreateRequest("station", "start", "terminal", "05:00:00", "weekdays", "up")
        val subwayRequest = SubwayTimetableImportRequest(listOf("station"), listOf(subwayEntry))
        val shuttleEntry = ShuttleTimetableImportEntry("route", "semester", true, "05:00:00")
        val shuttleRequest = ShuttleTimetableImportRequest(listOf("route"), listOf(shuttleEntry))
        val result = TimetableImportApplyResponse(1, 0, 0, 0)
        val subwaySnapshot = SubwayTimetableImportSnapshot(subwayRequest, "hash", result)
        val shuttleSnapshot = ShuttleTimetableImportSnapshot(shuttleRequest, "hash", result)

        val (stationIDs, subwayEntries) = subwayRequest
        val (route, period, weekday, time) = shuttleEntry
        val (routeNames, shuttleEntries) = shuttleRequest
        val (savedSubwayRequest, subwayHash, subwayResult) = subwaySnapshot
        val (savedShuttleRequest, shuttleHash, shuttleResult) = shuttleSnapshot
        assertEquals(listOf("station"), stationIDs)
        assertEquals(listOf(subwayEntry), subwayEntries)
        assertEquals(listOf("route", "semester", true, "05:00:00"), listOf(route, period, weekday, time))
        assertEquals(listOf("route"), routeNames)
        assertEquals(listOf(shuttleEntry), shuttleEntries)
        assertEquals(listOf(subwayRequest, "hash", result), listOf(savedSubwayRequest, subwayHash, subwayResult))
        assertEquals(listOf(shuttleRequest, "hash", result), listOf(savedShuttleRequest, shuttleHash, shuttleResult))
        assertEquals(subwayRequest, subwayRequest.copy())
        assertEquals(shuttleEntry, shuttleEntry.copy())
        assertEquals(shuttleRequest, shuttleRequest.copy())
        assertEquals(subwaySnapshot, subwaySnapshot.copy())
        assertEquals(shuttleSnapshot, shuttleSnapshot.copy())
        exerciseValue(subwayRequest, subwayRequest.copy(stationIDs = emptyList(), entries = emptyList()))
        exerciseValue(
            shuttleEntry,
            shuttleEntry.copy(routeName = "other", period = "vacation", weekday = false, departureTime = "06:00:00"),
        )
        exerciseValue(shuttleRequest, shuttleRequest.copy(routeNames = emptyList(), entries = emptyList()))
        exerciseValue(subwaySnapshot, subwaySnapshot.copy(request = subwayRequest, fingerprint = "other", result = result))
        exerciseValue(shuttleSnapshot, shuttleSnapshot.copy(request = shuttleRequest, fingerprint = "other", result = result))
    }

    private fun exerciseValue(
        value: Any,
        different: Any,
    ) {
        assertEquals(value, value)
        assertNotEquals(value, different)
        value.hashCode()
        value.toString()
    }
}
