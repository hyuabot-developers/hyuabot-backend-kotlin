package app.hyuabot.backend.room

import app.hyuabot.backend.building.RoomService
import app.hyuabot.backend.building.domain.RoomRequest
import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.building.exception.DuplicateRoomException
import app.hyuabot.backend.building.exception.RoomNotFoundException
import app.hyuabot.backend.database.entity.Room
import app.hyuabot.backend.database.repository.RoomRepository
import app.hyuabot.backend.security.WithCustomMockUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
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
class RoomControllerTest {
    @MockitoBean
    private lateinit var roomService: RoomService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var roomRepository: RoomRepository

    @BeforeEach
    fun setup() {
        roomRepository.deleteAll()
    }

    @Test
    @DisplayName("강의실 목록 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun getRoomListTest() {
        doReturn(
            listOf(
                Room(seq = 1, buildingName = "Building A", number = "101", name = "Room 101"),
                Room(seq = 2, buildingName = "Building B", number = "202", name = "Room 202"),
            ),
        ).whenever(roomService).getRoomList(null, null)
        mockMvc
            .perform(get("/api/v1/room"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].buildingName").value("Building A"))
            .andExpect(jsonPath("$.result[0].number").value("101"))
            .andExpect(jsonPath("$.result[0].name").value("Room 101"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].buildingName").value("Building B"))
            .andExpect(jsonPath("$.result[1].number").value("202"))
            .andExpect(jsonPath("$.result[1].name").value("Room 202"))
    }

    @Test
    @DisplayName("강의실 생성 테스트")
    @WithCustomMockUser(username = "test_user")
    fun createRoomTest() {
        val roomRequest = RoomRequest(buildingName = "Building A", number = "303", name = "Room 303")
        val room = Room(seq = 3, buildingName = "Building A", number = "303", name = "Room 303")

        doReturn(room).whenever(roomService).createRoom(roomRequest)

        mockMvc
            .perform(
                post("/api/v1/room")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.buildingName").value("Building A"))
            .andExpect(jsonPath("$.number").value("303"))
            .andExpect(jsonPath("$.name").value("Room 303"))
    }

    @Test
    @DisplayName("강의실 생성 실패 테스트 (중복된 강의실 번호)")
    @WithCustomMockUser(username = "test_user")
    fun createRoomDuplicateTest() {
        val roomRequest = RoomRequest(buildingName = "Building A", number = "101", name = "Room 101")
        whenever(roomService.createRoom(roomRequest)).thenThrow(DuplicateRoomException())

        mockMvc
            .perform(
                post("/api/v1/room")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUILDING_NAME_AND_NUMBER"))
    }

    @Test
    @DisplayName("강의실 생성 실패 테스트 (존재하지 않는 건물 이름)")
    @WithCustomMockUser(username = "test_user")
    fun createRoomWithoutBuildingNameTest() {
        val roomRequest = RoomRequest(buildingName = "", number = "404", name = "Room 404")
        whenever(roomService.createRoom(roomRequest)).thenThrow(BuildingNotFoundException())

        mockMvc
            .perform(
                post("/api/v1/room")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("BUILDING_NOT_FOUND"))
    }

    @Test
    @DisplayName("강의실 생성 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun createRoomOtherErrorTest() {
        val roomRequest = RoomRequest(buildingName = "Building A", number = "505", name = "Room 505")
        whenever(roomService.createRoom(roomRequest)).thenThrow(RuntimeException("Unexpected error"))

        mockMvc
            .perform(
                post("/api/v1/room")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("강의실 ID로 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun getRoomByIDTest() {
        val room = Room(seq = 1, buildingName = "Building A", number = "101", name = "Room 101")
        doReturn(room).whenever(roomService).getRoomByID(1)

        mockMvc
            .perform(get("/api/v1/room/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.buildingName").value("Building A"))
            .andExpect(jsonPath("$.number").value("101"))
            .andExpect(jsonPath("$.name").value("Room 101"))
    }

    @Test
    @DisplayName("강의실 ID로 조회 실패 테스트 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun getRoomByIDNotFoundTest() {
        whenever(roomService.getRoomByID(999)).thenThrow(RoomNotFoundException())

        mockMvc
            .perform(get("/api/v1/room/999"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("ROOM_NOT_FOUND"))
    }

    @Test
    @DisplayName("강의실 ID로 조회 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getRoomByIDOtherErrorTest() {
        whenever(roomService.getRoomByID(1)).thenThrow(RuntimeException("Unexpected error"))

        mockMvc
            .perform(get("/api/v1/room/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("강의실 수정 테스트")
    @WithCustomMockUser(username = "test_user")
    fun updateRoomTest() {
        val roomRequest = RoomRequest(buildingName = "Building A", number = "101", name = "Updated Room 101")
        val updatedRoom = Room(seq = 1, buildingName = "Building A", number = "101", name = "Updated Room 101")

        doReturn(updatedRoom).whenever(roomService).updateRoom(1, roomRequest)

        mockMvc
            .perform(
                put("/api/v1/room/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.buildingName").value("Building A"))
            .andExpect(jsonPath("$.number").value("101"))
            .andExpect(jsonPath("$.name").value("Updated Room 101"))
    }

    @Test
    @DisplayName("강의실 수정 실패 테스트 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun updateRoomNotFoundTest() {
        val roomRequest = RoomRequest(buildingName = "Building A", number = "101", name = "Updated Room 101")
        whenever(roomService.updateRoom(999, roomRequest)).thenThrow(RoomNotFoundException())

        mockMvc
            .perform(
                put("/api/v1/room/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("ROOM_NOT_FOUND"))
    }

    @Test
    @DisplayName("강의실 수정 실패 테스트 (존재하지 않는 건물 이름)")
    @WithCustomMockUser(username = "test_user")
    fun updateRoomWithoutBuildingNameTest() {
        val roomRequest = RoomRequest(buildingName = "", number = "101", name = "Updated Room 101")
        whenever(roomService.updateRoom(1, roomRequest)).thenThrow(BuildingNotFoundException())

        mockMvc
            .perform(
                put("/api/v1/room/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("BUILDING_NOT_FOUND"))
    }

    @Test
    @DisplayName("강의실 수정 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun updateRoomOtherErrorTest() {
        val roomRequest = RoomRequest(buildingName = "Building A", number = "101", name = "Updated Room 101")
        whenever(roomService.updateRoom(1, roomRequest)).thenThrow(RuntimeException("Unexpected error"))

        mockMvc
            .perform(
                put("/api/v1/room/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roomRequest)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("강의실 삭제 테스트")
    @WithCustomMockUser(username = "test_user")
    fun deleteRoomTest() {
        val room = Room(seq = 1, buildingName = "Building A", number = "101", name = "Room 101")
        doReturn(room).whenever(roomService).getRoomByID(1)
        mockMvc
            .perform(delete("/api/v1/room/1"))
            .andExpect(status().isNoContent)
        verify(roomService).deleteRoom(1)
    }

    @Test
    @DisplayName("강의실 삭제 실패 테스트 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun deleteRoomNotFoundTest() {
        whenever(roomService.deleteRoom(999)).thenThrow(RoomNotFoundException())
        mockMvc
            .perform(delete("/api/v1/room/999"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("ROOM_NOT_FOUND"))
    }

    @Test
    @DisplayName("강의실 삭제 실패 테스트 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun deleteRoomOtherErrorTest() {
        whenever(roomService.deleteRoom(1)).thenThrow(RuntimeException("Unexpected error"))
        mockMvc
            .perform(delete("/api/v1/room/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
