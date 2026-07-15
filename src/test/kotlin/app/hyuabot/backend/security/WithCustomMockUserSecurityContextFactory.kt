package app.hyuabot.backend.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithCustomMockUserSecurityContextFactory : WithSecurityContextFactory<WithCustomMockUser> {
    override fun createSecurityContext(annotation: WithCustomMockUser): SecurityContext {
        val auth =
            UsernamePasswordAuthenticationToken(
                JWTUser(
                    username = annotation.username,
                    password = "testPassword",
                    permissions = annotation.permissions.toSet(),
                ),
                null,
                annotation.permissions
                    .toSet()
                    .effectivePermissions()
                    .map {
                        org.springframework.security.core.authority
                            .SimpleGrantedAuthority(it.name)
                    },
            )
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        return context
    }
}
