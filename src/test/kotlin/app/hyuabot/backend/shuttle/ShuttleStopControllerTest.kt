package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleStop
import app.hyuabot.backend.security.WithCustomMockUser
import app.hyuabot.backend.shuttle.domain.CreateShuttleStopRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleStopRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleStopException
import app.hyuabot.backend.shuttle.exception.ShuttleStopNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleStopService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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
class ShuttleStopControllerTest {
    @MockitoBean
    private lateinit var shuttleStopService: ShuttleStopService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("셔틀버스 정류장 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun getAllStops() {
        doReturn(
            listOf(
                ShuttleStop(
                    name = "Stop1",
                    latitude = 37.5665,
                    longitude = 126.978,
                    route = emptyList(),
                    routeToStart = emptyList(),
                    routeToEnd = emptyList(),
                ),
                ShuttleStop(
                    name = "Stop2",
                    latitude = 37.5670,
                    longitude = 126.979,
                    route = emptyList(),
                    routeToStart = emptyList(),
                    routeToEnd = emptyList(),
                ),
            ),
        ).whenever(shuttleStopService).getAllStops()
        mockMvc
            .perform(get("/api/v1/shuttle/stop"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result[0].name").value("Stop1"))
            .andExpect(jsonPath("$.result[1].name").value("Stop2"))
            .andExpect(jsonPath("$.result.length()").value(2))
    }

    @Test
    @DisplayName("셔틀버스 정류장 생성")
    @WithCustomMockUser(username = "test_user")
    fun createShuttleStop() {
        val newStop =
            ShuttleStop(
                name = "NewStop",
                latitude = 37.5675,
                longitude = 126.980,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        val payload =
            CreateShuttleStopRequest(
                name = newStop.name,
                latitude = newStop.latitude,
                longitude = newStop.longitude,
            )
        doReturn(newStop).whenever(shuttleStopService).createStop(payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("NewStop"))
            .andExpect(jsonPath("$.latitude").value(37.5675))
            .andExpect(jsonPath("$.longitude").value(126.980))
    }

    @Test
    @DisplayName("셔틀버스 정류장 생성 (중복 정류장 이름)")
    @WithCustomMockUser(username = "test_user")
    fun createShuttleStopDuplicate() {
        val payload =
            CreateShuttleStopRequest(
                name = "ExistingStop",
                latitude = 37.5675,
                longitude = 126.980,
            )
        doThrow(DuplicateShuttleStopException::class).whenever(shuttleStopService).createStop(payload)
        mockMvc
            .perform(
                post("/api/v1/shuttle/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_STOP"))
    }

    @Test
    @DisplayName("셔틀버스 정류장 생성 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun createShuttleStopError() {
        val payload =
            CreateShuttleStopRequest(
                name = "ErrorStop",
                latitude = 37.5675,
                longitude = 126.980,
            )
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleStopService).createStop(payload)
        mockMvc
            .perform(
                post("/api/v1/shuttle/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 정류장 이름으로 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleStopByName() {
        val stopName = "TestStop"
        val stop =
            ShuttleStop(
                name = stopName,
                latitude = 37.5675,
                longitude = 126.980,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        doReturn(stop).whenever(shuttleStopService).getStopByName(stopName)

        mockMvc
            .perform(get("/api/v1/shuttle/stop/$stopName"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value(stopName))
            .andExpect(jsonPath("$.latitude").value(37.5675))
            .andExpect(jsonPath("$.longitude").value(126.980))
    }

    @Test
    @DisplayName("셔틀버스 정류장 이름으로 조회 (정류장 없음)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleStopByNameNotFound() {
        val stopName = "NonExistentStop"
        doThrow(ShuttleStopNotFoundException::class)
            .whenever(shuttleStopService)
            .getStopByName(stopName)

        mockMvc
            .perform(get("/api/v1/shuttle/stop/$stopName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 정류장 이름으로 조회 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleStopByNameError() {
        val stopName = "ErrorStop"
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleStopService).getStopByName(stopName)

        mockMvc
            .perform(get("/api/v1/shuttle/stop/$stopName"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 정류장 수정")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleStop() {
        val stopName = "UpdateStop"
        val updatedStop =
            ShuttleStop(
                name = stopName,
                latitude = 37.5680,
                longitude = 126.981,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        val payload =
            UpdateShuttleStopRequest(
                latitude = updatedStop.latitude,
                longitude = updatedStop.longitude,
            )
        doReturn(updatedStop).whenever(shuttleStopService).updateStop(stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/stop/$stopName")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedStop)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value(stopName))
            .andExpect(jsonPath("$.latitude").value(37.5680))
            .andExpect(jsonPath("$.longitude").value(126.981))
    }

    @Test
    @DisplayName("셔틀버스 정류장 수정 (정류장 없음)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleStopNotFound() {
        val stopName = "NonExistentStop"
        val payload =
            UpdateShuttleStopRequest(
                latitude = 37.5680,
                longitude = 126.981,
            )
        doThrow(ShuttleStopNotFoundException::class).whenever(shuttleStopService).updateStop(stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/stop/$stopName")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 정류장 수정 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleStopError() {
        val stopName = "ErrorStop"
        val payload =
            UpdateShuttleStopRequest(
                latitude = 37.5680,
                longitude = 126.981,
            )
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleStopService).updateStop(stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/stop/$stopName")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 정류장 삭제")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleStop() {
        val stopName = "DeleteStop"
        doNothing().whenever(shuttleStopService).deleteStopByName(stopName)
        mockMvc
            .perform(delete("/api/v1/shuttle/stop/$stopName"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("셔틀버스 정류장 삭제 (정류장 없음)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleStopNotFound() {
        val stopName = "NonExistentStop"
        doThrow(ShuttleStopNotFoundException::class).whenever(shuttleStopService).deleteStopByName(stopName)
        mockMvc
            .perform(delete("/api/v1/shuttle/stop/$stopName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 정류장 삭제 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleStopError() {
        val stopName = "ErrorStop"
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleStopService).deleteStopByName(stopName)
        mockMvc
            .perform(delete("/api/v1/shuttle/stop/$stopName"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
