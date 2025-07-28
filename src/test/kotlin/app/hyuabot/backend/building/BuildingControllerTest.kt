package app.hyuabot.backend.building

import app.hyuabot.backend.building.domain.CreateBuildingRequest
import app.hyuabot.backend.building.domain.UpdateBuildingRequest
import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.building.exception.DuplicateBuildingNameException
import app.hyuabot.backend.database.entity.Building
import app.hyuabot.backend.database.repository.BuildingRepository
import app.hyuabot.backend.security.WithCustomMockUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BuildingControllerTest {
    @MockitoBean
    private lateinit var buildingService: BuildingService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var buildingRepository: BuildingRepository

    @BeforeEach
    fun setUp() {
        buildingRepository.deleteAll()
    }

    @Test
    @DisplayName("건물 목록 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetBuildingList() {
        doReturn(
            listOf(
                Building(
                    id = "YB001",
                    name = "Building A",
                    campusID = 1,
                    latitude = 37.5665,
                    longitude = 126.978,
                    url = "https://example.com/buildingA",
                ),
                Building(
                    id = "Y002",
                    name = "Building B",
                    campusID = 1,
                    latitude = 37.5665,
                    longitude = 126.978,
                    url = "https://example.com/buildingB",
                ),
            ),
        ).whenever(buildingService).getBuildingList()
        // Perform the GET request to the building list endpoint
        mockMvc
            .perform(get("/api/v1/building"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].id").value("YB001"))
            .andExpect(jsonPath("$.result[0].name").value("Building A"))
            .andExpect(jsonPath("$.result[1].id").value("Y002"))
            .andExpect(jsonPath("$.result[1].name").value("Building B"))
    }

    @Test
    @DisplayName("건물 이름으로 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetBuildingByName() {
        val building =
            Building(
                id = "YB001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/buildingA",
            )
        doReturn(building).whenever(buildingService).getBuildingByName("Building A")

        mockMvc
            .perform(get("/api/v1/building/Building A"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("YB001"))
            .andExpect(jsonPath("$.name").value("Building A"))
    }

    @Test
    @DisplayName("건물 이름으로 조회 실패 테스트(존재하지 않는 건물)")
    @WithCustomMockUser(username = "test_user")
    fun testGetBuildingByNameNotFound() {
        doThrow(BuildingNotFoundException::class).whenever(buildingService).getBuildingByName("Nonexistent Building")

        mockMvc
            .perform(get("/api/v1/building/Nonexistent Building"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUILDING_NOT_FOUND"))
    }

    @Test
    @DisplayName("건물 이름으로 조회 실패 테스트(예외 발생)")
    @WithCustomMockUser(username = "test_user")
    fun testGetBuildingListFailure() {
        doThrow(RuntimeException("Database error")).whenever(buildingService).getBuildingByName("Building A")

        mockMvc
            .perform(get("/api/v1/building/Building A"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("건물 생성 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBuilding() {
        val newBuilding =
            CreateBuildingRequest(
                id = "YB002",
                name = "Building B",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/buildingB",
            )
        doReturn(
            Building(
                id = "YB002",
                name = "Building B",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/buildingB",
            ),
        ).whenever(buildingService).createBuilding(newBuilding)

        mockMvc
            .perform(
                post("/api/v1/building")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newBuilding)),
            ).andExpect(status().isCreated)
            .andExpect(content().json(objectMapper.writeValueAsString(newBuilding)))
    }

    @Test
    @DisplayName("건물 생성 실패 테스트(중복된 건물 이름)")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBuildingFailureDuplicateName() {
        val newBuilding =
            CreateBuildingRequest(
                id = "YB002",
                name = "Building B",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/buildingB",
            )
        doThrow(DuplicateBuildingNameException::class).whenever(buildingService).createBuilding(newBuilding)

        mockMvc
            .perform(
                post("/api/v1/building")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newBuilding)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUILDING_NAME"))
    }

    @Test
    @DisplayName("건물 생성 실패 테스트(예외 발생)")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBuildingFailure() {
        val newBuilding =
            CreateBuildingRequest(
                id = "YB002",
                name = "Building B",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/buildingB",
            )
        doThrow(RuntimeException("Database error")).whenever(buildingService).createBuilding(newBuilding)

        mockMvc
            .perform(
                post("/api/v1/building")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newBuilding)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("건물 수정 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBuilding() {
        val updatedBuilding =
            UpdateBuildingRequest(
                id = "YB001",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/updatedBuildingA",
            )
        doReturn(
            Building(
                id = "YB001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/updatedBuildingA",
            ),
        ).whenever(buildingService).updateBuilding("Building A", updatedBuilding)

        mockMvc
            .perform(
                put("/api/v1/building/Building A")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(updatedBuilding)),
            ).andExpect(status().isOk)
            .andExpect(content().json(objectMapper.writeValueAsString(updatedBuilding)))
    }

    @Test
    @DisplayName("건물 수정 실패 테스트(존재하지 않는 건물)")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBuildingNotFound() {
        val updatedBuilding =
            UpdateBuildingRequest(
                id = "YB001",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/updatedBuildingA",
            )
        doThrow(BuildingNotFoundException::class).whenever(buildingService).updateBuilding("Nonexistent Building", updatedBuilding)

        mockMvc
            .perform(
                put("/api/v1/building/Nonexistent Building")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(updatedBuilding)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUILDING_NOT_FOUND"))
    }

    @Test
    @DisplayName("건물 수정 실패 테스트(예외 발생)")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBuildingFailure() {
        val updatedBuilding =
            UpdateBuildingRequest(
                id = "YB001",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "https://example.com/updatedBuildingA",
            )
        doThrow(RuntimeException("Database error")).whenever(buildingService).updateBuilding("Building A", updatedBuilding)

        mockMvc
            .perform(
                put("/api/v1/building/Building A")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(updatedBuilding)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("건물 삭제 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBuilding() {
        whenever(buildingService.getBuildingByName("Building A"))
            .thenReturn(
                Building(
                    id = "YB001",
                    name = "Building A",
                    campusID = 1,
                    latitude = 37.5665,
                    longitude = 126.978,
                    url = "https://example.com/buildingA",
                ),
            )
        mockMvc
            .perform(delete("/api/v1/building/Building A"))
            .andExpect(status().isNoContent)
        // Verify that the building was deleted
        verify(buildingService).deleteBuildingByName("Building A")
    }

    @Test
    @DisplayName("건물 삭제 실패 테스트(존재하지 않는 건물)")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBuildingNotFound() {
        doThrow(BuildingNotFoundException::class).whenever(buildingService).deleteBuildingByName("Nonexistent Building")

        mockMvc
            .perform(delete("/api/v1/building/Nonexistent Building"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUILDING_NOT_FOUND"))
    }

    @Test
    @DisplayName("건물 삭제 실패 테스트(예외 발생)")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBuildingFailure() {
        doThrow(RuntimeException("Database error")).whenever(buildingService).deleteBuildingByName("Building A")

        mockMvc
            .perform(delete("/api/v1/building/Building A"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
