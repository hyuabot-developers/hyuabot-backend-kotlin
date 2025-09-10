package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.subway.domain.CreateSubwayRouteRequest
import app.hyuabot.backend.subway.domain.SubwayRouteListResponse
import app.hyuabot.backend.subway.domain.SubwayRouteResponse
import app.hyuabot.backend.subway.domain.UpdateSubwayRouteRequest
import app.hyuabot.backend.subway.exception.DuplicateSubwayRouteException
import app.hyuabot.backend.subway.exception.SubwayRouteNotFoundException
import app.hyuabot.backend.subway.service.SubwayService
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
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

@RequestMapping("/api/v1/subway/route")
@RestController
@Tag(name = "Subway", description = "지하철 관련 API")
class SubwayRouteController {
    @Autowired private lateinit var service: SubwayService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "지하철 노선 정보 조회", description = "지하철 노선 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "지하철 노선 정보 조회 성공",
        content = [Content(schema = Schema(implementation = SubwayRouteListResponse::class))],
    )
    fun getSubwayRoute(): SubwayRouteListResponse =
        SubwayRouteListResponse(
            service.getSubwayRoutes().map {
                SubwayRouteResponse(
                    id = it.id,
                    name = it.name,
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "지하철 노선 생성", description = "새로운 지하철 노선을 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "지하철 노선 생성 성공",
                content = [Content(schema = Schema(implementation = SubwayRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 지하철 노선 이름",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = ResponseBuilder.Message::class,
                            ),
                        examples = [
                            ExampleObject(
                                name = "중복된 지하철 노선 이름",
                                value = """{"message":"DUPLICATE_SUBWAY_ROUTE"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createSubwayRoute(
        @RequestBody payload: CreateSubwayRouteRequest,
    ): ResponseEntity<*> {
        try {
            service.createSubwayRoute(payload).let {
                return ResponseBuilder.response(
                    HttpStatus.CREATED,
                    SubwayRouteResponse(
                        id = it.id,
                        name = it.name,
                    ),
                )
            }
        } catch (_: DuplicateSubwayRouteException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SUBWAY_ROUTE"),
            )
        } catch (e: Exception) {
            logger.error("Error creating subway route", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "지하철 노선 ID로 조회", description = "지하철 노선 ID로 지하철 노선을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지하철 노선 ID로 조회 성공",
                content = [Content(schema = Schema(implementation = SubwayRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 노선 없음",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = ResponseBuilder.Message::class,
                            ),
                        examples = [
                            ExampleObject(
                                name = "지하철 노선 없음",
                                value = """{"message":"SUBWAY_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getSubwayRouteById(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        try {
            service.getSubwayRouteById(id).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    SubwayRouteResponse(
                        id = it.id,
                        name = it.name,
                    ),
                )
            }
        } catch (_: SubwayRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error fetching subway route by ID", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{id}")
    @Operation(summary = "지하철 노선 수정", description = "지하철 노선 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지하철 노선 수정 성공",
                content = [Content(schema = Schema(implementation = SubwayRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 노선 없음",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = ResponseBuilder.Message::class,
                            ),
                        examples = [
                            ExampleObject(
                                name = "지하철 노선 없음",
                                value = """{"message":"SUBWAY_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateSubwayRoute(
        @PathVariable id: Int,
        @RequestBody payload: UpdateSubwayRouteRequest,
    ): ResponseEntity<*> {
        try {
            service.updateSubwayRoute(id, payload).let {
                return ResponseBuilder.response(
                    HttpStatus.OK,
                    SubwayRouteResponse(
                        id = it.id,
                        name = it.name,
                    ),
                )
            }
        } catch (_: SubwayRouteNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error updating subway route", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "지하철 노선 삭제", description = "지하철 노선을 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "지하철 노선 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 노선 없음",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = ResponseBuilder.Message::class,
                            ),
                        examples = [
                            ExampleObject(
                                name = "지하철 노선 없음",
                                value = """{"message":"SUBWAY_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteSubwayRoute(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        try {
            service.deleteSubwayRoute(id)
            return ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: SubwayRouteNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting subway route", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }
}
