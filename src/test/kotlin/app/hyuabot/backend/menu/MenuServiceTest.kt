package app.hyuabot.backend.menu

import app.hyuabot.backend.cafeteria.MenuService
import app.hyuabot.backend.cafeteria.domain.MenuRequest
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.cafeteria.exception.MenuNotFoundException
import app.hyuabot.backend.database.entity.Menu
import app.hyuabot.backend.database.repository.CafeteriaRepository
import app.hyuabot.backend.database.repository.MenuRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MenuServiceTest {
    @Mock
    lateinit var menuRepository: MenuRepository

    @Mock
    lateinit var cafeteriaRepository: CafeteriaRepository

    @InjectMocks
    lateinit var menuService: MenuService

    @Test
    @DisplayName("학식 식당 목록 조회 테스트")
    fun getMenuListTest() {
        val cafeteriaIds = listOf(1, 2, 3)
        val dates =
            listOf(
                LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 8, 11),
                LocalDate.of(2025, 8, 12),
            )
        val types = listOf("조식", "중식", "석식")
        val menus: List<Menu> =
            cafeteriaIds.flatMap { cafeteriaId ->
                dates.flatMap { date ->
                    types.map { type ->
                        Menu(
                            seq = 0,
                            restaurantID = cafeteriaId,
                            date = date,
                            type = type,
                            food = "Sample Menu",
                            price = "Sample Price",
                            cafeteria = null,
                        )
                    }
                }
            }
        // Mocking the repository call
        whenever(menuRepository.findAll()).thenReturn(menus)
        whenever(menuRepository.findByDate(LocalDate.of(2025, 8, 10))).thenReturn(
            menus.filter { it.date == LocalDate.of(2025, 8, 10) },
        )
        whenever(menuRepository.findByRestaurantID(1)).thenReturn(
            menus.filter { it.restaurantID == 1 },
        )

        whenever(menuRepository.findByType("조식")).thenReturn(
            menus.filter { it.type == "조식" },
        )
        whenever(
            menuRepository.findByDateAndType(
                LocalDate.of(2025, 8, 10),
                "조식",
            ),
        ).thenReturn(
            menus.filter { it.date == LocalDate.of(2025, 8, 10) && it.type == "조식" },
        )
        whenever(
            menuRepository.findByRestaurantIDAndType(
                1,
                "조식",
            ),
        ).thenReturn(
            menus.filter { it.restaurantID == 1 && it.type == "조식" },
        )
        whenever(
            menuRepository.findByRestaurantIDAndDate(
                1,
                LocalDate.of(2025, 8, 10),
            ),
        ).thenReturn(
            menus.filter { it.restaurantID == 1 && it.date == LocalDate.of(2025, 8, 10) },
        )
        whenever(
            menuRepository.findByRestaurantIDAndDateAndType(
                1,
                LocalDate.of(2025, 8, 10),
                "조식",
            ),
        ).thenReturn(
            menus.filter { it.restaurantID == 1 && it.date == LocalDate.of(2025, 8, 10) && it.type == "조식" },
        )
        // Call the service method
        menuService.getMenuList(null, null, null).let {
            assertEquals(27, it.size)
            assertEquals(menus, it)
        }
        menuService.getMenuList(1, null, null).let {
            assertEquals(9, it.size)
            assertEquals(
                menus.filter { menu -> menu.restaurantID == 1 },
                it,
            )
        }
        menuService.getMenuList(null, LocalDate.of(2025, 8, 10), null).let {
            assertEquals(9, it.size)
            assertEquals(
                menus.filter { menu -> menu.date == LocalDate.of(2025, 8, 10) },
                it,
            )
        }
        menuService.getMenuList(null, null, "조식").let {
            assertEquals(9, it.size)
            assertEquals(
                menus.filter { menu -> menu.type == "조식" },
                it,
            )
        }
        menuService.getMenuList(1, LocalDate.of(2025, 8, 10), null).let {
            assertEquals(3, it.size)
            assertEquals(
                menus.filter { menu -> menu.restaurantID == 1 && menu.date == LocalDate.of(2025, 8, 10) },
                it,
            )
        }
        menuService.getMenuList(1, null, "조식").let {
            assertEquals(3, it.size)
            assertEquals(
                menus.filter { menu -> menu.restaurantID == 1 && menu.type == "조식" },
                it,
            )
        }
        menuService.getMenuList(null, LocalDate.of(2025, 8, 10), "조식").let {
            assertEquals(3, it.size)
            assertEquals(
                menus.filter { menu -> menu.date == LocalDate.of(2025, 8, 10) && menu.type == "조식" },
                it,
            )
        }
        menuService.getMenuList(1, LocalDate.of(2025, 8, 10), "조식").let {
            assertEquals(1, it.size)
            assertEquals(
                menus.filter { menu ->
                    menu.restaurantID == 1 && menu.date == LocalDate.of(2025, 8, 10) && menu.type == "조식"
                },
                it,
            )
        }
    }

    @Test
    @DisplayName("학식 메뉴 ID로 조회 테스트")
    fun getMenuByIdTest() {
        val menu =
            Menu(
                seq = 1,
                restaurantID = 1,
                date = LocalDate.of(2025, 8, 10),
                type = "조식",
                food = "Sample Menu",
                price = "Sample Price",
                cafeteria = null,
            )
        // Mocking the repository call
        whenever(menuRepository.findById(1)).thenReturn(Optional.of(menu))

        // Call the service method
        val foundMenu = menuService.getMenuById(1)
        assertEquals(menu, foundMenu)
    }

    @Test
    @DisplayName("학식 메뉴 ID로 조회 실패 테스트")
    fun getMenuByIdNotFoundTest() {
        // Mocking the repository call to return empty
        whenever(menuRepository.findById(1)).thenReturn(Optional.empty())

        // Call the service method and expect an exception
        assertThrows<MenuNotFoundException> { menuService.getMenuById(1) }
    }

    @Test
    @DisplayName("학식 메뉴 생성 테스트")
    fun createMenuTest() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = "2025-08-10",
                type = "조식",
                food = "Sample Menu",
                price = "Sample Price",
            )
        val menu =
            Menu(
                seq = 1,
                restaurantID = 1,
                date = LocalDate.of(2025, 8, 10),
                type = "조식",
                food = "Sample Menu",
                price = "Sample Price",
                cafeteria = null,
            )

        // Mocking the repository call
        whenever(cafeteriaRepository.existsById(1)).thenReturn(true)
        whenever(
            menuRepository.save(
                Menu(
                    restaurantID = 1,
                    date = LocalDate.of(2025, 8, 10),
                    type = "조식",
                    food = "Sample Menu",
                    price = "Sample Price",
                    cafeteria = null,
                ),
            ),
        ).thenReturn(menu)

        // Call the service method
        val createdMenu = menuService.createMenu(menuRequest)
        assertEquals(menu, createdMenu)
    }

    @Test
    @DisplayName("학식 메뉴 생성 실패 테스트 (식당 없음)")
    fun createMenuNotFoundTest() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = "2025-08-10",
                type = "조식",
                food = "Sample Menu",
                price = "Sample Price",
            )

        // Mocking the repository call
        whenever(cafeteriaRepository.existsById(1)).thenReturn(false)

        // Call the service method and expect an exception
        assertThrows<CafeteriaNotFoundException> { menuService.createMenu(menuRequest) }
    }

    @Test
    @DisplayName("학식 메뉴 수정 테스트")
    fun updateMenuTest() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = "2025-08-10",
                type = "조식",
                food = "Updated Menu",
                price = "Updated Price",
            )
        val existingMenu =
            Menu(
                seq = 1,
                restaurantID = 1,
                date = LocalDate.of(2025, 8, 10),
                type = "조식",
                food = "Sample Menu",
                price = "Sample Price",
                cafeteria = null,
            )
        val updatedMenu =
            existingMenu.copy(
                food = "Updated Menu",
                price = "Updated Price",
            )

        // Mocking the repository call
        whenever(menuRepository.findById(1)).thenReturn(Optional.of(existingMenu))
        whenever(cafeteriaRepository.existsById(1)).thenReturn(true)
        whenever(
            menuRepository.save(
                Menu(
                    seq = 1,
                    restaurantID = 1,
                    date = LocalDate.of(2025, 8, 10),
                    type = "조식",
                    food = "Updated Menu",
                    price = "Updated Price",
                    cafeteria = null,
                ),
            ),
        ).thenReturn(updatedMenu)

        // Call the service method
        val result = menuService.updateMenu(1, menuRequest)
        assertEquals(updatedMenu, result)
    }

    @Test
    @DisplayName("학식 메뉴 수정 실패 테스트 (메뉴 없음)")
    fun updateMenuNotFoundTest() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = "2025-08-10",
                type = "조식",
                food = "Updated Menu",
                price = "Updated Price",
            )

        // Mocking the repository call
        whenever(menuRepository.findById(1)).thenReturn(Optional.empty())

        // Call the service method and expect an exception
        assertThrows<MenuNotFoundException> { menuService.updateMenu(1, menuRequest) }
    }

    @Test
    @DisplayName("학식 메뉴 수정 실패 테스트 (식당 없음)")
    fun updateMenuCafeteriaNotFoundTest() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = "2025-08-10",
                type = "조식",
                food = "Updated Menu",
                price = "Updated Price",
            )
        val existingMenu =
            Menu(
                seq = 1,
                restaurantID = 1,
                date = LocalDate.of(2025, 8, 10),
                type = "조식",
                food = "Sample Menu",
                price = "Sample Price",
                cafeteria = null,
            )

        // Mocking the repository call
        whenever(menuRepository.findById(1)).thenReturn(Optional.of(existingMenu))
        whenever(cafeteriaRepository.existsById(1)).thenReturn(false)

        // Call the service method and expect an exception
        assertThrows<CafeteriaNotFoundException> { menuService.updateMenu(1, menuRequest) }
    }

    @Test
    @DisplayName("학식 메뉴 삭제 테스트")
    fun deleteMenuTest() {
        whenever(menuRepository.existsById(1)).thenReturn(true)
        // Call the service method
        menuService.deleteMenuById(1)
        verify(menuRepository).deleteById(1)
    }

    @Test
    @DisplayName("학식 메뉴 삭제 실패 테스트 (메뉴 없음)")
    fun deleteMenuNotFoundTest() {
        whenever(menuRepository.existsById(1)).thenReturn(false)

        // Call the service method and expect an exception
        assertThrows<MenuNotFoundException> { menuService.deleteMenuById(1) }
    }
}
