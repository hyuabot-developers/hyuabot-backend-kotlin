package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.DevicePushToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DevicePushTokenRepository : JpaRepository<DevicePushToken, Long> {
    fun findByInstallationId(installationId: UUID): List<DevicePushToken>

    fun findByProviderAndToken(
        provider: String,
        token: String,
    ): DevicePushToken?

    fun deleteByProviderAndToken(
        provider: String,
        token: String,
    )
}
