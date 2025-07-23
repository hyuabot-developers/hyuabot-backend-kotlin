package app.hyuabot.backend.readingRoom

import app.hyuabot.backend.readingRoom.domain.CreateReadingRoomRequest
import app.hyuabot.backend.readingRoom.domain.ReadingRoomListResponse
import app.hyuabot.backend.readingRoom.domain.ReadingRoomResponse
import app.hyuabot.backend.readingRoom.domain.UpdateReadingRoomRequest
import app.hyuabot.backend.readingRoom.exception.DuplicateReadingRoomException
import app.hyuabot.backend.readingRoom.exception.ReadingRoomNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
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

@RequestMapping(path = ["/api/v1/reading-room"])
@RestController
@Tag(name = "Reading Room", description = "도서관 열람실 관련 API")
class ReadingRoomController {
    @Autowired private lateinit var readingRoomService: ReadingRoomService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "열람실 목록 조회", description = "모든 열람실의 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "열람실 목록 조회 성공",
        content = [
            Content(
                schema = Schema(implementation = ReadingRoomListResponse::class),
                examples = [
                    ExampleObject(
                        name = "열람실 목록 조회 성공 예시",
                        value = """
                            {
                                "result": [
                                    {
                                        "seq": 61,
                                        "name": "제1열람실 (2F)",
                                        "campusID": 2,
                                        "isActive": true,
                                        "isReservable": true,
                                        "total": 320,
                                        "active": 320,
                                        "occupied": 18,
                                        "available": 302,
                                        "updatedAt": "2025-07-22 16:06:03"
                                    },
                                    {
                                        "seq": 63,
                                        "name": "제2열람실 (4F)",
                                        "campusID": 2,
                                        "isActive": true,
                                        "isReservable": true,
                                        "total": 216,
                                        "active": 216,
                                        "occupied": 35,
                                        "available": 181,
                                        "updatedAt": "2025-07-22 16:06:03"
                                    },
                                    {
                                        "seq": 131,
                                        "name": "집중열람실 (4F)",
                                        "campusID": 2,
                                        "isActive": true,
                                        "isReservable": true,
                                        "total": 12,
                                        "active": 12,
                                        "occupied": 0,
                                        "available": 12,
                                        "updatedAt": "2025-07-22 16:06:03"
                                    },
                                    {
                                        "seq": 132,
                                        "name": "노상일 HOLMZ (4F)",
                                        "campusID": 2,
                                        "isActive": true,
                                        "isReservable": true,
                                        "total": 82,
                                        "active": 82,
                                        "occupied": 14,
                                        "available": 68,
                                        "updatedAt": "2025-07-22 16:06:03"
                                    }
                                ]
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    fun getReadingRoomList(
        @RequestParam(name = "name", required = false) name: String?,
    ): ReadingRoomListResponse =
        ReadingRoomListResponse(
            result =
                readingRoomService
                    .getReadingRoomList(name)
                    .map {
                        ReadingRoomResponse(
                            seq = it.id,
                            name = it.name,
                            campusID = it.campusID,
                            isActive = it.isActive,
                            isReservable = it.isReservable,
                            total = it.total,
                            active = it.active,
                            occupied = it.occupied,
                            available = it.available,
                            updatedAt =
                                LocalDateTimeBuilder.convertLocalDateTimeToString(
                                    LocalDateTimeBuilder.convertAsServiceTimezone(it.updatedAt),
                                ),
                        )
                    },
        )

    @PostMapping("", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "열람실 생성",
        description = "새로운 열람실을 생성합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CreateReadingRoomRequest::class),
                        examples = [
                            ExampleObject(
                                name = "열람실 생성 예시",
                                value = """
                                {
                                    "id": 100,
                                    "name": "새로운 열람실",
                                    "campusID": 1,
                                    "total": 50
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
                description = "열람실 생성 성공",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ReadingRoomResponse::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 등록 성공 예시",
                                        value = """
                                        {
                                            "seq": 100,
                                            "name": "새로운 열람실",
                                            "campusID": 1,
                                            "isActive": true,
                                            "isReservable": true,
                                            "total": 50,
                                            "active": 50,
                                            "occupied": 0,
                                            "available": 50,
                                            "updatedAt": "2025-07-22 16:06:03"
                                        }
                                        """,
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "열람실 ID 중복 오류",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 이름 중복 예시",
                                        value = "{\"message\": \"DUPLICATE_READING_ROOM_ID\"}",
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
    fun createReadingRoom(
        @RequestBody payload: CreateReadingRoomRequest,
    ): ResponseEntity<*> {
        try {
            return readingRoomService
                .createReadingRoom(payload)
                .let {
                    ResponseBuilder.response(
                        HttpStatus.CREATED,
                        ReadingRoomResponse(
                            seq = it.id,
                            name = it.name,
                            campusID = it.campusID,
                            isActive = it.isActive,
                            isReservable = it.isReservable,
                            total = it.total,
                            active = it.active,
                            occupied = it.occupied,
                            available = it.available,
                            updatedAt =
                                LocalDateTimeBuilder.convertLocalDateTimeToString(
                                    LocalDateTimeBuilder.convertAsServiceTimezone(it.updatedAt),
                                ),
                        ),
                    )
                }
        } catch (_: DuplicateReadingRoomException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                "DUPLICATE_READING_ROOM_ID",
            )
        } catch (e: Exception) {
            logger.error("Error creating reading room: ${e.message}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }
    }

    @GetMapping("/{seq}")
    @Operation(
        summary = "열람실 상세 조회",
        description = "열람실의 상세 정보를 조회합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "열람실 상세 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = ReadingRoomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "열람실 상세 조회 성공 예시",
                                value = """
                                    {
                                        "seq": 61,
                                        "name": "제1열람실 (2F)",
                                        "campusID": 2,
                                        "isActive": true,
                                        "isReservable": true,
                                        "total": 320,
                                        "active": 320,
                                        "occupied": 18,
                                        "available": 302,
                                        "updatedAt": "2025-07-22 16:06:03"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "열람실을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "열람실을 찾을 수 없음 예시",
                                value = "{\"message\": \"READING_ROOM_NOT_FOUND\"}",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getReadingRoomDetail(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            readingRoomService.getReadingRoomById(seq).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    ReadingRoomResponse(
                        seq = it.id,
                        name = it.name,
                        campusID = it.campusID,
                        isActive = it.isActive,
                        isReservable = it.isReservable,
                        total = it.total,
                        active = it.active,
                        occupied = it.occupied,
                        available = it.available,
                        updatedAt =
                            LocalDateTimeBuilder.convertLocalDateTimeToString(
                                LocalDateTimeBuilder.convertAsServiceTimezone(it.updatedAt),
                            ),
                    ),
                )
            }
        } catch (_: ReadingRoomNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                "READING_ROOM_NOT_FOUND",
            )
        } catch (e: Exception) {
            logger.error("Error fetching reading room detail: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }

    @PutMapping("/{seq}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "열람실 정보 수정",
        description = "열람실의 정보를 수정합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = UpdateReadingRoomRequest::class),
                        examples = [
                            ExampleObject(
                                name = "열람실 정보 수정 예시",
                                value = """
                                {
                                    "campusID": 1,
                                    "name": "수정된 열람실",
                                    "total": 60,
                                    "active": 60,
                                    "isActive": true,
                                    "isReservable": true
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
                description = "열람실 정보 수정 성공",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ReadingRoomResponse::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "열람실 정보 수정 성공 예시",
                                        value = """
                                        {
                                            "seq": 61,
                                            "name": "수정된 열람실",
                                            "campusID": 1,
                                            "isActive": true,
                                            "isReservable": true,
                                            "total": 60,
                                            "active": 60,
                                            "occupied": 0,
                                            "available": 60,
                                            "updatedAt": "2025-07-22 16:06:03"
                                        }
                                        """,
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "열람실을 찾을 수 없음",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "열람실을 찾을 수 없음 예시",
                                        value = "{\"message\": \"READING_ROOM_NOT_FOUND\"}",
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
    fun updateReadingRoom(
        @PathVariable seq: Int,
        @RequestBody payload: UpdateReadingRoomRequest,
    ): ResponseEntity<*> =
        try {
            readingRoomService.updateReadingRoom(seq, payload).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    ReadingRoomResponse(
                        seq = it.id,
                        name = it.name,
                        campusID = it.campusID,
                        isActive = it.isActive,
                        isReservable = it.isReservable,
                        total = it.total,
                        active = it.active,
                        occupied = it.occupied,
                        available = it.available,
                        updatedAt =
                            LocalDateTimeBuilder.convertLocalDateTimeToString(
                                LocalDateTimeBuilder.convertAsServiceTimezone(it.updatedAt),
                            ),
                    ),
                )
            }
        } catch (_: ReadingRoomNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                "READING_ROOM_NOT_FOUND",
            )
        } catch (e: Exception) {
            logger.error("Error updating reading room: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "열람실 삭제",
        description = "열람실을 삭제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "열람실 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "열람실을 찾을 수 없음",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "열람실을 찾을 수 없음 예시",
                                        value = "{\"message\": \"READING_ROOM_NOT_FOUND\"}",
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
    fun deleteReadingRoom(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            readingRoomService.deleteReadingRoomById(seq)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: ReadingRoomNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                "READING_ROOM_NOT_FOUND",
            )
        } catch (e: Exception) {
            logger.error("Error deleting reading room: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }
}
