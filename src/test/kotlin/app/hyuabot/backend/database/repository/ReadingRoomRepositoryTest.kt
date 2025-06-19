package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.entity.ReadingRoom
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.ZonedDateTime

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class ReadingRoomRepositoryTest {
    @Autowired lateinit var campusRepository: CampusRepository

    @Autowired lateinit var readingRoomRepository: ReadingRoomRepository

    private val currentTime = ZonedDateTime.now()
    private val campus =
        Campus(
            id = 1,
            name = "Main Campus",
        )

    private val rooms =
        (1..10).map {
            ReadingRoom(
                id = it,
                name = "Reading Room $it",
                campusID = campus.id,
                isActive = true,
                isReservable = true,
                total = 100,
                active = 100,
                occupied = 0,
                available = null,
                updatedAt = currentTime,
                campus = campus,
            )
        }

    @BeforeEach
    fun setUp() {
        campusRepository.save(campus)
        readingRoomRepository.saveAll(rooms)
    }

    @AfterEach
    fun tearDown() {
        readingRoomRepository.deleteAll()
        campusRepository.deleteAll()
    }

    @Test
    @DisplayName("열람실 목록 키워드 검색")
    fun testFindByNameContaining() {
        val keyword = "Reading Room"
        val foundRooms = readingRoomRepository.findByNameContaining(keyword)
        assert(foundRooms.size == 10)
        foundRooms.forEach { room ->
            assert(room.id <= 10)
            assert(room.name.contains(keyword))
            assert(room.isActive)
            assert(room.isReservable)
            assert(room.campusID == campus.id)
            assert(room.campus.id == campus.id)
            assert(room.updatedAt == currentTime)
            assert(room.isReservable)
            assert(room.isActive)
            assert(room.total == 100)
            assert(room.active == 100)
            assert(room.occupied == 0)
            assert(room.available == null || (room.available ?: 0) < 100)
        }
    }

    @Test
    @DisplayName("캠퍼스 ID로 열람실 목록 조회")
    fun testFindByCampusID() {
        val foundRooms = readingRoomRepository.findByCampusId(campus.id)
        assert(foundRooms.size == 10)
        assert(foundRooms.all { it.campusID == campus.id })
    }

    @Test
    @DisplayName("캠퍼스 ID 및 이름 키워드로 열람실 목록 조회")
    fun testFindByCampusIDAndNameContaining() {
        val keyword = "Reading Room"
        val foundRooms = readingRoomRepository.findByCampusIdAndNameContaining(campus.id, keyword)
        assert(foundRooms.size == 10)
        assert(foundRooms.all { it.name.contains(keyword) })
    }
}
