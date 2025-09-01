package app.hyuabot.backend.calendar.controller

import app.hyuabot.backend.calendar.CalendarService
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/calendar/version")
@RestController
@Tag(name = "Calendar", description = "학사일정 관련 API")
class CalendarVersionController {
    @Autowired private lateinit var service: CalendarService

    @GetMapping("")
    @Operation(
        summary = "학사일정 버전 조회",
        description = "학사일정의 버전을 조회합니다. 클라이언트는 이 버전을 통해 로컬에 저장된 데이터가 최신인지 확인할 수 있습니다.",
    )
    fun getCalendarVersion(): ResponseEntity<ResponseBuilder.Message> {
        service.getCalendarVersion().let {
            return ResponseBuilder.response(
                status = HttpStatus.OK,
                body = ResponseBuilder.Message(it.name),
            )
        }
    }
}
