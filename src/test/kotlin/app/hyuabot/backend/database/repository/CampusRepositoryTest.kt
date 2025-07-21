package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Campus
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
class CampusRepositoryTest {
    @Autowired lateinit var campusRepository: CampusRepository
    private val campus = Campus(name = "Main Campus")

    @BeforeEach
    fun setUp() {
        campusRepository.save(campus)
    }

    @AfterEach
    fun tearDown() {
        campusRepository.deleteAll()
    }

    @Test
    @DisplayName("캠퍼스 목록 키워드 검색")
    fun testFindByNameContaining() {
        val campus = campusRepository.findByNameContaining("Main Campus")
        assert(campus.isNotEmpty())
        campus.first().let {
            assert(it.name.contains("Main Campus"))
            assert(it.id == this.campus.id)
            assert(it.cafeteria.isEmpty())
            assert(it.building.isEmpty())
            assert(it.readingRoom.isEmpty())
        }
        val emptyCampus = campusRepository.findByNameContaining("Nonexistent Campus")
        assert(emptyCampus.isEmpty())
    }
}
