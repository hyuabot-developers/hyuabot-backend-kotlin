package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleTimetableView
import app.hyuabot.backend.security.WithCustomMockUser
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShuttleTimetableControllerTest {
    @MockitoBean
    private lateinit var shuttleTimetableService: ShuttleTimetableService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("셔틀버스 시간표 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleTimetable() {
        doReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    periodType = "semester",
                    weekday = true,
                    routeName = "A",
                    routeTag = "A",
                    stopName = "Stop1",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "Group1",
                ),
                ShuttleTimetableView(
                    seq = 2,
                    periodType = "semester",
                    weekday = true,
                    routeName = "B",
                    routeTag = "B",
                    stopName = "Stop2",
                    departureTime = LocalTime.parse("10:00:00"),
                    destinationGroup = "Group2",
                ),
            ),
        ).whenever(shuttleTimetableService).getShuttleTimetable()

        mockMvc
            .perform(get("/api/v1/shuttle/timetable"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].period").value("semester"))
            .andExpect(jsonPath("$.result[0].isWeekdays").value(true))
            .andExpect(jsonPath("$.result[0].routeName").value("A"))
            .andExpect(jsonPath("$.result[0].routeTag").value("A"))
            .andExpect(jsonPath("$.result[0].stopName").value("Stop1"))
            .andExpect(jsonPath("$.result[0].departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.result[0].destinationGroup").value("Group1"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].period").value("semester"))
            .andExpect(jsonPath("$.result[1].isWeekdays").value(true))
            .andExpect(jsonPath("$.result[1].routeName").value("B"))
            .andExpect(jsonPath("$.result[1].routeTag").value("B"))
            .andExpect(jsonPath("$.result[1].stopName").value("Stop2"))
            .andExpect(jsonPath("$.result[1].departureTime").value("10:00:00"))
            .andExpect(jsonPath("$.result[1].destinationGroup").value("Group2"))
    }
}
