package app.hyuabot.backend.campus

import app.hyuabot.backend.campus.domain.CampusListResponse
import app.hyuabot.backend.campus.domain.CampusResponse
import app.hyuabot.backend.campus.domain.CreateCampusRequest
import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.campus.exception.DuplicateCampusException
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
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/v1/campus"])
@RestController
@Tag(name = "Campus", description = "캠퍼스 관련 API")
class CampusController {
    @Autowired lateinit var campusService: CampusService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "캠퍼스 목록 조회", description = "등록된 캠퍼스 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "캠퍼스 목록 조회 성공",
        content =
            [
                Content(
                    schema = Schema(implementation = CampusListResponse::class),
                    examples =
                        arrayOf(
                            ExampleObject(
                                name = "캠퍼스 목록 조회 성공 예시",
                                value = """
                                    {
                                        "result": [
                                            {
                                                "seq": 1,
                                                "name": "서울"
                                            },
                                            {
                                                "seq": 2,
                                                "name": "ERICA"
                                            }
                                        ]
                                    }
                                """,
                            ),
                        ),
                ),
            ],
    )
    fun getCampusList(): ResponseEntity<CampusListResponse> =
        campusService.getCampusList().let { campusList ->
            ResponseBuilder.response(
                HttpStatus.OK,
                CampusListResponse(
                    result = campusList.map { CampusResponse(it.id!!, it.name) },
                ),
            )
        }

    @PostMapping("", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "캠퍼스 등록",
        description = "새로운 캠퍼스를 등록합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CreateCampusRequest::class),
                        examples = [
                            ExampleObject(
                                name = "테스트 캠퍼스 등록 예시",
                                value = """
                                {
                                    "name": "테스트 캠퍼스"
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
                description = "캠퍼스 등록 성공",
                content =
                    [
                        Content(
                            schema = Schema(implementation = CampusResponse::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 등록 성공 예시",
                                        value = """
                                        {
                                            "seq": 3,
                                            "name": "신규 캠퍼스"
                                        }
                                        """,
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "캠퍼스 이름 중복",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 이름 중복 예시",
                                        value = "{\"message\": \"DUPLICATE_CAMPUS_NAME\"}",
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
    fun createCampus(
        @RequestBody payload: CreateCampusRequest,
    ): ResponseEntity<*> =
        try {
            campusService.createCampus(payload).let { campus ->
                ResponseBuilder.response(
                    HttpStatus.CREATED,
                    CampusResponse(campus.id!!, campus.name),
                )
            }
        } catch (_: DuplicateCampusException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                "DUPLICATE_CAMPUS_NAME",
            )
        } catch (_: Exception) {
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }

    @GetMapping("/{seq}")
    @Operation(
        summary = "캠퍼스 상세 조회",
        description = "특정 캠퍼스의 상세 정보를 조회합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "캠퍼스 상세 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = CampusResponse::class),
                        examples = [
                            ExampleObject(
                                name = "캠퍼스 상세 조회 성공 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "name": "서울"
                                    }
                                    """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "캠퍼스가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "캠퍼스 존재하지 않음 예시",
                                value = "{\"message\": \"CAMPUS_NOT_FOUND\"}",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getCampusDetail(
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            return campusService.getCampusById(seq).let { campus ->
                ResponseBuilder.response(
                    HttpStatus.OK,
                    CampusResponse(campus.id!!, campus.name),
                )
            }
        } catch (_: CampusNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                "CAMPUS_NOT_FOUND",
            )
        } catch (e: Exception) {
            logger.error("캠퍼스 상세 조회 실패: ${e.message}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }
    }

    @PutMapping("/{seq}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "캠퍼스 수정",
        description = "등록된 캠퍼스의 이름을 수정합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CreateCampusRequest::class),
                        examples = [
                            ExampleObject(
                                name = "테스트 캠퍼스 등록 예시",
                                value = """
                                {
                                    "name": "테스트 캠퍼스"
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
                description = "캠퍼스 수정 성공",
                content =
                    [
                        Content(
                            schema = Schema(implementation = CampusResponse::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 수정 성공 예시",
                                        value = """
                                        {
                                            "seq": 3,
                                            "name": "신규 캠퍼스"
                                        }
                                        """,
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "캠퍼스가 존재하지 않음",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 존재하지 않음 예시",
                                        value = "{\"message\": \"CAMPUS_NOT_FOUND\"}",
                                    ),
                                ),
                        ),
                    ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "캠퍼스 이름 중복",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 이름 중복 예시",
                                        value = "{\"message\": \"DUPLICATE_CAMPUS_NAME\"}",
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
    fun updateCampus(
        @PathVariable seq: Int,
        @RequestBody payload: CreateCampusRequest,
    ): ResponseEntity<*> =
        try {
            campusService.updateCampus(seq, payload).let { campus ->
                ResponseBuilder.response(
                    HttpStatus.OK,
                    CampusResponse(campus.id!!, campus.name),
                )
            }
        } catch (_: DuplicateCampusException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                "DUPLICATE_CAMPUS_NAME",
            )
        } catch (_: CampusNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                "CAMPUS_NOT_FOUND",
            )
        } catch (e: Exception) {
            logger.error("캠퍼스 수정 실패: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "캠퍼스 삭제",
        description = "등록된 캠퍼스를 삭제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "캠퍼스 수정 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "캠퍼스가 존재하지 않음",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "캠퍼스 존재하지 않음 예시",
                                        value = "{\"message\": \"CAMPUS_NOT_FOUND\"}",
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
    fun deleteCampus(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            campusService.deleteCampusById(seq)
            ResponseEntity.noContent().build<Any>()
        } catch (_: CampusNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                "CAMPUS_NOT_FOUND",
            )
        } catch (e: Exception) {
            logger.error("캠퍼스 삭제 실패: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
            )
        }
}
