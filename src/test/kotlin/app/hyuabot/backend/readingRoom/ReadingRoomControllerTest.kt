package app.hyuabot.backend.readingRoom

import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.entity.ReadingRoom
import app.hyuabot.backend.database.repository.ReadingRoomRepository
import app.hyuabot.backend.readingRoom.domain.UpdateReadingRoomRequest
import app.hyuabot.backend.readingRoom.exception.DuplicateReadingRoomException
import app.hyuabot.backend.readingRoom.exception.ReadingRoomNotFoundException
import app.hyuabot.backend.security.WithCustomMockUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReadingRoomControllerTest {
    @MockitoBean
    private lateinit var readingRoomService: ReadingRoomService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var readingRoomRepository: ReadingRoomRepository

    @BeforeEach
    fun setUp() {
        readingRoomRepository.deleteAll()
    }

    @Test
    @DisplayName("열람실 목록 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetReadingRoomList() {
        // Mocking the service method
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        doReturn(
            listOf(
                ReadingRoom(
                    id = 1,
                    name = "열람실 A",
                    campusID = 1,
                    isActive = true,
                    isReservable = true,
                    total = 10,
                    active = 10,
                    occupied = 0,
                    available = 10,
                    updatedAt = now,
                    campus = Campus(id = 1, name = "서울"),
                ),
                ReadingRoom(
                    id = 2,
                    name = "열람실 B",
                    campusID = 2,
                    isActive = true,
                    isReservable = true,
                    total = 20,
                    active = 15,
                    occupied = 5,
                    updatedAt = now,
                    campus = Campus(id = 2, name = "ERICA"),
                ),
            ),
        ).whenever(readingRoomService).getReadingRoomList(null)

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/reading-room"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].name").value("열람실 A"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].name").value("열람실 B"))
    }

    @Test
    @DisplayName("열람실 목록 조회 테스트 (키워드 검색)")
    @WithCustomMockUser(username = "test_user")
    fun testGetReadingRoomListWithKeyword() {
        // Mocking the service method
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        doReturn(
            listOf(
                ReadingRoom(
                    id = 1,
                    name = "열람실 A",
                    campusID = 1,
                    isActive = true,
                    isReservable = true,
                    total = 10,
                    active = 10,
                    occupied = 0,
                    available = 10,
                    updatedAt = now,
                    campus = Campus(id = 1, name = "서울"),
                ),
            ),
        ).whenever(readingRoomService).getReadingRoomList("A")

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/reading-room").param("name", "A"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].name").value("열람실 A"))
    }

    @Test
    @DisplayName("열람실 생성 테스트")
    @WithCustomMockUser(username = "test_user")
    fun createReadingRoomTest() {
        // Mocking the service method
        val newRoom =
            ReadingRoom(
                id = 999,
                name = "열람실 C",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 30,
                active = 30,
                occupied = 0,
                available = 30,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = null,
            )
        doReturn(newRoom)
            .whenever(readingRoomService)
            .createReadingRoom(any())

        // Perform POST request and verify response
        mockMvc
            .perform(
                post("/api/v1/reading-room")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "id" to 999,
                                "name" to "열람실 C",
                                "campusID" to 1,
                                "total" to 30,
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(999))
            .andExpect(jsonPath("$.name").value("열람실 C"))
            .andExpect(jsonPath("$.campusID").value(1))
            .andExpect(jsonPath("$.total").value(30))
    }

    @Test
    @DisplayName("열람실 생성 테스트")
    @WithCustomMockUser(username = "test_user")
    fun createReadingRoomNullAvailableTest() {
        // Mocking the service method
        val newRoom =
            ReadingRoom(
                id = 999,
                name = "열람실 C",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 30,
                active = 30,
                occupied = 0,
                available = 30,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = null,
            )
        doReturn(newRoom)
            .whenever(readingRoomService)
            .createReadingRoom(any())

        // Perform POST request and verify response
        mockMvc
            .perform(
                post("/api/v1/reading-room")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "id" to 999,
                                "name" to "열람실 C",
                                "campusID" to 1,
                                "total" to 30,
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(999))
            .andExpect(jsonPath("$.name").value("열람실 C"))
            .andExpect(jsonPath("$.campusID").value(1))
            .andExpect(jsonPath("$.total").value(30))
    }

    @Test
    @DisplayName("열람실 생성 실패 테스트 (중복 ID)")
    @WithCustomMockUser(username = "test_user")
    fun createReadingRoomDuplicateIDTest() {
        // Mocking the service method to throw an exception
        doThrow(DuplicateReadingRoomException::class)
            .whenever(readingRoomService)
            .createReadingRoom(any())

        // Perform POST request and verify response
        mockMvc
            .perform(
                post("/api/v1/reading-room")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "id" to 999,
                                "name" to "열람실 C",
                                "campusID" to 1,
                                "total" to 30,
                            ),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_READING_ROOM_ID"))
    }

    @Test
    @DisplayName("열람실 생성 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun createReadingRoomOtherErrorTest() {
        // Mocking the service method to throw an exception
        doThrow(RuntimeException("DB_ERROR"))
            .whenever(readingRoomService)
            .createReadingRoom(any())
        // Perform POST request and verify response
        mockMvc
            .perform(
                post("/api/v1/reading-room")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "id" to 999,
                                "name" to "열람실 C",
                                "campusID" to 1,
                                "total" to 30,
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("열람실 ID로 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetReadingRoomById() {
        // Mocking the service method
        val room =
            ReadingRoom(
                id = 1,
                name = "열람실 A",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 10,
                active = 10,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = Campus(id = 1, name = "서울"),
            )
        doReturn(room).whenever(readingRoomService).getReadingRoomById(1)

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/reading-room/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("열람실 A"))
    }

    @Test
    @DisplayName("열람실 ID로 조회 실패 테스트 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun testGetReadingRoomByIdNotFound() {
        // Mocking the service method to throw an exception
        doThrow(ReadingRoomNotFoundException::class)
            .whenever(readingRoomService)
            .getReadingRoomById(999)

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/reading-room/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("READING_ROOM_NOT_FOUND"))
    }

    @Test
    @DisplayName("열람실 ID로 조회 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun testGetReadingRoomByIdOtherError() {
        // Mocking the service method to throw an exception
        doThrow(RuntimeException("기타 오류"))
            .whenever(readingRoomService)
            .getReadingRoomById(999)

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/reading-room/999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("열람실 ID로 수정 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateReadingRoom() {
        // Mocking the service method
        val existingRoom =
            ReadingRoom(
                id = 1,
                name = "열람실 A 수정",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 15,
                active = 15,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = Campus(id = 1, name = "서울"),
            )
        doReturn(existingRoom)
            .whenever(readingRoomService)
            .updateReadingRoom(
                1,
                UpdateReadingRoomRequest(
                    name = "열람실 A 수정",
                    campusID = 1,
                    total = 15,
                    active = 15,
                    isActive = true,
                    isReservable = true,
                ),
            )

        // Perform PUT request to update reading room and verify response
        mockMvc
            .perform(
                put("/api/v1/reading-room/1")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "name" to "열람실 A 수정",
                                "campusID" to 1,
                                "total" to 15,
                                "active" to 15,
                                "isActive" to true,
                                "isReservable" to true,
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("열람실 A 수정"))
            .andExpect(jsonPath("$.campusID").value(1))
            .andExpect(jsonPath("$.total").value(15))
            .andExpect(jsonPath("$.active").value(15))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.isReservable").value(true))
    }

    @Test
    @DisplayName("열람실 ID로 수정 실패 테스트 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateReadingRoomNotFound() {
        // Mocking the service method to throw an exception
        doThrow(ReadingRoomNotFoundException::class)
            .whenever(readingRoomService)
            .updateReadingRoom(
                999,
                UpdateReadingRoomRequest(
                    name = "열람실 A 수정",
                    campusID = 1,
                    total = 15,
                    active = 15,
                    isActive = true,
                    isReservable = true,
                ),
            )

        // Perform PUT request to update reading room and verify response
        mockMvc
            .perform(
                put("/api/v1/reading-room/999")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "name" to "열람실 A 수정",
                                "campusID" to 1,
                                "total" to 15,
                                "active" to 15,
                                "isActive" to true,
                                "isReservable" to true,
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("READING_ROOM_NOT_FOUND"))
    }

    @Test
    @DisplayName("열람실 ID로 수정 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateReadingRoomOtherError() {
        // Mocking the service method to throw an exception
        doThrow(RuntimeException::class)
            .whenever(readingRoomService)
            .updateReadingRoom(
                999,
                UpdateReadingRoomRequest(
                    name = "열람실 A 수정",
                    campusID = 1,
                    total = 15,
                    active = 15,
                    isActive = true,
                    isReservable = true,
                ),
            )

        // Perform PUT request to update reading room and verify response
        mockMvc
            .perform(
                put("/api/v1/reading-room/999")
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "name" to "열람실 A 수정",
                                "campusID" to 1,
                                "total" to 15,
                                "active" to 15,
                                "isActive" to true,
                                "isReservable" to true,
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("열람실 ID로 삭제 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteReadingRoomById() {
        // Mocking the service method
        val room =
            ReadingRoom(
                id = 1,
                name = "열람실 A",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 10,
                active = 10,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = Campus(id = 1, name = "서울"),
            )
        whenever(readingRoomService.getReadingRoomById(1)).thenReturn(room)

        // Perform DELETE request and verify response
        mockMvc
            .perform(delete("/api/v1/reading-room/1"))
            .andExpect(status().isNoContent)

        // Verify that the reading room was deleted
        verify(readingRoomService).deleteReadingRoomById(1)
    }

    @Test
    @DisplayName("열람실 ID로 삭제 실패 테스트 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteReadingRoomByIdNotFound() {
        // Mocking the service method to throw an exception
        doThrow(ReadingRoomNotFoundException::class)
            .whenever(readingRoomService)
            .deleteReadingRoomById(999)

        // Perform DELETE request and verify response
        mockMvc
            .perform(delete("/api/v1/reading-room/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("READING_ROOM_NOT_FOUND"))
    }

    @Test
    @DisplayName("열람실 ID로 삭제 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteReadingRoomByIdOtherError() {
        // Mocking the service method to throw an exception
        doThrow(RuntimeException::class)
            .whenever(readingRoomService)
            .deleteReadingRoomById(999)

        // Perform DELETE request and verify response
        mockMvc
            .perform(delete("/api/v1/reading-room/999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
