package app.hyuabot.backend.readingRoom

import app.hyuabot.backend.readingRoom.domain.ReadingRoomListResponse
import app.hyuabot.backend.readingRoom.domain.ReadingRoomResponse
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/v1/reading-room"])
@RestController
@Tag(name = "Reading Room", description = "도서관 열람실 관련 API")
class ReadingRoomController {
    @Autowired lateinit var readingRoomService: ReadingRoomService
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
    fun getReadingRoomList(): ReadingRoomListResponse =
        ReadingRoomListResponse(
            result =
                readingRoomService
                    .getReadingRoomList()
                    .filter { it.available != null }
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
                            available = it.available ?: 0,
                            updatedAt =
                                LocalDateTimeBuilder.convertLocalDateTimeToString(
                                    LocalDateTimeBuilder.convertAsServiceTimezone(it.updatedAt),
                                ),
                        )
                    },
        )
}
