package app.hyuabot.backend.room

import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.room.domain.RoomListResponse
import app.hyuabot.backend.room.domain.RoomRequest
import app.hyuabot.backend.room.domain.RoomResponse
import app.hyuabot.backend.room.exception.DuplicateRoomException
import app.hyuabot.backend.room.exception.RoomNotFoundException
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
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/v1/room"])
@RestController
@Tag(name = "Room", description = "교내 강의실 API")
class RoomController {
    @Autowired private lateinit var roomService: RoomService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping(path = [""])
    @Operation(summary = "교내 강의실 목록 조회", description = "교내 강의실 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "교내 강의실 목록 조회 성공",
        content = [
            Content(
                schema = Schema(implementation = RoomListResponse::class),
                examples = [
                    ExampleObject(
                        name = "강의실 목록 조회 예시",
                        value = """
                            {
                                "result": [
                                    {
                                        "seq": 1,
                                        "buildingName": "공학관",
                                        "number": "101",
                                        "name": "공학관 101호"
                                    },
                                    {
                                        "seq": 2,
                                        "buildingName": "공학관",
                                        "number": "102",
                                        "name": "공학관 102호"
                                    }
                                ]
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    fun getRoomList(
        @RequestParam(value = "building", required = false) building: String? = null,
        @RequestParam(value = "name", required = false) name: String? = null,
    ): ResponseEntity<RoomListResponse> =
        ResponseBuilder.response(
            HttpStatus.OK,
            RoomListResponse(
                result =
                    roomService
                        .getRoomList(building, name)
                        .map {
                            RoomResponse(
                                seq = it.seq ?: 0,
                                buildingName = it.buildingName,
                                number = it.number,
                                name = it.name,
                            )
                        }.sortedBy { it.seq },
            ),
        )

    @PostMapping(path = [""], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "교내 강의실 생성",
        description = "교내 강의실을 생성합니다. `building`과 `number`는 중복될 수 없습니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = RoomRequest::class),
                        examples = [
                            ExampleObject(
                                name = "강의실 생성 예시",
                                value = """
                                {
                                    "buildingName": "공학관",
                                    "number": "101",
                                    "name": "공학관 101호"
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "교내 강의실 생성 성공",
                content = [
                    Content(
                        schema = Schema(implementation = RoomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "강의실 생성 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "buildingName": "공학관",
                                        "number": "101",
                                        "name": "공학관 101호"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "교내 강의실 중복 생성 실패",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "강의실 중복 생성 예시",
                                        value = "{\"message\": \"DUPLICATE_BUILDING_NAME_AND_NUMBER\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "서버 오류 예시",
                                        value = "{\"message\": \"INTERNAL_SERVER_ERROR\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
        ],
    )
    fun createRoom(
        @RequestBody payload: RoomRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                roomService.createRoom(payload).let {
                    RoomResponse(
                        seq = it.seq ?: 0,
                        buildingName = it.buildingName,
                        number = it.number,
                        name = it.name,
                    )
                },
            )
        } catch (_: DuplicateRoomException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_BUILDING_NAME_AND_NUMBER"),
            )
        } catch (_: BuildingNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUILDING_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to create room: ${e.message}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping(path = ["/{seq}"])
    @Operation(
        summary = "교내 강의실 상세 조회",
        description = "교내 강의실의 상세 정보를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "교내 강의실의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "교내 강의실 상세 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = RoomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "강의실 상세 조회 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "buildingName": "공학관",
                                        "number": "101",
                                        "name": "공학관 101호"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "교내 강의실을 찾을 수 없음",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "강의실 찾기 실패 예시",
                                        value = "{\"message\": \"ROOM_NOT_FOUND\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "서버 오류 예시",
                                        value = "{\"message\": \"INTERNAL_SERVER_ERROR\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
        ],
    )
    fun getRoomDetail(
        @PathVariable("seq") seq: Int,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(
                HttpStatus.OK,
                roomService.getRoomByID(seq).let {
                    RoomResponse(
                        seq = it.seq ?: 0,
                        buildingName = it.buildingName,
                        number = it.number,
                        name = it.name,
                    )
                },
            )
        } catch (e: RoomNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("ROOM_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to get room detail: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping(path = ["/{seq}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "교내 강의실 수정",
        description = "교내 강의실의 정보를 수정합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "교내 강의실의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = RoomRequest::class),
                        examples = [
                            ExampleObject(
                                name = "강의실 수정 예시",
                                value = """
                                    {
                                        "buildingName": "공학관",
                                        "number": "101",
                                        "name": "공학관 101호"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "교내 강의실 수정 성공",
                content = [
                    Content(
                        schema = Schema(implementation = RoomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "강의실 수정 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "buildingName": "공학관",
                                        "number": "101",
                                        "name": "공학관 101호"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "교내 강의실을 찾을 수 없음",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "강의실 찾기 실패 예시",
                                        value = "{\"message\": \"ROOM_NOT_FOUND\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "서버 오류 예시",
                                        value = "{\"message\": \"INTERNAL_SERVER_ERROR\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
        ],
    )
    fun updateRoom(
        @PathVariable("seq") seq: Int,
        @RequestBody payload: RoomRequest,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(
                HttpStatus.OK,
                roomService.updateRoom(seq, payload).let {
                    RoomResponse(
                        seq = it.seq ?: 0,
                        buildingName = it.buildingName,
                        number = it.number,
                        name = it.name,
                    )
                },
            )
        } catch (_: RoomNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("ROOM_NOT_FOUND"),
            )
        } catch (_: BuildingNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUILDING_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to update room: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping(path = ["/{seq}"])
    @Operation(
        summary = "교내 강의실 삭제",
        description = "교내 강의실을 삭제합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "교내 강의실의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "교내 강의실 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "교내 강의실을 찾을 수 없음",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "강의실 찾기 실패 예시",
                                        value = "{\"message\": \"ROOM_NOT_FOUND\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "서버 오류 예시",
                                        value = "{\"message\": \"INTERNAL_SERVER_ERROR\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
        ],
    )
    fun deleteRoom(
        @PathVariable("seq") seq: Int,
    ): ResponseEntity<*> =
        try {
            roomService.deleteRoom(seq)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: RoomNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("ROOM_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to delete room: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
