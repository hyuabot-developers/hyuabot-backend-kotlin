package app.hyuabot.backend.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithCustomMockUserSecurityContextFactory : WithSecurityContextFactory<WithCustomMockUser> {
    override fun createSecurityContext(annotation: WithCustomMockUser?): SecurityContext? {
        val username = annotation?.username ?: return null
        val auth =
            UsernamePasswordAuthenticationToken(
                JWTUser(
                    username = username,
                    password = "testPassword",
                ),
                null,
                emptyList(),
            )
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        return context
    }
}
