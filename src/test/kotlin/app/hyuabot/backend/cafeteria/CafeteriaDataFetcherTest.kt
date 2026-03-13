package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.database.entity.Cafeteria
import app.hyuabot.backend.database.entity.Menu
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

@EnableDgsTest
@SpringJUnitConfig
@Import(CafeteriaDataFetcher::class, ScalarRegistration::class)
class CafeteriaDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var cafeteriaService: CafeteriaService

    @MockitoBean lateinit var menuService: MenuService

    private val today = LocalDate.now()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun createCafeteria(
        seq: Int = 1,
        campusID: Int = 1,
        name: String = "test",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        breakfastTime: String? = null,
        lunchTime: String? = null,
        dinnerTime: String? = null,
    ) = Cafeteria(
        id = seq,
        campusID = campusID,
        name = name,
        latitude = latitude,
        longitude = longitude,
        breakfastTime = breakfastTime,
        lunchTime = lunchTime,
        dinnerTime = dinnerTime,
        campus = null,
    )

    private fun createMenu(
        seq: Int = 1,
        cafeteriaID: Int = 1,
        date: LocalDate = today,
        type: String = "test",
        food: String = "test menu",
        price: String = "test price",
    ): Menu =
        Menu(
            seq = seq,
            restaurantID = cafeteriaID,
            date = date,
            type = type,
            food = food,
            price = price,
            cafeteria = null,
        )

    @Test
    @DisplayName("학식 식당 및 메뉴 조회 테스트")
    fun testCommuteShuttle() {
        whenever(cafeteriaService.getCafeteriaList(campusID = 2)).thenReturn(
            listOf(
                createCafeteria(
                    seq = 1,
                    campusID = 2,
                    name = "test1",
                ),
                createCafeteria(
                    seq = 2,
                    campusID = 2,
                    name = "test2",
                ),
            ),
        )
        whenever(
            menuService.getMenuList(
                cafeteriaID = 1,
                date = today,
                type = null,
            ),
        ).thenReturn(
            listOf(
                createMenu(seq = 1, cafeteriaID = 1, date = today),
                createMenu(seq = 2, cafeteriaID = 1, date = today),
            ),
        )
        whenever(
            menuService.getMenuList(
                cafeteriaID = 2,
                date = today,
                type = null,
            ),
        ).thenReturn(
            listOf(
                createMenu(seq = 3, cafeteriaID = 2, date = today),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    cafeteria (input: { campus: 2, date: "${dateFormatter.format(today)}" }) {
                        seq
                        name
                        latitude
                        longitude
                        campus
                        runningTime {
                            breakfast                        
                            lunch
                            dinner
                        }
                        menus {
                            seq                        
                            type
                            food
                            price
                        }
                    }
                }
                """.trimIndent(),
                "data.cafeteria",
            )
        assertEquals(2, result.size)
        val firstCafeteria = result.elementAt(0)
        assertEquals(1, firstCafeteria["seq"])
        assertEquals(2, firstCafeteria["campus"])
        assertEquals(0.0, firstCafeteria["latitude"])
        assertEquals(0.0, firstCafeteria["longitude"])
        val firstCafeteriaMenu = firstCafeteria["menus"] as List<*>
        assertEquals(2, firstCafeteriaMenu.size)
        val firstMenu = firstCafeteriaMenu.elementAt(0) as Map<*, *>
        assertEquals(1, firstMenu["seq"])
        assertEquals("test", firstMenu["type"])
        assertEquals("test menu", firstMenu["food"])
        assertEquals("test price", firstMenu["price"])
    }
}
