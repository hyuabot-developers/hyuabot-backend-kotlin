package app.hyuabot.backend.security

import org.springframework.security.test.context.support.WithSecurityContext

@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithCustomMockUserSecurityContextFactory::class)
annotation class WithCustomMockUser(
    val username: String = "testUser",
)
