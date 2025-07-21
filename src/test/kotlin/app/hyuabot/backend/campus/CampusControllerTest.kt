package app.hyuabot.backend.campus

import app.hyuabot.backend.campus.domain.CreateCampusRequest
import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.campus.exception.DuplicateCampusException
import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.repository.CampusRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampusControllerTest {
    @MockitoBean
    private lateinit var campusService: CampusService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var campusRepository: CampusRepository

    @BeforeEach
    fun setUp() {
        campusRepository.deleteAll()
    }

    @Test
    @DisplayName("캠퍼스 목록 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetCampusList() {
        // Mocking the service method
        doReturn(
            listOf(
                Campus(id = 1, name = "서울"),
                Campus(id = 2, name = "ERICA"),
            ),
        ).whenever(campusService).getCampusList()

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/campus"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].name").value("서울"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].name").value("ERICA"))
    }

    @Test
    @DisplayName("캠퍼스 생성 테스트")
    @WithCustomMockUser(username = "test_user")
    fun createCampusTest() {
        // Mocking the service method
        val newCampus = Campus(id = 3, name = "신규 캠퍼스")
        doReturn(newCampus)
            .whenever(campusService)
            .createCampus(any())

        // Perform POST request and verify response
        mockMvc
            .perform(
                post("/api/v1/campus")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "신규 캠퍼스"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(3))
            .andExpect(jsonPath("$.name").value("신규 캠퍼스"))
    }

    @Test
    @DisplayName("캠퍼스 생성 실패 테스트 (중복 이름)")
    @WithCustomMockUser(username = "test_user")
    fun createCampusDuplicateNameTest() {
        // Mocking the service method to throw an exception
        doThrow(DuplicateCampusException::class)
            .whenever(campusService)
            .createCampus(any())

        // Perform POST request and verify response
        mockMvc
            .perform(
                post("/api/v1/campus")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "중복 캠퍼스"))),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_CAMPUS_NAME"))
    }

    @Test
    @DisplayName("캠퍼스 생성 실패 테스트 (기타)")
    @WithCustomMockUser(username = "test_user")
    fun createCampusOtherErrorTest() {
        // Mocking the service method to throw an exception
        doThrow(RuntimeException("DB_ERROR"))
            .whenever(campusService)
            .createCampus(CreateCampusRequest(name = "기타 캠퍼스"))
        // Perform POST request and verify response
        mockMvc
            .perform(
                post("/api/v1/campus")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "기타 캠퍼스"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetCampusById() {
        // Mocking the service method
        val campus = Campus(id = 1, name = "서울")
        doReturn(campus).whenever(campusService).getCampusById(1)

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/campus/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("서울"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 조회 실패 테스트 (존재하지 않는 캠퍼스)")
    @WithCustomMockUser(username = "test_user")
    fun testGetCampusByIdNotFound() {
        // Mocking the service method to throw an exception
        doThrow(CampusNotFoundException::class)
            .whenever(campusService)
            .getCampusById(999)

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/campus/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CAMPUS_NOT_FOUND"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 조회 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun testGetCampusByIdOtherError() {
        // Mocking the service method to throw an exception
        doThrow(RuntimeException("기타 오류"))
            .whenever(campusService)
            .getCampusById(999)

        // Perform GET request and verify response
        mockMvc
            .perform(get("/api/v1/campus/999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateCampus() {
        // Mocking the service method
        doReturn(
            Campus(
                id = 1,
                name = "서울 수정",
            ),
        ).whenever(campusService).updateCampus(
            1,
            CreateCampusRequest(name = "서울 수정"),
        )

        // Perform POST request to update campus and verify response
        mockMvc
            .perform(
                put("/api/v1/campus/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "서울 수정"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("서울 수정"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 실패 테스트 (중복 이름)")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateCampusDuplicateName() {
        // Mocking the service method to throw an exception
        doThrow(DuplicateCampusException::class)
            .whenever(campusService)
            .updateCampus(1, CreateCampusRequest(name = "중복 캠퍼스"))

        // Perform POST request to update campus and verify response
        mockMvc
            .perform(
                put("/api/v1/campus/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "중복 캠퍼스"))),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_CAMPUS_NAME"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 실패 테스트 (캠퍼스 없음)")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateCampusNotFound() {
        // Mocking the service method to throw an exception
        doThrow(CampusNotFoundException::class)
            .whenever(campusService)
            .updateCampus(999, CreateCampusRequest(name = "서울 수정"))

        // Perform POST request to update campus and verify response
        mockMvc
            .perform(
                put("/api/v1/campus/999")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "서울 수정"))),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CAMPUS_NOT_FOUND"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateCampusOtherError() {
        // Mocking the service method to throw an exception
        doThrow(RuntimeException::class)
            .whenever(campusService)
            .updateCampus(999, CreateCampusRequest(name = "서울 수정"))

        // Perform POST request to update campus and verify response
        mockMvc
            .perform(
                put("/api/v1/campus/999")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "서울 수정"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 삭제 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteCampusById() {
        // Mocking the service method
        val campus = Campus(id = 1, name = "서울")
        whenever(campusService.getCampusById(1)).thenReturn(campus)

        // Perform DELETE request and verify response
        mockMvc
            .perform(delete("/api/v1/campus/1"))
            .andExpect(status().isNoContent)

        // Verify that the campus was deleted
        verify(campusService).deleteCampusById(1)
    }

    @Test
    @DisplayName("캠퍼스 ID로 삭제 실패 테스트 (존재하지 않는 캠퍼스)")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteCampusByIdNotFound() {
        // Mocking the service method to throw an exception
        doThrow(CampusNotFoundException::class)
            .whenever(campusService)
            .deleteCampusById(999)

        // Perform DELETE request and verify response
        mockMvc
            .perform(delete("/api/v1/campus/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CAMPUS_NOT_FOUND"))
    }

    @Test
    @DisplayName("캠퍼스 ID로 삭제 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteCampusByIdOtherError() {
        // Mocking the service method to throw an exception
        doThrow(DuplicateCampusException::class)
            .whenever(campusService)
            .deleteCampusById(999)

        // Perform DELETE request and verify response
        mockMvc
            .perform(delete("/api/v1/campus/999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
