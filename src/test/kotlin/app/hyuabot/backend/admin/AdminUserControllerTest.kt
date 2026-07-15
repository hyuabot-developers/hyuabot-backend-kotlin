package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.AdminUserResponse
import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.security.WithCustomMockUser
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {
    @MockitoBean
    lateinit var service: AdminUserService

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val response =
        AdminUserResponse(
            username = "user",
            nickname = "User",
            email = "user@example.com",
            phone = "01000000000",
            active = true,
            permissions = listOf(AdminPermission.SHUTTLE),
        )

    @Test
    @WithCustomMockUser
    fun superAdminCanListUsers() {
        whenever(service.getUsers()).thenReturn(listOf(response))

        mockMvc
            .perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].username").value("user"))
    }

    @Test
    @WithCustomMockUser(permissions = [AdminPermission.SHUTTLE])
    fun managementPermissionCannotListUsers() {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden)
    }

    @Test
    fun anonymousUserCannotListUsers() {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized)
    }

    @Test
    @WithCustomMockUser
    fun superAdminCanUpdateUser() {
        val request = UpdateAdminUserRequest(true, setOf(AdminPermission.SHUTTLE))
        whenever(service.updateUser("user", request)).thenReturn(response)

        mockMvc
            .perform(
                put("/api/v1/admin/users/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.permissions[0]").value("SHUTTLE"))
    }

    @Test
    @WithCustomMockUser
    fun updateMissingUserReturnsNotFound() {
        val request = UpdateAdminUserRequest(true, emptySet())
        whenever(service.updateUser("missing", request)).thenThrow(AdminUserNotFoundException())

        mockMvc
            .perform(
                put("/api/v1/admin/users/missing")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("ADMIN_USER_NOT_FOUND"))
    }

    @Test
    @WithCustomMockUser
    fun updateLastSuperAdminReturnsConflict() {
        val request = UpdateAdminUserRequest(false, emptySet())
        whenever(service.updateUser("user", request)).thenThrow(LastSuperAdminException())

        mockMvc
            .perform(
                put("/api/v1/admin/users/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("LAST_SUPER_ADMIN_REQUIRED"))
    }
}
