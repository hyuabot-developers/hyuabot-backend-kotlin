package app.hyuabot.backend.menu

import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.menu.domain.MenuListResponse
import app.hyuabot.backend.menu.domain.MenuRequest
import app.hyuabot.backend.menu.domain.MenuResponse
import app.hyuabot.backend.menu.exception.MenuNotFoundException
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
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RequestMapping(path = ["/api/v1/menu"])
@RestController
@Tag(name = "Menu", description = "메뉴 관련 API")
class MenuController {
    @Autowired private lateinit var menuService: MenuService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(
        summary = "메뉴 목록 조회",
        description = "메뉴 목록을 조회합니다. 필터링을 위해 쿼리 파라미터를 사용할 수 있습니다.",
        parameters = [
            Parameter(
                name = "cafeteriaID",
                description = "식당 ID",
                required = false,
            ),
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
        @Parameter(description = "식당 ID", required = false) cafeteriaID: Int?,
        @Parameter(description = "날짜 (yyyy-MM-dd 형식)", required = false) date: String?,
        @Parameter(description = "식사 종류 (예: 조식, 중식, 석식)", required = false) type: String?,
    ): MenuListResponse =
        MenuListResponse(
            result =
                menuService
                    .getMenuList(
                        cafeteriaID = cafeteriaID,
                        date = date?.let { LocalDate.parse(it) },
                        type = type,
                    ).map {
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

    @PostMapping(path = [""], consumes = [MediaType.APPLICATION_JSON_VALUE])
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
        @RequestBody payload: MenuRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                menuService.createMenu(payload).let {
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

    @GetMapping("/{seq}")
    @Operation(
        summary = "메뉴 상세 조회",
        description = "메뉴의 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "seq",
                description = "메뉴의 고유 ID (시퀀스 번호)",
                required = true,
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
    ): ResponseEntity<*> =
        try {
            val menu = menuService.getMenuById(seq)
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

    @PutMapping("/{seq}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "메뉴 수정",
        description = "기존 메뉴를 수정합니다.",
        parameters = [
            Parameter(
                name = "seq",
                description = "수정할 메뉴의 고유 ID (시퀀스 번호)",
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
        @RequestBody payload: MenuRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.OK,
                menuService.updateMenu(seq, payload).let {
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

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "메뉴 삭제",
        description = "기존 메뉴를 삭제합니다.",
        parameters = [
            Parameter(
                name = "seq",
                description = "삭제할 메뉴의 고유 ID (시퀀스 번호)",
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
    ): ResponseEntity<*> =
        try {
            menuService.deleteMenuById(seq)
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
