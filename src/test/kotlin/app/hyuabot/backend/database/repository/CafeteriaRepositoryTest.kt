package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Cafeteria
import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.entity.Menu
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class CafeteriaRepositoryTest {
    @Autowired lateinit var campusRepository: CampusRepository

    @Autowired lateinit var cafeteriaRepository: CafeteriaRepository

    @Autowired lateinit var menuRepository: MenuRepository

    private val campus =
        Campus(
            id = 1,
            name = "Main Campus",
        )
    private lateinit var cafeteriaList: MutableList<Cafeteria>
    private lateinit var menuList: MutableList<Menu>

    @BeforeEach
    fun setUp() {
        cafeteriaList =
            mutableListOf(
                Cafeteria(
                    id = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                    campus = campus,
                    campusID = campus.id,
                ),
                Cafeteria(
                    id = 2,
                    name = "Cafeteria B",
                    latitude = 37.7750,
                    longitude = -122.4195,
                    breakfastTime = "07:30-10:30",
                    lunchTime = "11:30-14:30",
                    dinnerTime = "17:30-20:30",
                    campus = campus,
                    campusID = campus.id,
                ),
            )
        menuList =
            mutableListOf(
                Menu(
                    restaurantID = 1,
                    date = LocalDate.now(),
                    type = "중식",
                    food = "Menu A",
                    price = "5000",
                    cafeteria = cafeteriaList[0],
                ),
                Menu(
                    restaurantID = 1,
                    date = LocalDate.now(),
                    type = "석식",
                    food = "Menu B",
                    price = "6000",
                    cafeteria = cafeteriaList[0],
                ),
            )
        campusRepository.save(campus)
        cafeteriaRepository.saveAllAndFlush(cafeteriaList)
        menuRepository.saveAll(menuList)
    }

    @AfterEach
    fun tearDown() {
        campusRepository.deleteAll()
    }

    @Test
    @DisplayName("식당 목록 키워드 검색")
    fun testFindByName() {
        val foundCafeteria = cafeteriaRepository.findByNameContaining("A")
        assert(foundCafeteria.isNotEmpty())
        assert(foundCafeteria.size == 1)
        foundCafeteria.first().let {
            assert(it.id == 1)
            assert(it.campusID == 1)
            assert(it.name == "Cafeteria A")
            assert(it.latitude == 37.7749)
            assert(it.longitude == -122.4194)
            assert(it.breakfastTime == "07:00-10:00")
            assert(it.lunchTime == "11:00-14:00")
            assert(it.dinnerTime == "17:00-20:00")
            assert(it.campus.id == campus.id)
            assert(it.campus.name == campus.name)
            assert(it.menu.isEmpty())
        }
    }

    @Test
    @DisplayName("식당 목록 캠퍼스 ID로 검색")
    fun testFindByCampusID() {
        val foundCafeterias = cafeteriaRepository.findByCampusID(campus.id)
        assert(foundCafeterias.isNotEmpty())
        assert(foundCafeterias.size == 2)
        foundCafeterias.forEach {
            assert(it.campus.id == campus.id)
            assert(it.campus.name == campus.name)
        }
    }

    @Test
    @DisplayName("식당 및 날짜별 메뉴 조회")
    fun testFindMenuByRestaurantIDAndDate() {
        val foundMenus = menuRepository.findByRestaurantIDAndDate(1, LocalDate.now())
        assert(foundMenus.isNotEmpty())
        assert(foundMenus.size == 2)
        foundMenus.forEach {
            assert(it.restaurantID == 1)
            assert(it.cafeteria.id == 1)
            assert(it.date == LocalDate.now())
        }
    }

    @Test
    @DisplayName("식당, 날짜, 타입별 메뉴 조회")
    fun testFindMenuByRestaurantIDAndDateAndType() {
        val foundMenus = menuRepository.findByRestaurantIDAndDateAndType(1, LocalDate.now(), "중식")
        assert(foundMenus.isNotEmpty())
        assert(foundMenus.size == 1)
        foundMenus.first().let {
            assert(it.seq != null)
            assert(it.food == "Menu A")
            assert(it.price == "5000")
            assert(it.restaurantID == 1)
            assert(it.date == LocalDate.now())
            assert(it.type == "중식")
        }
    }
}
