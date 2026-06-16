package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.subway.domain.BulkSubwayTimetableCreateRequest
import app.hyuabot.backend.subway.domain.BulkSubwayTimetableDeleteRequest
import app.hyuabot.backend.subway.domain.SubwayTimetableListResponse
import app.hyuabot.backend.subway.domain.SubwayTimetableResponse
import app.hyuabot.backend.subway.exception.SubwayStartStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTerminalStationNotFoundException
import app.hyuabot.backend.subway.service.SubwayService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/subway/timetable")
@RestController
@Tag(name = "Subway", description = "지하철 관련 API")
class SubwayTimetableController {
    @Autowired private lateinit var service: SubwayService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "지하철 시간표 정보 조회", description = "지하철 시간표 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "지하철 시간표 정보 조회 성공",
        content = [Content(schema = Schema(implementation = SubwayTimetableListResponse::class))],
    )
    fun getSubwayTimetable(): SubwayTimetableListResponse =
        SubwayTimetableListResponse(
            service.getAllTimetables().map {
                SubwayTimetableResponse(
                    seq = it.seq!!,
                    stationID = it.stationID,
                    startStationID = it.startStationID,
                    terminalStationID = it.terminalStationID,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(it.departureTime),
                    weekday = it.weekday,
                    direction = it.heading,
                )
            },
        )

    @DeleteMapping("")
    @Operation(summary = "지하철 시간표 일괄 삭제", description = "SEQ 목록 또는 역 ID 목록+조건으로 시간표를 일괄 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "지하철 시간표 일괄 삭제 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun deleteBulkSubwayTimetable(
        @RequestBody payload: BulkSubwayTimetableDeleteRequest,
    ): ResponseEntity<*> =
        try {
            service.deleteTimetablesBulk(payload)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: IllegalArgumentException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("INVALID_REQUEST"))
        } catch (e: Exception) {
            logger.error("Error bulk deleting subway timetables", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }

    @PostMapping("/bulk")
    @Operation(summary = "지하철 시간표 일괄 생성", description = "여러 역의 시간표를 한꺼번에 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "지하철 시간표 일괄 생성 성공",
                content = [Content(schema = Schema(implementation = SubwayTimetableListResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "역을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun createBulkSubwayTimetable(
        @RequestBody payloads: List<BulkSubwayTimetableCreateRequest>,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(
                HttpStatus.CREATED,
                SubwayTimetableListResponse(
                    service.createTimetablesBulk(payloads).map {
                        SubwayTimetableResponse(
                            seq = it.seq!!,
                            stationID = it.stationID,
                            startStationID = it.startStationID,
                            terminalStationID = it.terminalStationID,
                            departureTime = LocalDateTimeBuilder.convertLocalTimeToString(it.departureTime),
                            weekday = it.weekday,
                            direction = it.heading,
                        )
                    },
                ),
            )
        } catch (_: LocalTimeNotValidException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("INVALID_TIME_FORMAT"))
        } catch (_: SubwayStationNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"))
        } catch (_: SubwayStartStationNotFoundException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("SUBWAY_START_STATION_NOT_FOUND"))
        } catch (_: SubwayTerminalStationNotFoundException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("SUBWAY_TERMINAL_STATION_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error bulk creating subway timetables", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }
}
