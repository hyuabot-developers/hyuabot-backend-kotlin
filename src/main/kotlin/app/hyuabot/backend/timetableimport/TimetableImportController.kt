package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyRequest
import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/subway/timetable/import")
class SubwayTimetableImportController(
    private val service: SubwayTimetableImportService,
) {
    @PostMapping("/preview")
    fun preview(
        @RequestBody request: SubwayTimetableImportRequest,
    ) = service.preview(request)

    @PostMapping("/apply")
    fun apply(
        @RequestBody request: TimetableImportApplyRequest,
    ) = service.apply(request.previewID)

    @ExceptionHandler(TimetableImportException::class)
    fun handleImportException(exception: TimetableImportException): ResponseEntity<ResponseBuilder.Message> =
        ResponseBuilder.response(
            if (exception.code == "PREVIEW_STALE" || exception.code == "PREVIEW_ALREADY_APPLYING") {
                HttpStatus.CONFLICT
            } else {
                HttpStatus.NOT_FOUND
            },
            exception.code,
        )
}

@RestController
@RequestMapping("/api/v1/shuttle/timetable/import")
class ShuttleTimetableImportController(
    private val service: ShuttleTimetableImportService,
) {
    @PostMapping("/preview")
    fun preview(
        @RequestBody request: ShuttleTimetableImportRequest,
    ) = service.preview(request)

    @PostMapping("/apply")
    fun apply(
        @RequestBody request: TimetableImportApplyRequest,
    ) = service.apply(request.previewID)

    @ExceptionHandler(TimetableImportException::class)
    fun handleImportException(exception: TimetableImportException): ResponseEntity<ResponseBuilder.Message> =
        ResponseBuilder.response(
            if (exception.code == "PREVIEW_STALE" || exception.code == "PREVIEW_ALREADY_APPLYING") {
                HttpStatus.CONFLICT
            } else {
                HttpStatus.NOT_FOUND
            },
            exception.code,
        )
}
