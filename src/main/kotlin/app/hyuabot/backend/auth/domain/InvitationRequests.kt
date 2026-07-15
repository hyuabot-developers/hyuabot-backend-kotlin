package app.hyuabot.backend.auth.domain

import java.time.ZonedDateTime

data class ValidateInvitationRequest(
    val token: String,
)

data class CompleteInvitationRequest(
    val token: String,
    val password: String,
)

data class InvitationValidationResponse(
    val valid: Boolean,
    val expiresAt: ZonedDateTime?,
)

data class UpdateProfileRequest(
    val nickname: String,
    val email: String,
    val phone: String,
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
