package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusStopListResponse
import app.hyuabot.backend.bus.domain.BusStopResponse
import app.hyuabot.backend.bus.domain.CreateBusStopRequest
import app.hyuabot.backend.bus.domain.UpdateBusStopRequest
import app.hyuabot.backend.bus.exception.BusStopNotFoundException
import app.hyuabot.backend.bus.exception.DuplicateBusStopException
import app.hyuabot.backend.bus.service.BusStopService
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

@RequestMapping("/api/v1/bus/stop")
@RestController
@Tag(name = "Bus", description = "노선 버스 관련 API")
class BusStopController {
    @Autowired private lateinit var service: BusStopService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "버스 정류장 목록 조회", description = "등록된 모든 버스 정류장 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "버스 정류장 목록 조회 성공",
        content = [Content(schema = Schema(implementation = BusStopListResponse::class))],
    )
    fun getAllBusStops(): BusStopListResponse =
        BusStopListResponse(
            service.getBusStopList().map {
                BusStopResponse(
                    id = it.id,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    districtCode = it.districtCode,
                    regionName = it.regionName,
                    mobileNumber = it.mobileNumber,
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "버스 정류장 등록", description = "새로운 버스 정류장을 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "버스 정류장 등록 성공",
                content = [Content(schema = Schema(implementation = BusStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "버스 정류장 중복",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "DUPLICATE_BUS_STOP"}""")],
                    ),
                ],
            ),
        ],
    )
    fun createBusStop(
        @RequestBody payload: CreateBusStopRequest,
    ): ResponseEntity<*> =
        try {
            service.createBusStop(payload).let {
                ResponseBuilder.response(
                    HttpStatus.CREATED,
                    BusStopResponse(
                        id = it.id,
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        districtCode = it.districtCode,
                        regionName = it.regionName,
                        mobileNumber = it.mobileNumber,
                    ),
                )
            }
        } catch (_: DuplicateBusStopException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, ResponseBuilder.Message("DUPLICATE_BUS_STOP"))
        } catch (e: Exception) {
            logger.error("Error creating bus stop", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }

    @GetMapping("/{id}")
    @Operation(summary = "버스 정류장 조회", description = "ID로 버스 정류장을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 정류장 조회 성공",
                content = [Content(schema = Schema(implementation = BusStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "버스 정류장 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "BUS_STOP_NOT_FOUND"}""")],
                    ),
                ],
            ),
        ],
    )
    fun getBusStopById(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        try {
            service.getBusStopById(id).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    BusStopResponse(
                        id = it.id,
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        districtCode = it.districtCode,
                        regionName = it.regionName,
                        mobileNumber = it.mobileNumber,
                    ),
                )
            }
        } catch (_: BusStopNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("BUS_STOP_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error retrieving bus stop with id $id", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }

    @PutMapping("/{id}")
    @Operation(summary = "버스 정류장 수정", description = "ID로 버스 정류장을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 정류장 수정 성공",
                content = [Content(schema = Schema(implementation = BusStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "버스 정류장 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "BUS_STOP_NOT_FOUND"}""")],
                    ),
                ],
            ),
        ],
    )
    fun updateBusStop(
        @PathVariable id: Int,
        @RequestBody payload: UpdateBusStopRequest,
    ): ResponseEntity<*> =
        try {
            service.updateBusStop(id, payload).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    BusStopResponse(
                        id = it.id,
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        districtCode = it.districtCode,
                        regionName = it.regionName,
                        mobileNumber = it.mobileNumber,
                    ),
                )
            }
        } catch (_: BusStopNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("BUS_STOP_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error updating bus stop with id $id", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }

    @DeleteMapping("/{id}")
    @Operation(summary = "버스 정류장 삭제", description = "ID로 버스 정류장을 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 정류장 삭제 성공",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "버스 정류장 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "BUS_STOP_NOT_FOUND"}""")],
                    ),
                ],
            ),
        ],
    )
    fun deleteBusStop(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteBusStopById(id)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, ResponseBuilder.Message("BUS_STOP_DELETED"))
        } catch (_: BusStopNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("BUS_STOP_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error deleting bus stop with id $id", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }
}
