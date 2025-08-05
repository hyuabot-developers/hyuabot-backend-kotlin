package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.cafeteria.domain.CafeteriaListResponse
import app.hyuabot.backend.cafeteria.domain.CafeteriaResponse
import app.hyuabot.backend.cafeteria.domain.CreateCafeteriaRequest
import app.hyuabot.backend.cafeteria.domain.MenuListResponse
import app.hyuabot.backend.cafeteria.domain.MenuRequest
import app.hyuabot.backend.cafeteria.domain.MenuResponse
import app.hyuabot.backend.cafeteria.domain.UpdateCafeteriaRequest
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.cafeteria.exception.DuplicateCafeteriaIDException
import app.hyuabot.backend.cafeteria.exception.MenuNotFoundException
import app.hyuabot.backend.campus.exception.CampusNotFoundException
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
import java.time.LocalDate

@RequestMapping(path = ["/api/v1/cafeteria"])
@RestController
@Tag(name = "Cafeteria", description = "학식 식당 관련 API")
class CafeteriaController {
    @Autowired private lateinit var cafeteriaService: CafeteriaService

    @Autowired private lateinit var menuService: MenuService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "학식 식당 목록 조회", description = "학식 식당 목록을 조회합니다. campusID와 name 파라미터를 통해 필터링할 수 있습니다.")
    @ApiResponse(
        responseCode = "200",
        description = "학식 식당 목록 조회 성공",
        content = [
            Content(
                schema =
                    Schema(
                        implementation = CafeteriaListResponse::class,
                        description = "학식 식당 목록",
                    ),
                examples = [
                    ExampleObject(
                        name = "식당 목록 조회 성공 예시",
                        value = """
                            {
                                "result": [
                                    {
                                        "seq": 1,
                                        "campusID": 1,
                                        "name": "학생회관 식당",
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "breakfastTime": "08:00-09:30",
                                        "lunchTime": "11:30-13:30",
                                        "dinnerTime": "17:00-19:00"
                                    },
                                    {
                                        "seq": 2,
                                        "campusID": 1,
                                        "name": "과학관 식당",
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "breakfastTime": "08:00-09:30",
                                        "lunchTime": "11:30-13:30",
                                        "dinnerTime": "17:00-19:00"
                                    }
                                ]
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    fun getCafeteriaList(
        @RequestParam(value = "campusID", required = false) campusID: Int?,
        @RequestParam(value = "name", required = false) name: String? = null,
    ): CafeteriaListResponse =
        CafeteriaListResponse(
            cafeteriaService.getCafeteriaList(campusID, name).map {
                CafeteriaResponse(
                    seq = it.id,
                    campusID = it.campusID,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    breakfastTime = it.breakfastTime,
                    lunchTime = it.lunchTime,
                    dinnerTime = it.dinnerTime,
                )
            },
        )

    @PostMapping(path = [""], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "학식 식당 생성",
        description = "새로운 학식 식당을 생성합니다. 요청 본문에 학식 식당 정보를 포함해야 합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "학식 식당 생성 요청 본문",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CreateCafeteriaRequest::class),
                        examples = [
                            ExampleObject(
                                name = "학식 식당 생성 예시",
                                value = """
                                {
                                    "id": 1,
                                    "campusID": 1,
                                    "name": "학생회관 식당",
                                    "latitude": 37.5665,
                                    "longitude": 126.978,
                                    "breakfastTime": "08:00-09:30",
                                    "lunchTime": "11:30-13:30",
                                    "dinnerTime": "17:00-19:00"
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
                description = "학식 식당 생성 성공",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = CafeteriaResponse::class,
                                description = "생성된 학식 식당 정보",
                            ),
                        examples = [
                            ExampleObject(
                                name = "학식 식당 생성 성공 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "campusID": 1,
                                        "name": "학생회관 식당",
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "breakfastTime": "08:00-09:30",
                                        "lunchTime": "11:30-13:30",
                                        "dinnerTime": "17:00-19:00"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "학식 식당 생성 실패 - 이미 존재하는 ID",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "학식 식당 ID 중복 예시",
                                value = "{\"message\": \"DUPLICATE_CAFETERIA_ID\"}",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "학식 식당 생성 실패 - 캠퍼스 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "캠퍼스 없음 예시",
                                value = "{\"message\": \"CAMPUS_NOT_FOUND\"}",
                            ),
                        ],
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
    fun createCafeteria(
        @RequestBody payload: CreateCafeteriaRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                cafeteriaService.createCafeteria(payload).let {
                    CafeteriaResponse(
                        seq = it.id,
                        campusID = it.campusID,
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        breakfastTime = it.breakfastTime,
                        lunchTime = it.lunchTime,
                        dinnerTime = it.dinnerTime,
                    )
                },
            )
        } catch (_: CampusNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CAMPUS_NOT_FOUND"),
            )
        } catch (_: DuplicateCafeteriaIDException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_CAFETERIA_ID"),
            )
        } catch (e: Exception) {
            logger.error("Error creating cafeteria: ${e.message}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{seq}")
    @Operation(
        summary = "학식 식당 상세 조회",
        description = "학식 식당의 상세 정보를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "학식 식당의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
        ],
    )
    @ApiResponse(
        responseCode = "200",
        description = "학식 식당 상세 조회 성공",
        content = [
            Content(
                schema =
                    Schema(
                        implementation = CafeteriaResponse::class,
                        description = "학식 식당 상세 정보",
                    ),
                examples = [
                    ExampleObject(
                        name = "학식 식당 상세 조회 성공 예시",
                        value = """
                            {
                                "seq": 1,
                                "campusID": 1,
                                "name": "학생회관 식당",
                                "latitude": 37.5665,
                                "longitude": 126.978,
                                "breakfastTime": "08:00-09:30",
                                "lunchTime": "11:30-13:30",
                                "dinnerTime": "17:00-19:00"
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @ApiResponse(
        responseCode = "404",
        description = "학식 식당을 찾을 수 없음",
        content = [
            Content(
                schema = Schema(implementation = ResponseBuilder.Message::class),
                examples = [
                    ExampleObject(
                        name = "학식 식당 없음 예시",
                        value = "{\"message\": \"CAFETERIA_NOT_FOUND\"}",
                    ),
                ],
            ),
        ],
    )
    fun getCafeteriaById(
        @PathVariable("seq") seq: Int,
    ): ResponseEntity<*> =
        try {
            val cafeteria = cafeteriaService.getCafeteriaById(seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                CafeteriaResponse(
                    seq = cafeteria.id,
                    campusID = cafeteria.campusID,
                    name = cafeteria.name,
                    latitude = cafeteria.latitude,
                    longitude = cafeteria.longitude,
                    breakfastTime = cafeteria.breakfastTime,
                    lunchTime = cafeteria.lunchTime,
                    dinnerTime = cafeteria.dinnerTime,
                ),
            )
        } catch (_: CafeteriaNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CAFETERIA_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving cafeteria by ID: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{seq}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "학식 식당 정보 수정",
        description = "학식 식당의 정보를 수정합니다. 요청 본문에 수정할 학식 식당 정보를 포함해야 합니다.",
        parameters = [
            Parameter(
                name = "seq",
                description = "수정할 학식 식당의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "학식 식당 수정 요청 본문",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = UpdateCafeteriaRequest::class),
                        examples = [
                            ExampleObject(
                                name = "학식 식당 수정 예시",
                                value = """
                                    {
                                        "campusID": 1,
                                        "name": "학생회관 식당",
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "breakfastTime": "08:00-09:30",
                                        "lunchTime": "11:30-13:30",
                                        "dinnerTime": "17:00-19:00"
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
                description = "학식 식당 정보 수정 성공",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = CafeteriaResponse::class,
                                description = "수정된 학식 식당 정보",
                            ),
                        examples = [
                            ExampleObject(
                                name = "학식 식당 수정 성공 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "campusID": 1,
                                        "name": "학생회관 식당",
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "breakfastTime": "08:00-09:30",
                                        "lunchTime": "11:30-13:30",
                                        "dinnerTime": "17:00-19:00"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "학식 식당을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "학식 식당 없음 예시",
                                value = "{\"message\": \"CAFETERIA_NOT_FOUND\"}",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "학식 식당 생성 실패 - 캠퍼스 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "캠퍼스 없음 예시",
                                value = "{\"message\": \"CAMPUS_NOT_FOUND\"}",
                            ),
                        ],
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
    fun updateCafeteria(
        @PathVariable("seq") seq: Int,
        @RequestBody payload: UpdateCafeteriaRequest,
    ): ResponseEntity<*> =
        try {
            val updatedCafeteria = cafeteriaService.updateCafeteria(seq, payload)
            ResponseBuilder.response(
                HttpStatus.OK,
                CafeteriaResponse(
                    seq = updatedCafeteria.id,
                    campusID = updatedCafeteria.campusID,
                    name = updatedCafeteria.name,
                    latitude = updatedCafeteria.latitude,
                    longitude = updatedCafeteria.longitude,
                    breakfastTime = updatedCafeteria.breakfastTime,
                    lunchTime = updatedCafeteria.lunchTime,
                    dinnerTime = updatedCafeteria.dinnerTime,
                ),
            )
        } catch (_: CafeteriaNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CAFETERIA_NOT_FOUND"),
            )
        } catch (_: CampusNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CAMPUS_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error updating cafeteria: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "학식 식당 삭제",
        description = "학식 식당을 삭제합니다. 해당 ID의 학식 식당이 존재하지 않으면 404 오류를 반환합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "삭제할 학식 식당의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "학식 식당 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "학식 식당을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "학식 식당 없음 예시",
                                value = "{\"message\": \"CAFETERIA_NOT_FOUND\"}",
                            ),
                        ],
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
    fun deleteCafeteriaById(
        @PathVariable("seq") seq: Int,
    ): ResponseEntity<*> =
        try {
            cafeteriaService.deleteCafeteriaById(seq)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: CafeteriaNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CAFETERIA_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting cafeteria: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{seq}/menu")
    @Operation(
        summary = "메뉴 목록 조회",
        description = "메뉴 목록을 조회합니다. 필터링을 위해 쿼리 파라미터를 사용할 수 있습니다.",
        parameters = [
            Parameter(
                name = "date",
                description = "날짜 (yyyy-MM-dd 형식)",
                required = false,
            ),
            Parameter(
                name = "type",
                description = "식사 종류 (예: 조식, 중식, 석식)",
                required = false,
            ),
        ],
    )
    @ApiResponse(
        responseCode = "200",
        description = "메뉴 목록 조회 성공",
        content = [
            Content(
                schema =
                    Schema(
                        implementation = MenuListResponse::class,
                        description = "메뉴 목록",
                    ),
                examples = [
                    ExampleObject(
                        name = "메뉴 목록 예시",
                        value = """
                            {
                                "result": [
                                    {
                                        "seq": 1,
                                        "cafeteriaID": 101,
                                        "date": "2023-10-01",
                                        "type": "조식",
                                        "food": "김치찌개, 밥, 계란후라이",
                                        "price": "3000"
                                    },
                                    {
                                        "seq": 2,
                                        "cafeteriaID": 101,
                                        "date": "2023-10-01",
                                        "type": "중식",
                                        "food": "제육볶음, 밥, 김치",
                                        "price": "4000"
                                    }
                                ]
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    fun getMenuList(
        @PathVariable("seq") seq: Int,
        @Parameter(description = "날짜 (yyyy-MM-dd 형식)", required = false) date: String?,
        @Parameter(description = "식사 종류 (예: 조식, 중식, 석식)", required = false) type: String?,
    ): ResponseEntity<*> =
        try {
            cafeteriaService.getCafeteriaById(seq)
            val menuList =
                menuService.getMenuList(
                    cafeteriaID = seq,
                    date = date?.let { LocalDate.parse(it) },
                    type = type,
                )
            ResponseBuilder.response(
                HttpStatus.OK,
                MenuListResponse(
                    result =
                        menuList.map {
                            MenuResponse(
                                seq = it.seq!!,
                                cafeteriaID = it.restaurantID,
                                date = it.date.toString(),
                                type = it.type,
                                food = it.food,
                                price = it.price,
                            )
                        },
                ),
            )
        } catch (_: CafeteriaNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CAFETERIA_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving menu list: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PostMapping(path = ["/{seq}/menu"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "메뉴 생성",
        description = "새로운 메뉴를 생성합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "메뉴 생성 요청",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = MenuRequest::class),
                        examples = [
                            ExampleObject(
                                name = "메뉴 생성 예시",
                                value = """
                                {
                                    "cafeteriaID": 101,
                                    "date": "2023-10-01",
                                    "type": "조식",
                                    "food": "김치찌개, 밥, 계란후라이",
                                    "price": "3000"
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
                description = "메뉴 생성 성공",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = MenuResponse::class,
                                description = "생성된 메뉴 정보",
                            ),
                        examples = [
                            ExampleObject(
                                name = "메뉴 생성 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "cafeteriaID": 101,
                                        "date": "2023-10-01",
                                        "type": "조식",
                                        "food": "김치찌개, 밥, 계란후라이",
                                        "price": "3000"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (존재하지 않는 식당 ID)",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "캠퍼스 없음 예시",
                                value = "{\"message\": \"CAFETERIA_NOT_FOUND\"}",
                            ),
                        ],
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
    fun createMenu(
        @PathVariable("seq") seq: Int,
        @RequestBody payload: MenuRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                menuService.createMenu(seq, payload).let {
                    MenuResponse(
                        seq = it.seq!!,
                        cafeteriaID = it.restaurantID,
                        date = it.date.toString(),
                        type = it.type,
                        food = it.food,
                        price = it.price,
                    )
                },
            )
        } catch (_: CafeteriaNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CAFETERIA_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("메뉴 생성 중 오류 발생", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{seq}/menu/{menuSeq}")
    @Operation(
        summary = "메뉴 상세 조회",
        description = "메뉴의 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "seq",
                description = "학식 식당의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
            Parameter(
                name = "menuSeq",
                description = "메뉴의 고유 ID",
                required = true,
                schema = Schema(type = "integer"),
            ),
        ],
    )
    @ApiResponse(
        responseCode = "200",
        description = "메뉴 상세 조회 성공",
        content = [
            Content(
                schema =
                    Schema(
                        implementation = MenuResponse::class,
                        description = "메뉴 상세 정보",
                    ),
                examples = [
                    ExampleObject(
                        name = "메뉴 상세 조회 예시",
                        value = """
                            {
                                "seq": 1,
                                "cafeteriaID": 101,
                                "date": "2023-10-01",
                                "type": "조식",
                                "food": "김치찌개, 밥, 계란후라이",
                                "price": "3000"
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @ApiResponse(
        responseCode = "404",
        description = "메뉴를 찾을 수 없음",
        content = [
            Content(
                schema = Schema(implementation = ResponseBuilder.Message::class),
                examples = [
                    ExampleObject(
                        name = "메뉴 없음 예시",
                        value = "{\"message\": \"MENU_NOT_FOUND\"}",
                    ),
                ],
            ),
        ],
    )
    fun getMenuById(
        @PathVariable("seq") seq: Int,
        @PathVariable("menuSeq") menuSeq: Int,
    ): ResponseEntity<*> =
        try {
            cafeteriaService.getCafeteriaById(seq)
            val menu = menuService.getMenuById(seq, menuSeq)
            ResponseBuilder.response(
                HttpStatus.OK,
                MenuResponse(
                    seq = menu.seq!!,
                    cafeteriaID = menu.restaurantID,
                    date = menu.date.toString(),
                    type = menu.type,
                    food = menu.food,
                    price = menu.price,
                ),
            )
        } catch (_: CafeteriaNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CAFETERIA_NOT_FOUND"),
            )
        } catch (_: MenuNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("MENU_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("메뉴 상세 조회 중 오류 발생", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{seq}/menu/{menuSeq}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "메뉴 수정",
        description = "기존 메뉴를 수정합니다.",
        parameters = [
            Parameter(
                name = "seq",
                description = "학식 식당의 고유 ID",
                required = true,
            ),
            Parameter(
                name = "menuSeq",
                description = "수정할 메뉴의 고유 ID",
                required = true,
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "메뉴 수정 요청",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = MenuRequest::class),
                        examples = [
                            ExampleObject(
                                name = "메뉴 수정 예시",
                                value = """
                                    {
                                        "date": "2023-10-01",
                                        "type": "조식",
                                        "food": "김치찌개, 밥, 계란후라이",
                                        "price": "3000"
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
                description = "메뉴 수정 성공",
                content = [
                    Content(
                        schema =
                            Schema(
                                implementation = MenuResponse::class,
                                description = "수정된 메뉴 정보",
                            ),
                        examples = [
                            ExampleObject(
                                name = "메뉴 수정 예시",
                                value = """
                                    {
                                        "seq": 1,
                                        "cafeteriaID": 101,
                                        "date": "2023-10-01",
                                        "type": "조식",
                                        "food": "김치찌개, 밥, 계란후라이",
                                        "price": "3000"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (존재하지 않는 식당 ID)",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "캠퍼스 없음 예시",
                                value = "{\"message\": \"CAFETERIA_NOT_FOUND\"}",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "메뉴를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "메뉴 없음 예시",
                                value = "{\"message\": \"MENU_NOT_FOUND\"}",
                            ),
                        ],
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
    fun updateMenu(
        @PathVariable("seq") seq: Int,
        @PathVariable("menuSeq") menuSeq: Int,
        @RequestBody payload: MenuRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.OK,
                menuService.updateMenu(seq, menuSeq, payload).let {
                    MenuResponse(
                        seq = it.seq!!,
                        cafeteriaID = it.restaurantID,
                        date = it.date.toString(),
                        type = it.type,
                        food = it.food,
                        price = it.price,
                    )
                },
            )
        } catch (_: CafeteriaNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CAFETERIA_NOT_FOUND"),
            )
        } catch (_: MenuNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("MENU_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("메뉴 수정 중 오류 발생", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @DeleteMapping("/{seq}/menu/{menuSeq}")
    @Operation(
        summary = "메뉴 삭제",
        description = "기존 메뉴를 삭제합니다.",
        parameters = [
            Parameter(
                name = "seq",
                description = "학식 식당의 고유 ID",
                required = true,
            ),
            Parameter(
                name = "menuSeq",
                description = "삭제할 메뉴의 고유 ID",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "메뉴 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "메뉴를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "메뉴 없음 예시",
                                value = "{\"message\": \"MENU_NOT_FOUND\"}",
                            ),
                        ],
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
    fun deleteMenuById(
        @PathVariable("seq") seq: Int,
        @PathVariable("menuSeq") menuSeq: Int,
    ): ResponseEntity<*> =
        try {
            menuService.deleteMenuById(seq, menuSeq)
            ResponseEntity.noContent().build<Any>()
        } catch (_: MenuNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("MENU_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("메뉴 삭제 중 오류 발생", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
