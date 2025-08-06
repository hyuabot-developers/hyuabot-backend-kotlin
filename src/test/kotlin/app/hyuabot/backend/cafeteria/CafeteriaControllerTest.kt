package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.cafeteria.domain.MenuRequest
import app.hyuabot.backend.cafeteria.domain.UpdateCafeteriaRequest
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.cafeteria.exception.DuplicateCafeteriaIDException
import app.hyuabot.backend.cafeteria.exception.MenuNotFoundException
import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.database.entity.Cafeteria
import app.hyuabot.backend.database.entity.Menu
import app.hyuabot.backend.database.repository.CafeteriaRepository
import app.hyuabot.backend.database.repository.MenuRepository
import app.hyuabot.backend.security.WithCustomMockUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
class CafeteriaControllerTest {
    @MockitoBean
    private lateinit var cafeteriaService: CafeteriaService

    @MockitoBean
    private lateinit var menuService: MenuService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var cafeteriaRepository: CafeteriaRepository

    @Autowired
    private lateinit var menuRepository: MenuRepository

    @BeforeEach
    fun setUp() {
        menuRepository.deleteAll()
        cafeteriaRepository.deleteAll()
    }

    @Test
    @DisplayName("학식 식당 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun getCafeteriaList() {
        doReturn(
            listOf(
                Cafeteria(
                    id = 1,
                    campusID = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                    campus = null,
                ),
                Cafeteria(
                    id = 2,
                    campusID = 1,
                    name = "Cafeteria B",
                    latitude = 37.7750,
                    longitude = -122.4195,
                    breakfastTime = "07:30-10:30",
                    lunchTime = "11:30-14:30",
                    dinnerTime = "17:30-20:30",
                    campus = null,
                ),
                Cafeteria(
                    id = 3,
                    campusID = 1,
                    name = "Cafeteria C",
                    latitude = 37.7760,
                    longitude = -122.4200,
                    breakfastTime = "08:00-11:00",
                    lunchTime = "12:00-15:00",
                    dinnerTime = "18:00-21:00",
                    campus = null,
                ),
            ),
        ).whenever(cafeteriaService).getCafeteriaList()
        mockMvc
            .perform(get("/api/v1/cafeteria"))
            .andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$[0].id").value(1)
                jsonPath("$[1].id").value(2)
                jsonPath("$[2].id").value(3)
                jsonPath("$[0].name").value("Cafeteria A")
                jsonPath("$[1].name").value("Cafeteria B")
                jsonPath("$[2].name").value("Cafeteria C")
            }
    }

    @Test
    @DisplayName("학식 식당 생성")
    @WithCustomMockUser(username = "test_user")
    fun createCafeteria() {
        val newCafeteria =
            Cafeteria(
                id = 4,
                campusID = 1,
                name = "Cafeteria D",
                latitude = 37.7770,
                longitude = -122.4210,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campus = null,
            )
        doReturn(newCafeteria).whenever(cafeteriaService).createCafeteria(any())
        mockMvc
            .perform(
                post("/api/v1/cafeteria")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newCafeteria)),
            ).andExpect {
                status().isCreated
                content().contentType("application/json")
                jsonPath("$.id").value(4)
                jsonPath("$.name").value("Cafeteria D")
                jsonPath("$.latitude").value(37.7770)
                jsonPath("$.longitude").value(-122.4210)
                jsonPath("$.breakfastTime").value("07:00-10:00")
                jsonPath("$.lunchTime").value("11:00-14:00")
                jsonPath("$.dinnerTime").value("17:00-20:00")
            }
    }

    @Test
    @DisplayName("학식 식당 생성 (중복된 ID)")
    @WithCustomMockUser(username = "test_user")
    fun createCafeteriaDuplicateId() {
        val newCafeteria =
            Cafeteria(
                id = 1,
                campusID = 1,
                name = "Cafeteria D",
                latitude = 37.7770,
                longitude = -122.4210,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campus = null,
            )
        doThrow(DuplicateCafeteriaIDException::class).whenever(cafeteriaService).createCafeteria(any())
        mockMvc
            .perform(
                post("/api/v1/cafeteria")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newCafeteria)),
            ).andExpect {
                status().isConflict
                content().contentType("application/json")
                jsonPath("$.message").value("DUPLICATE_CAFETERIA_ID")
            }
    }

    @Test
    @DisplayName("학식 식당 생성 (캠퍼스 ID가 존재하지 않음)")
    @WithCustomMockUser(username = "test_user")
    fun createCafeteriaCampusNotFound() {
        val newCafeteria =
            Cafeteria(
                id = 4,
                campusID = 999,
                name = "Cafeteria D",
                latitude = 37.7770,
                longitude = -122.4210,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campus = null,
            )
        doThrow(CampusNotFoundException::class).whenever(cafeteriaService).createCafeteria(any())
        mockMvc
            .perform(
                post("/api/v1/cafeteria")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newCafeteria)),
            ).andExpect {
                status().isBadRequest
                content().contentType("application/json")
                jsonPath("$.message").value("CAMPUS_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("학식 식당 생성 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun createCafeteriaException() {
        val newCafeteria =
            Cafeteria(
                id = 4,
                campusID = 1,
                name = "Cafeteria D",
                latitude = 37.7770,
                longitude = -122.4210,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campus = null,
            )
        doThrow(RuntimeException("Unexpected error")).whenever(cafeteriaService).createCafeteria(any())
        mockMvc
            .perform(
                post("/api/v1/cafeteria")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newCafeteria)),
            ).andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun getCafeteriaById() {
        val cafeteria =
            Cafeteria(
                id = 1,
                campusID = 1,
                name = "Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campus = null,
            )
        doReturn(cafeteria).whenever(cafeteriaService).getCafeteriaById(1)
        mockMvc
            .perform(get("/api/v1/cafeteria/1"))
            .andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$.id").value(1)
                jsonPath("$.name").value("Cafeteria A")
                jsonPath("$.latitude").value(37.7749)
                jsonPath("$.longitude").value(-122.4194)
                jsonPath("$.breakfastTime").value("07:00-10:00")
                jsonPath("$.lunchTime").value("11:00-14:00")
                jsonPath("$.dinnerTime").value("17:00-20:00")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 조회 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun getCafeteriaByIdNotFound() {
        doThrow(CafeteriaNotFoundException::class).whenever(cafeteriaService).getCafeteriaById(1)
        mockMvc
            .perform(get("/api/v1/cafeteria/1"))
            .andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("CAFETERIA_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 조회 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun getCafeteriaByIdException() {
        doThrow(RuntimeException("Unexpected error")).whenever(cafeteriaService).getCafeteriaById(1)
        mockMvc
            .perform(get("/api/v1/cafeteria/1"))
            .andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun updateCafeteria() {
        val updatedCafeteria =
            Cafeteria(
                id = 1,
                campusID = 1,
                name = "Updated Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campus = null,
            )
        val payload =
            UpdateCafeteriaRequest(
                campusID = 1,
                name = "Updated Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
            )
        doReturn(updatedCafeteria).whenever(cafeteriaService).updateCafeteria(1, payload)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$.id").value(1)
                jsonPath("$.name").value("Updated Cafeteria A")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 수정 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun updateCafeteriaNotFound() {
        val payload =
            UpdateCafeteriaRequest(
                campusID = 1,
                name = "Updated Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
            )
        doThrow(CafeteriaNotFoundException::class).whenever(cafeteriaService).updateCafeteria(1, payload)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("CAFETERIA_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 수정 (캠퍼스 ID가 존재하지 않음)")
    @WithCustomMockUser(username = "test_user")
    fun updateCafeteriaCampusNotFound() {
        val payload =
            UpdateCafeteriaRequest(
                campusID = 999,
                name = "Updated Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
            )
        doThrow(CampusNotFoundException::class).whenever(cafeteriaService).updateCafeteria(1, payload)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect {
                status().isBadRequest
                content().contentType("application/json")
                jsonPath("$.message").value("CAMPUS_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 수정 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun updateCafeteriaException() {
        val payload =
            UpdateCafeteriaRequest(
                campusID = 1,
                name = "Updated Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
            )
        doThrow(RuntimeException("Unexpected error")).whenever(cafeteriaService).updateCafeteria(1, payload)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun deleteCafeteriaById() {
        mockMvc
            .perform(delete("/api/v1/cafeteria/1"))
            .andExpect {
                status().isNoContent
            }
        verify(cafeteriaService).deleteCafeteriaById(1)
    }

    @Test
    @DisplayName("학식 식당 항목 삭제 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun deleteCafeteriaByIdNotFound() {
        doThrow(CafeteriaNotFoundException::class).whenever(cafeteriaService).deleteCafeteriaById(1)
        mockMvc
            .perform(delete("/api/v1/cafeteria/1"))
            .andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("CAFETERIA_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("학식 식당 항목 삭제 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun deleteCafeteriaByIdException() {
        doThrow(RuntimeException("Unexpected error")).whenever(cafeteriaService).deleteCafeteriaById(1)
        mockMvc
            .perform(delete("/api/v1/cafeteria/1"))
            .andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
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
            date = null,
            type = null,
        )
        // With date and cafeteriaID parameters
        mockMvc
            .perform(get("/api/v1/cafeteria/1/menu"))
            .andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$.result[0].seq").value(1)
                jsonPath("$.result[0].restaurantID").value(1)
                jsonPath("$.result[0].date").value(LocalDate.now().toString())
                jsonPath("$.result[0].type").value("lunch")
                jsonPath("$.result[0].food").value("Spaghetti")
                jsonPath("$.result[0].price").value("$10")
                jsonPath("$.result[1].seq").value(2)
                jsonPath("$.result[1].restaurantID").value(1)
                jsonPath("$.result[1].date").value(LocalDate.now().toString())
                jsonPath("$.result[1].type").value("dinner")
                jsonPath("$.result[1].food").value("Pizza")
                jsonPath("$.result[1].price").value("$12")
                jsonPath("$.result[2].seq").value(3)
                jsonPath("$.result[2].restaurantID").value(1)
                jsonPath("$.result[2].date").value(LocalDate.now().toString())
                jsonPath("$.result[2].type").value("breakfast")
                jsonPath("$.result[2].food").value("Pancakes")
                jsonPath("$.result[2].price").value("$8")
            }
    }

    @Test
    @DisplayName("메뉴 목록 조회 (존재하지 않는 식당 ID)")
    @WithCustomMockUser(username = "test_user")
    fun getMenuListWithNonExistentCafeteria() {
        doThrow(CafeteriaNotFoundException::class).whenever(menuService).getMenuList(
            cafeteriaID = 1,
            date = null,
            type = null,
        )
        // With date and cafeteriaID parameters
        mockMvc
            .perform(get("/api/v1/cafeteria/1/menu"))
            .andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("CAFETERIA_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("메뉴 목록 조회 (예외 처리)")
    @WithCustomMockUser(username = "test_user")
    fun getMenuListException() {
        doThrow(
            RuntimeException("Unexpected error"),
        ).whenever(menuService).getMenuList(
            cafeteriaID = 1,
            date = null,
            type = null,
        )
        // With date and cafeteriaID parameters
        mockMvc
            .perform(get("/api/v1/cafeteria/1/menu"))
            .andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }

    @Test
    @DisplayName("메뉴 생성 성공")
    @WithCustomMockUser(username = "test_user")
    fun createMenu() {
        val menuRequest =
            MenuRequest(
                date = LocalDate.now(),
                type = "lunch",
                food = "Burger",
                price = "$6",
            )

        val createdMenu =
            Menu(
                seq = 1,
                restaurantID = 1,
                date = menuRequest.date,
                type = menuRequest.type,
                food = menuRequest.food,
                price = menuRequest.price,
                cafeteria = null,
            )
        doReturn(createdMenu).whenever(menuService).createMenu(1, menuRequest)
        mockMvc
            .perform(
                post("/api/v1/cafeteria/1/menu")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isCreated
                content().contentType("application/json")
                jsonPath("$.seq").value(1)
                jsonPath("$.restaurantID").value(1)
                jsonPath("$.date").value(menuRequest.date.toString())
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
                date = LocalDate.now(),
                type = "lunch",
                food = "Burger",
                price = "$6",
            )
        doThrow(CafeteriaNotFoundException()).whenever(menuService).createMenu(999, menuRequest)
        mockMvc
            .perform(
                post("/api/v1/cafeteria/999/menu")
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
                date = LocalDate.now(),
                type = "lunch",
                food = "Burger",
                price = "$6",
            )
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).createMenu(1, menuRequest)
        mockMvc
            .perform(
                post("/api/v1/cafeteria/1/menu")
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
        doReturn(menu).whenever(menuService).getMenuById(1, menuId)
        mockMvc
            .perform(get("/api/v1/cafeteria/1/menu/$menuId"))
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
    @DisplayName("메뉴 ID로 조회 실패 (존재하지 않는 식당 ID)")
    @WithCustomMockUser(username = "test_user")
    fun getMenuWithNonExistentCafeteria() {
        val menuId = 999
        doThrow(CafeteriaNotFoundException()).whenever(menuService).getMenuById(1, menuId)
        mockMvc
            .perform(get("/api/v1/cafeteria/1/menu/$menuId"))
            .andExpect {
                status().isNotFound
                content().contentType("application/json")
                jsonPath("$.message").value("CAFETERIA_NOT_FOUND")
            }
    }

    @Test
    @DisplayName("메뉴 ID로 조회 실패 (메뉴 없음)")
    @WithCustomMockUser(username = "test_user")
    fun getMenuByIdFail() {
        val menuId = 999
        doThrow(MenuNotFoundException()).whenever(menuService).getMenuById(1, menuId)
        mockMvc
            .perform(get("/api/v1/cafeteria/1/menu/$menuId"))
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
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).getMenuById(1, menuId)
        mockMvc
            .perform(get("/api/v1/cafeteria/1/menu/$menuId"))
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
                date = LocalDate.now(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        val updatedMenu =
            Menu(
                seq = menuId,
                restaurantID = 1,
                date = menuRequest.date,
                type = menuRequest.type,
                food = menuRequest.food,
                price = menuRequest.price,
                cafeteria = null,
            )
        doReturn(updatedMenu).whenever(menuService).updateMenu(1, menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/1/menu/$menuId")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(menuRequest)),
            ).andExpect {
                status().isOk
                content().contentType("application/json")
                jsonPath("$.seq").value(menuId)
                jsonPath("$.restaurantID").value(1)
                jsonPath("$.date").value(menuRequest.date.toString())
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
                date = LocalDate.now(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        doThrow(MenuNotFoundException()).whenever(menuService).updateMenu(1, menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/1/menu/$menuId")
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
                date = LocalDate.now(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        doThrow(CafeteriaNotFoundException()).whenever(menuService).updateMenu(999, menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/999/menu/$menuId")
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
                date = LocalDate.now(),
                type = "lunch",
                food = "Updated Burger",
                price = "$7",
            )
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).updateMenu(1, menuId, menuRequest)
        mockMvc
            .perform(
                put("/api/v1/cafeteria/1/menu/$menuId")
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
            .perform(delete("/api/v1/cafeteria/1/menu/$menuId"))
            .andExpect {
                status().isNoContent
            }
        verify(menuService).deleteMenuById(1, menuId)
    }

    @Test
    @DisplayName("메뉴 삭제 실패 (메뉴 없음)")
    @WithCustomMockUser(username = "test_user")
    fun deleteMenuFail() {
        val menuId = 999
        doThrow(MenuNotFoundException()).whenever(menuService).deleteMenuById(1, menuId)
        mockMvc
            .perform(delete("/api/v1/cafeteria/1/menu/$menuId"))
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
        doThrow(RuntimeException("Unexpected error")).whenever(menuService).deleteMenuById(1, menuId)
        mockMvc
            .perform(delete("/api/v1/cafeteria/1/menu/$menuId"))
            .andExpect {
                status().isInternalServerError
                content().contentType("application/json")
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
    }
}
