package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.shuttle.domain.CreateShuttleStopRequest
import app.hyuabot.backend.shuttle.domain.ShuttleStopListResponse
import app.hyuabot.backend.shuttle.domain.ShuttleStopResponse
import app.hyuabot.backend.shuttle.domain.UpdateShuttleStopRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleStopException
import app.hyuabot.backend.shuttle.exception.ShuttleStopNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleStopService
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/shuttle/stop")
@RestController
@Tag(name = "Shuttle", description = "셔틀버스 관련 API")
class ShuttleStopController {
    @Autowired private lateinit var shuttleStopService: ShuttleStopService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "셔틀버스 정류장 조회", description = "셔틀버스 정류장 목록을 조회합니다.")
    fun getShuttleStops(): ShuttleStopListResponse =
        ShuttleStopListResponse(
            result =
                shuttleStopService.getAllStops().map {
                    ShuttleStopResponse(
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                    )
                },
        )

    @PostMapping("")
    @Operation(
        summary = "셔틀버스 정류장 생성",
        description = "새로운 셔틀버스 정류장을 생성합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 정류장 생성 요청",
                content = [Content(schema = Schema(implementation = CreateShuttleStopRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 정류장 생성 성공",
                content = [Content(schema = Schema(implementation = ShuttleStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "셔틀버스 정류장 중복",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 셔틀버스 정류장 ID",
                                value = """{"message": "DUPLICATE_SHUTTLE_STOP"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createShuttleStop(
        @RequestBody payload: CreateShuttleStopRequest,
    ): ResponseEntity<*> {
        try {
            val createdStop = shuttleStopService.createStop(payload)
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                ShuttleStopResponse(
                    name = createdStop.name,
                    latitude = createdStop.latitude,
                    longitude = createdStop.longitude,
                ),
            )
        } catch (_: DuplicateShuttleStopException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SHUTTLE_STOP"),
            )
        } catch (e: Exception) {
            logger.error("Error creating shuttle stop: ${payload.name}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{name}")
    @Operation(
        summary = "셔틀버스 정류장 조회",
        description = "특정 셔틀버스 정류장을 조회합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "셔틀버스 정류장 ID",
                required = true,
                schema = Schema(type = "string"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 정류장 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 정류장 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 정류장 없음",
                                value = """{"message": "SHUTTLE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getShuttleStopByName(
        @PathVariable name: String,
    ): ResponseEntity<*> =
        try {
            val stop = shuttleStopService.getStopByName(name)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleStopResponse(
                    name = stop.name,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                ),
            )
        } catch (_: ShuttleStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_STOP_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving shuttle stop: $name", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{name}")
    @Operation(
        summary = "셔틀버스 정류장 수정",
        description = "특정 셔틀버스 정류장을 수정합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "셔틀버스 정류장 ID",
                required = true,
                schema = Schema(type = "string"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 정류장 수정 요청",
                content = [Content(schema = Schema(implementation = UpdateShuttleStopRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 정류장 수정 성공",
                content = [Content(schema = Schema(implementation = ShuttleStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 정류장 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 정류장 없음",
                                value = """{"message": "SHUTTLE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateShuttleStop(
        @PathVariable name: String,
        @RequestBody payload: UpdateShuttleStopRequest,
    ): ResponseEntity<*> =
        try {
            val updatedStop = shuttleStopService.updateStop(name, payload)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleStopResponse(
                    name = updatedStop.name,
                    latitude = updatedStop.latitude,
                    longitude = updatedStop.longitude,
                ),
            )
        } catch (_: ShuttleStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_STOP_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error updating shuttle stop: $name", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{name}")
    @Operation(
        summary = "셔틀버스 정류장 삭제",
        description = "특정 셔틀버스 정류장을 삭제합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "셔틀버스 정류장 ID",
                required = true,
                schema = Schema(type = "string"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "셔틀버스 정류장 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 정류장 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 정류장 없음",
                                value = """{"message": "SHUTTLE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteShuttleStopByName(
        @PathVariable name: String,
    ): ResponseEntity<*> =
        try {
            shuttleStopService.deleteStopByName(name)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: ShuttleStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_STOP_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting shuttle stop: $name", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
