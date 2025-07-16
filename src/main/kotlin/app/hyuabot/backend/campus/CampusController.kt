package app.hyuabot.backend.campus

import app.hyuabot.backend.campus.domain.CampusListResponse
import app.hyuabot.backend.campus.domain.CampusResponse
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
                    result = campusList.map { CampusResponse(it.id, it.name) },
                ),
            )
        }
}
