package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyRequest
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyResponse
import app.hyuabot.backend.timetableimport.domain.TimetableImportPreviewResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus

class TimetableImportControllerTest {
    private val subwayService = mock<SubwayTimetableImportService>()
    private val shuttleService = mock<ShuttleTimetableImportService>()
    private val subwayController = SubwayTimetableImportController(subwayService)
    private val shuttleController = ShuttleTimetableImportController(shuttleService)
    private val preview = TimetableImportPreviewResponse("id", "expires", 1, 2, 3, 4, emptyList(), emptyList(), emptyList())
    private val applied = TimetableImportApplyResponse(1, 2, 3, 4)

    @Test
    fun `subway endpoints delegate and map import errors`() {
        val request = SubwayTimetableImportRequest(emptyList(), emptyList())
        whenever(subwayService.preview(request)).thenReturn(preview)
        whenever(subwayService.apply("id")).thenReturn(applied)
        assertEquals(preview, subwayController.preview(request))
        assertEquals(applied, subwayController.apply(TimetableImportApplyRequest("id")))
        assertEquals(HttpStatus.CONFLICT, subwayController.handleImportException(TimetableImportException("PREVIEW_STALE")).statusCode)
        assertEquals(
            HttpStatus.CONFLICT,
            subwayController.handleImportException(TimetableImportException("PREVIEW_ALREADY_APPLYING")).statusCode,
        )
        assertEquals(HttpStatus.NOT_FOUND, subwayController.handleImportException(TimetableImportException("PREVIEW_NOT_FOUND")).statusCode)
    }

    @Test
    fun `shuttle endpoints delegate and map import errors`() {
        val request = ShuttleTimetableImportRequest(emptyList(), emptyList())
        whenever(shuttleService.preview(request)).thenReturn(preview)
        whenever(shuttleService.apply("id")).thenReturn(applied)
        assertEquals(preview, shuttleController.preview(request))
        assertEquals(applied, shuttleController.apply(TimetableImportApplyRequest("id")))
        assertEquals(HttpStatus.CONFLICT, shuttleController.handleImportException(TimetableImportException("PREVIEW_STALE")).statusCode)
        assertEquals(
            HttpStatus.CONFLICT,
            shuttleController.handleImportException(TimetableImportException("PREVIEW_ALREADY_APPLYING")).statusCode,
        )
        assertEquals(
            HttpStatus.NOT_FOUND,
            shuttleController.handleImportException(TimetableImportException("PREVIEW_NOT_FOUND")).statusCode,
        )
    }
}
