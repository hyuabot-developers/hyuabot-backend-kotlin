package app.hyuabot.backend.building

import app.hyuabot.backend.building.domain.BuildingListResponse
import app.hyuabot.backend.building.domain.BuildingResponse
import app.hyuabot.backend.building.domain.CreateBuildingRequest
import app.hyuabot.backend.building.domain.RoomListResponse
import app.hyuabot.backend.building.domain.RoomRequest
import app.hyuabot.backend.building.domain.RoomResponse
import app.hyuabot.backend.building.domain.UpdateBuildingRequest
import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.building.exception.DuplicateBuildingNameException
import app.hyuabot.backend.building.exception.DuplicateRoomException
import app.hyuabot.backend.building.exception.RoomNotFoundException
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

@RequestMapping(path = ["/api/v1/building"])
@RestController
@Tag(name = "Building", description = "교내 지도 건물 정보 API")
class BuildingController {
    @Autowired private lateinit var buildingService: BuildingService

    @Autowired private lateinit var roomService: RoomService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping(path = [""])
    @Operation(summary = "건물 목록 조회", description = "교내 건물 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "건물 목록 조회 성공",
        content = [
            Content(
                schema = Schema(implementation = BuildingListResponse::class),
                examples = [
                    ExampleObject(
                        name = "건물 목록 예시",
                        value = """
                            {
                                "result": [
                                    {
                                        "id": "1",
                                        "name": "중앙도서관",
                                        "campusID": 1,
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "url": "https://example.com/building/1"
                                    },
                                    {
                                        "id": "2",
                                        "name": "공학관",
                                        "campusID": 1,
                                        "latitude": 37.5670,
                                        "longitude": 126.979,
                                        "url": "https://example.com/building/2"
                                    }
                                ]
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    fun getBuildingList(
        @RequestParam(value = "campusID", required = false) campusID: Int?,
        @RequestParam(value = "name", required = false) name: String? = null,
    ): ResponseEntity<BuildingListResponse> =
        ResponseBuilder.response(
            HttpStatus.OK,
            BuildingListResponse(
                result =
                    buildingService
                        .getBuildingList(campusID, name)
                        .map { building ->
                            BuildingResponse(
                                id = building.id,
                                name = building.name,
                                campusID = building.campusID,
                                latitude = building.latitude,
                                longitude = building.longitude,
                                url = building.url,
                            )
                        },
            ),
        )

    @PostMapping(path = [""], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "건물 생성",
        description = "새로운 교내 건물을 생성합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CreateBuildingRequest::class),
                        examples = [
                            ExampleObject(
                                name = "건물 생성 예시",
                                value = """
                                {
                                    "id": "3",
                                    "name": "신축관",
                                    "campusID": 1,
                                    "latitude": 37.5680,
                                    "longitude": 126.980,
                                    "url": "https://example.com/building/3"
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
                description = "건물 생성 성공",
                content = [
                    Content(
                        schema = Schema(implementation = BuildingResponse::class),
                        examples = [
                            ExampleObject(
                                name = "건물 생성 응답 예시",
                                value = """
                                    {
                                        "id": "3",
                                        "name": "신축관",
                                        "campusID": 1,
                                        "latitude": 37.5680,
                                        "longitude": 126.980,
                                        "url": "https://example.com/building/3"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "건물 이름 중복",
                content =
                    [
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "건물 이름 중복 예시",
                                        value = "{\"message\": \"DUPLICATE_BUILDING_NAME\"}",
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
    fun createBuilding(
        @RequestBody payload: CreateBuildingRequest,
    ): ResponseEntity<*> {
        try {
            buildingService.createBuilding(payload).let {
                return ResponseBuilder.response(
                    HttpStatus.CREATED,
                    BuildingResponse(
                        id = payload.id,
                        name = payload.name,
                        campusID = payload.campusID,
                        latitude = payload.latitude,
                        longitude = payload.longitude,
                        url = payload.url,
                    ),
                )
            }
        } catch (_: DuplicateBuildingNameException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_BUILDING_NAME"),
            )
        } catch (e: Exception) {
            logger.error("Error creating building: ${e.message}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{name}")
    @Operation(
        summary = "건물 상세 조회",
        description = "특정 건물의 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "조회할 건물의 이름",
                required = true,
                schema = Schema(type = "string"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "건물 상세 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = BuildingResponse::class),
                        examples = [
                            ExampleObject(
                                name = "건물 상세 조회 예시",
                                value = """
                                    {
                                        "id": "1",
                                        "name": "중앙도서관",
                                        "campusID": 1,
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "url": "https://example.com/building/1"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "건물 이름을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "건물 이름 없음 예시",
                                value = "{\"message\": \"BUILDING_NOT_FOUND\"}",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getBuildingDetail(
        @PathVariable("name") name: String,
    ): ResponseEntity<*> =
        try {
            val building = buildingService.getBuildingByName(name)
            ResponseBuilder.response(
                HttpStatus.OK,
                BuildingResponse(
                    id = building.id,
                    name = building.name,
                    campusID = building.campusID,
                    latitude = building.latitude,
                    longitude = building.longitude,
                    url = building.url,
                ),
            )
        } catch (_: BuildingNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUILDING_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving building: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{name}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "건물 정보 수정",
        description = "특정 건물의 정보를 수정합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "수정할 건물의 이름",
                required = true,
                schema = Schema(type = "string"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = UpdateBuildingRequest::class),
                        examples = [
                            ExampleObject(
                                name = "건물 수정 예시",
                                value = """
                                    {
                                        "id": "1",
                                        "campusID": 1,
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "url": "https://example.com/building/1"
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
                description = "건물 정보 수정 성공",
                content = [
                    Content(
                        schema = Schema(implementation = BuildingResponse::class),
                        examples = [
                            ExampleObject(
                                name = "건물 수정 응답 예시",
                                value = """
                                    {
                                        "id": "1",
                                        "name": "중앙도서관",
                                        "campusID": 1,
                                        "latitude": 37.5665,
                                        "longitude": 126.978,
                                        "url": "https://example.com/building/1"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "건물 이름을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "건물 이름 없음 예시",
                                value = "{\"message\": \"BUILDING_NOT_FOUND\"}",
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
    fun updateBuilding(
        @PathVariable("name") name: String,
        @RequestBody payload: UpdateBuildingRequest,
    ): ResponseEntity<*> =
        try {
            buildingService.updateBuilding(name, payload).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    BuildingResponse(
                        id = payload.id,
                        name = name,
                        campusID = payload.campusID,
                        latitude = payload.latitude,
                        longitude = payload.longitude,
                        url = payload.url,
                    ),
                )
            }
        } catch (_: BuildingNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUILDING_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error updating building: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{name}")
    @Operation(
        summary = "건물 삭제",
        description = "특정 건물을 삭제합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "삭제할 건물의 이름",
                required = true,
                schema = Schema(type = "string"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "건물 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "건물 이름을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "건물 이름 없음 예시",
                                value = "{\"message\": \"BUILDING_NOT_FOUND\"}",
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
    fun deleteBuilding(
        @PathVariable("name") name: String,
    ): ResponseEntity<*> =
        try {
            buildingService.deleteBuildingByName(name)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: BuildingNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUILDING_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting building: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping(path = ["/{name}/room"])
    @Operation(
        summary = "교내 강의실 목록 조회",
        description = "특정 건물의 교내 강의실 목록을 조회합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "조회할 건물의 이름",
                required = true,
                schema = Schema(type = "string"),
            ),
        ],
    )
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
        @PathVariable("name") building: String,
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
                                seq = it.seq!!,
                                buildingName = it.buildingName,
                                number = it.number,
                                name = it.name,
                            )
                        }.sortedBy { it.seq },
            ),
        )

    @PostMapping(path = ["/{name}/room"], consumes = [MediaType.APPLICATION_JSON_VALUE])
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
        @PathVariable("name") building: String,
        @RequestBody payload: RoomRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                roomService.createRoom(building, payload).let {
                    RoomResponse(
                        seq = it.seq!!,
                        buildingName = building,
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

    @GetMapping(path = ["/{name}/room/{seq}"])
    @Operation(
        summary = "교내 강의실 상세 조회",
        description = "교내 강의실의 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "조회할 건물의 이름",
                required = true,
                schema = Schema(type = "string"),
            ),
            Parameter(
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
        @PathVariable("name") building: String,
        @PathVariable("seq") seq: Int,
    ): ResponseEntity<*> =
        try {
            buildingService.getBuildingByName(building)
            ResponseBuilder.response(
                HttpStatus.OK,
                roomService.getRoomByID(building, seq).let {
                    RoomResponse(
                        seq = it.seq!!,
                        buildingName = building,
                        number = it.number,
                        name = it.name,
                    )
                },
            )
        } catch (_: BuildingNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUILDING_NOT_FOUND"),
            )
        } catch (_: RoomNotFoundException) {
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

    @PutMapping(path = ["/{name}/room/{seq}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "교내 강의실 수정",
        description = "교내 강의실의 정보를 수정합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "수정할 건물의 이름",
                required = true,
                schema = Schema(type = "string"),
            ),
            Parameter(
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
        @PathVariable("name") building: String,
        @PathVariable("seq") seq: Int,
        @RequestBody payload: RoomRequest,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(
                HttpStatus.OK,
                roomService.updateRoom(building, seq, payload).let {
                    RoomResponse(
                        seq = it.seq!!,
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
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUILDING_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to update room: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping(path = ["/{name}/room/{seq}"])
    @Operation(
        summary = "교내 강의실 삭제",
        description = "교내 강의실을 삭제합니다.",
        parameters = [
            Parameter(
                name = "name",
                description = "삭제할 건물의 이름",
                required = true,
                schema = Schema(type = "string"),
            ),
            Parameter(
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
        @PathVariable("name") building: String,
        @PathVariable("seq") seq: Int,
    ): ResponseEntity<*> =
        try {
            roomService.deleteRoom(building, seq)
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
