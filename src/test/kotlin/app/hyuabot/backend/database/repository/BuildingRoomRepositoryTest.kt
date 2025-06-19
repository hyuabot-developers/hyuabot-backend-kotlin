package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Building
import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.entity.Room
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class BuildingRoomRepositoryTest {
    @Autowired lateinit var campusRepository: CampusRepository

    @Autowired lateinit var buildingRepository: BuildingRepository

    @Autowired lateinit var roomRepository: RoomRepository
    private val campus =
        Campus(
            id = 1,
            name = "Main Campus",
        )
    private lateinit var buildingList: MutableList<Building>
    private lateinit var roomList: MutableList<Room>

    @BeforeEach
    fun setUp() {
        buildingList =
            mutableListOf(
                Building(
                    id = "A",
                    name = "Building A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    url = "http://example.com/building_a.jpg",
                    campus = campus,
                    campusID = campus.id,
                ),
                Building(
                    id = "B",
                    name = "Building B",
                    latitude = 37.7750,
                    longitude = -122.4195,
                    url = "http://example.com/building_b.jpg",
                    campus = campus,
                    campusID = campus.id,
                ),
                Building(
                    id = "C",
                    name = "Building C",
                    latitude = 37.7751,
                    longitude = -122.4196,
                    url = "http://example.com/building_c.jpg",
                    campus = campus,
                    campusID = campus.id,
                ),
            )
        roomList =
            mutableListOf(
                Room(
                    number = "101",
                    name = "Room 101",
                    building = buildingList[0],
                    buildingName = buildingList[0].name,
                ),
                Room(
                    number = "102",
                    name = "Room 102",
                    building = buildingList[0],
                    buildingName = buildingList[0].name,
                ),
            )
        campusRepository.save(campus)
        buildingRepository.saveAll(buildingList)
        roomRepository.saveAll(roomList)
    }

    @AfterEach
    fun tearDown() {
        roomRepository.deleteAll()
        buildingRepository.deleteAll()
        campusRepository.deleteAll()
    }

    @Test
    @DisplayName("건물 항목 조회")
    fun testFindByName() {
        val building = buildingRepository.findByName("Building A")
        assert(building != null)
        assert(building?.id == "A")
        assert(building?.name == "Building A")
        assert(building?.latitude == 37.7749)
        assert(building?.longitude == -122.4194)
        assert(building?.url == "http://example.com/building_a.jpg")
        assert(building?.campusID == 1)
        assert(building?.campus?.id == 1)
        assert(building?.campus?.name == "Main Campus")
        assert(building?.room?.isEmpty() == true)
    }

    @Test
    @DisplayName("건물 항목 삭제")
    fun testDeleteBuildingByName() {
        val deletedCount = buildingRepository.deleteBuildingByName("Building B")
        assert(deletedCount == 1)
    }

    @Test
    @DisplayName("건물 목록 키워드 검색")
    fun testFindByNameContaining() {
        val buildings = buildingRepository.findByNameContaining("Building")
        assert(buildings.size == 3)
        assert(buildings.any { it.name == "Building A" })
        assert(buildings.any { it.name == "Building B" })
        assert(buildings.any { it.name == "Building C" })
    }

    @Test
    @DisplayName("건물 목록 키워드 검색 및 캠퍼스 ID 필터링")
    fun testFindByCampusIDAndNameContaining() {
        val buildings = buildingRepository.findByCampusIDAndNameContaining(1, "Building")
        assert(buildings.size == 3)
        assert(buildings.any { it.name == "Building A" })
        assert(buildings.any { it.name == "Building B" })
        assert(buildings.any { it.name == "Building C" })
    }

    @Test
    @DisplayName("건물 위도/경도 범위 검색")
    fun testFindByLatitudeBetweenAndLongitudeBetween() {
        val buildings =
            buildingRepository.findByLatitudeBetweenAndLongitudeBetween(
                south = 37.7740,
                north = 37.7760,
                west = -122.4200,
                east = -122.4180,
            )
        assert(buildings.size == 3)
        assert(buildings.any { it.name == "Building A" })
        assert(buildings.any { it.name == "Building B" })
        assert(buildings.any { it.name == "Building C" })
    }

    @Test
    @DisplayName("강의실 항목 조회")
    fun testFindByBuildingNameAndNumber() {
        val room = roomRepository.findByBuildingNameAndNumber("Building A", "101")
        assert(room != null)
        assert(room?.buildingName == "Building A")
        assert(room?.number == "101")
        assert(room?.name == "Room 101")
        assert(room?.building?.id == "A")
    }

    @Test
    @DisplayName("강의실 항목 삭제")
    fun testDeleteRoomByBuildingNameAndNumber() {
        val deletedCount = roomRepository.deleteByBuildingNameAndNumber("Building A", "101")
        assert(deletedCount == 1)
    }

    @Test
    @DisplayName("강의실 목록 키워드 검색")
    fun testFindRoomByNameContaining() {
        val rooms = roomRepository.findByNameContaining("Room")
        assert(rooms.size == 2)
        assert(rooms.any { it.name == "Room 101" })
        assert(rooms.any { it.name == "Room 102" })
    }
}
