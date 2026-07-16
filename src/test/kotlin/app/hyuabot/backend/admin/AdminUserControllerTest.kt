package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.AdminUserInvitationResponse
import app.hyuabot.backend.admin.domain.AdminUserResponse
import app.hyuabot.backend.admin.domain.CreateAdminUserRequest
import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.admin.exception.PendingUserActivationException
import app.hyuabot.backend.admin.exception.SelfDeletionException
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.DuplicateUserIDException
import app.hyuabot.backend.auth.exception.InvalidInvitationException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.security.WithCustomMockUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {
    @Autowired
    lateinit var controller: AdminUserController

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
    @WithCustomMockUser(username = "jil8885")
    fun superAdminCanCreateUserAndReissueInvitation() {
        val request = CreateAdminUserRequest("new-user", "New User", "new@example.com", "", emptySet())
        val invitation = AdminUserInvitationResponse(response.copy(active = false), "token", ZonedDateTime.now().plusHours(24))
        whenever(service.createUser(request, "jil8885")).thenReturn(invitation)
        whenever(service.reissueInvitation("new-user", "jil8885")).thenReturn(invitation)

        mockMvc
            .perform(
                post("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").value("token"))

        mockMvc
            .perform(post("/api/v1/admin/users/new-user/invitation"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("token"))
    }

    @Test
    fun directCreateRejectsMissingSecurityContext() {
        SecurityContextHolder.clearContext()

        assertThrows<RuntimeException> {
            controller.createUser(CreateAdminUserRequest("user", "User", "user@example.com", "", emptySet()))
        }
    }

    @Test
    @WithCustomMockUser
    fun createUserMapsValidationAndDuplicateErrors() {
        val request = CreateAdminUserRequest("new-user", "New User", "new@example.com", "", emptySet())

        doThrow(DuplicateUserIDException()).whenever(service).createUser(request, "testUser")
        performCreate(request).andExpect(status().isConflict).andExpect(jsonPath("$.message").value("DUPLICATE_USER_ID"))

        doThrow(DuplicateEmailException()).whenever(service).createUser(request, "testUser")
        performCreate(request).andExpect(status().isConflict).andExpect(jsonPath("$.message").value("DUPLICATE_EMAIL"))

        doThrow(InvalidUserInputException("INVALID_USER_INPUT")).whenever(service).createUser(request, "testUser")
        performCreate(request).andExpect(status().isBadRequest).andExpect(jsonPath("$.message").value("INVALID_USER_INPUT"))
    }

    @Test
    @WithCustomMockUser
    fun reissueInvitationMapsMissingAndConfiguredUser() {
        whenever(service.reissueInvitation("missing", "testUser")).thenThrow(AdminUserNotFoundException())
        mockMvc
            .perform(post("/api/v1/admin/users/missing/invitation"))
            .andExpect(status().isNotFound)

        whenever(service.reissueInvitation("configured", "testUser")).thenThrow(InvalidInvitationException())
        mockMvc
            .perform(post("/api/v1/admin/users/configured/invitation"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("USER_ALREADY_SETUP"))
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

    @Test
    @WithCustomMockUser
    fun updatePendingUserReturnsConflict() {
        val request = UpdateAdminUserRequest(true, emptySet())
        whenever(service.updateUser("pending", request)).thenThrow(PendingUserActivationException())

        mockMvc
            .perform(
                put("/api/v1/admin/users/pending")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("USER_SETUP_REQUIRED"))
    }

    @Test
    @WithCustomMockUser
    fun superAdminCanDeleteUser() {
        mockMvc
            .perform(delete("/api/v1/admin/users/user"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("ADMIN_USER_DELETED"))
    }

    @Test
    @WithCustomMockUser
    fun deleteUserMapsNotFoundAndSafetyConflicts() {
        doThrow(AdminUserNotFoundException()).whenever(service).deleteUser("missing", "testUser")
        mockMvc
            .perform(delete("/api/v1/admin/users/missing"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("ADMIN_USER_NOT_FOUND"))

        doThrow(LastSuperAdminException()).whenever(service).deleteUser("last-admin", "testUser")
        mockMvc
            .perform(delete("/api/v1/admin/users/last-admin"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("LAST_SUPER_ADMIN_REQUIRED"))

        doThrow(SelfDeletionException()).whenever(service).deleteUser("testUser", "testUser")
        mockMvc
            .perform(delete("/api/v1/admin/users/testUser"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("SELF_DELETION_NOT_ALLOWED"))
    }

    private fun performCreate(request: CreateAdminUserRequest) =
        mockMvc.perform(
            post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
}
