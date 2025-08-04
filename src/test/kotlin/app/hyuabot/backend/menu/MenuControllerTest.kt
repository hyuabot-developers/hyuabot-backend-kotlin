package app.hyuabot.backend.menu

import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.database.entity.Menu
import app.hyuabot.backend.database.repository.MenuRepository
import app.hyuabot.backend.menu.domain.MenuRequest
import app.hyuabot.backend.menu.exception.MenuNotFoundException
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
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuControllerTest {
    @MockitoBean
    private lateinit var menuService: MenuService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var menuRepository: MenuRepository

    @BeforeEach
    fun setUp() {
        // Clear the repository before each test
        menuRepository.deleteAll()
    }

    @Test
    @DisplayName("메뉴 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun getMenuList() {
        doReturn(
            listOf(
                Menu(
                    seq = 1,
                    restaurantID = 1,
                    date = LocalDate.now(),
                    type = "lunch",
                    food = "Spaghetti",
                    price = "$10",
                    cafeteria = null,
                ),
                Menu(
                    seq = 2,
                    restaurantID = 1,
                    date = LocalDate.now(),
                    type = "dinner",
                    food = "Pizza",
                    price = "$12",
                    cafeteria = null,
                ),
                Menu(
                    seq = 3,
                    restaurantID = 1,
                    date = LocalDate.now(),
                    type = "breakfast",
                    food = "Pancakes",
                    price = "$8",
                    cafeteria = null,
                ),
            ),
        ).whenever(menuService).getMenuList(
            cafeteriaID = 1,
            date = LocalDate.now(),
            type = null,
        )
        // With date and cafeteriaID parameters
        mockMvc
            .perform(get("/api/v1/menu"))
            .andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$[0].seq").value(1)
                jsonPath("$[0].restaurantID").value(1)
                jsonPath("$[0].date").value(LocalDate.now().toString())
                jsonPath("$[0].type").value("lunch")
                jsonPath("$[0].food").value("Spaghetti")
                jsonPath("$[0].price").value("$10")
                jsonPath("$[1].seq").value(2)
                jsonPath("$[1].restaurantID").value(1)
                jsonPath("$[1].date").value(LocalDate.now().toString())
                jsonPath("$[1].type").value("dinner")
                jsonPath("$[1].food").value("Pizza")
                jsonPath("$[1].price").value("$12")
                jsonPath("$[2].seq").value(3)
                jsonPath("$[2].restaurantID").value(1)
                jsonPath("$[2].date").value(LocalDate.now().toString())
                jsonPath("$[2].type").value("breakfast")
                jsonPath("$[2].food").value("Pancakes")
                jsonPath("$[2].price").value("$8")
            }
    }

    @Test
    @DisplayName("메뉴 목록 조회 - 날짜와 타입 필터링")
    @WithCustomMockUser(username = "test_user")
    fun getMenuListWithDateAndType() {
        val date = LocalDate.now()
        val type = "lunch"
        doReturn(
            listOf(
                Menu(
                    seq = 1,
                    restaurantID = 1,
                    date = date,
                    type = type,
                    food = "Salad",
                    price = "$5",
                    cafeteria = null,
                ),
            ),
        ).whenever(menuService).getMenuList(
            cafeteriaID = 1,
            date = date,
            type = type,
        )

        mockMvc
            .perform(
                get("/api/v1/menu")
                    .param("cafeteriaID", "1")
                    .param("date", date.toString())
                    .param("type", type),
            ).andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$[0].seq").value(1)
                jsonPath("$[0].restaurantID").value(1)
                jsonPath("$[0].date").value(date.toString())
                jsonPath("$[0].type").value(type)
                jsonPath("$[0].food").value("Salad")
                jsonPath("$[0].price").value("$5")
            }
    }

    @Test
    @DisplayName("메뉴 생성 성공")
    @WithCustomMockUser(username = "test_user")
    fun createMenu() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = LocalDate.now().toString(),
                type = "lunch",
                food = "Burger",
                price = "$6",
            )

        val createdMenu =
            Menu(
                seq = 1,
                restaurantID = menuRequest.cafeteriaID,
                date = LocalDate.parse(menuRequest.date),
                type = menuRequest.type,
                food = menuRequest.food,
                price = menuRequest.price,
                cafeteria = null,
            )
        doReturn(createdMenu).whenever(menuService).createMenu(menuRequest)
        mockMvc
            .perform(
                post("/api/v1/menu")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isCreated
                content().contentType("application/json")
                jsonPath("$.seq").value(1)
                jsonPath("$.restaurantID").value(menuRequest.cafeteriaID)
                jsonPath("$.date").value(LocalDate.parse(menuRequest.date).toString())
                jsonPath("$.type").value(menuRequest.type)
                jsonPath("$.food").value(menuRequest.food)
                jsonPath("$.price").value(menuRequest.price)
            }
    }

    @Test
    @DisplayName("메뉴 생성 실패 (식당 없음)")
    @WithCustomMockUser(username = "test_user")
    fun createMenuFail() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 999,
                date = LocalDate.now().toString(),
                type = "lunch",
                food = "Burger",
                price = "$6",
            )
        doThrow(CafeteriaNotFoundException()).whenever(menuService).createMenu(menuRequest)
        mockMvc
            .perform(
                post("/api/v1/menu")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("CAFETERIA_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("메뉴 생성 실패 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun createMenuException() {
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = LocalDate.now().toString(),
                type = "lunch",
                food = "Burger",
                price = "$6",
            )
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).createMenu(menuRequest)
        mockMvc
            .perform(
                post("/api/v1/menu")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }

    @Test
    @DisplayName("메뉴 ID로 조회")
    @WithCustomMockUser(username = "test_user")
    fun getMenuById() {
        val menuId = 1
        val menu =
            Menu(
                seq = menuId,
                restaurantID = 1,
                date = LocalDate.now(),
                type = "lunch",
                food = "Salad",
                price = "$5",
                cafeteria = null,
            )
        doReturn(menu).whenever(menuService).getMenuById(menuId)
        mockMvc
            .perform(get("/api/v1/menu/$menuId"))
            .andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$.seq").value(menuId)
                jsonPath("$.restaurantID").value(1)
                jsonPath("$.date").value(LocalDate.now().toString())
                jsonPath("$.type").value("lunch")
                jsonPath("$.food").value("Salad")
                jsonPath("$.price").value("$5")
            }
    }

    @Test
    @DisplayName("메뉴 ID로 조회 실패 (메뉴 없음)")
    @WithCustomMockUser(username = "test_user")
    fun getMenuByIdFail() {
        val menuId = 999
        doThrow(MenuNotFoundException()).whenever(menuService).getMenuById(menuId)
        mockMvc
            .perform(get("/api/v1/menu/$menuId"))
            .andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("MENU_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("메뉴 ID로 조회 실패 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun getMenuByIdException() {
        val menuId = 1
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).getMenuById(menuId)
        mockMvc
            .perform(get("/api/v1/menu/$menuId"))
            .andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }

    @Test
    @DisplayName("메뉴 수정 성공")
    @WithCustomMockUser(username = "test_user")
    fun updateMenu() {
        val menuId = 1
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = LocalDate.now().toString(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        val updatedMenu =
            Menu(
                seq = menuId,
                restaurantID = menuRequest.cafeteriaID,
                date = LocalDate.parse(menuRequest.date),
                type = menuRequest.type,
                food = menuRequest.food,
                price = menuRequest.price,
                cafeteria = null,
            )
        doReturn(updatedMenu).whenever(menuService).updateMenu(menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/menu/$menuId")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$.seq").value(menuId)
                jsonPath("$.restaurantID").value(menuRequest.cafeteriaID)
                jsonPath("$.date").value(LocalDate.parse(menuRequest.date).toString())
                jsonPath("$.type").value(menuRequest.type)
                jsonPath("$.food").value(menuRequest.food)
                jsonPath("$.price").value(menuRequest.price)
            }
    }

    @Test
    @DisplayName("메뉴 수정 실패 (메뉴 없음)")
    @WithCustomMockUser(username = "test_user")
    fun updateMenuByIdFail() {
        val menuId = 999
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = LocalDate.now().toString(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        doThrow(MenuNotFoundException()).whenever(menuService).updateMenu(menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/menu/$menuId")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("MENU_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("메뉴 수정 실패 (식당 없음)")
    @WithCustomMockUser(username = "test_user")
    fun updateMenuFail() {
        val menuId = 999
        val menuRequest =
            MenuRequest(
                cafeteriaID = 999,
                date = LocalDate.now().toString(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        doThrow(CafeteriaNotFoundException()).whenever(menuService).updateMenu(menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/menu/$menuId")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isBadRequest
                content().contentType("application/json")
                jsonPath("$.message").value("CAFETERIA_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("메뉴 수정 실패 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun updateMenuException() {
        val menuId = 1
        val menuRequest =
            MenuRequest(
                cafeteriaID = 1,
                date = LocalDate.now().toString(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).updateMenu(menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/menu/$menuId")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }

    @Test
    @DisplayName("메뉴 삭제 성공")
    @WithCustomMockUser(username = "test_user")
    fun deleteMenu() {
        val menuId = 1
        mockMvc
            .perform(delete("/api/v1/menu/$menuId"))
            .andExpect {
                status().isNoContent
            }
        verify(menuService).deleteMenuById(menuId)
    }

    @Test
    @DisplayName("메뉴 삭제 실패 (메뉴 없음)")
    @WithCustomMockUser(username = "test_user")
    fun deleteMenuFail() {
        val menuId = 999
        doThrow(MenuNotFoundException()).whenever(menuService).deleteMenuById(menuId)
        mockMvc
            .perform(delete("/api/v1/menu/$menuId"))
            .andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("MENU_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("메뉴 삭제 실패 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun deleteMenuException() {
        val menuId = 1
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).deleteMenuById(menuId)
        mockMvc
            .perform(delete("/api/v1/menu/$menuId"))
            .andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }
}
