package app.hyuabot.backend.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean

@OpenAPIDefinition(
    info = Info(title = "HYUabot Backend API", version = "v1.0.0", description = "API documentation for HYUabot Backend"),
    servers = [
        Server(url = "http://localhost:8080", description = "Local Development Server"),
        Server(url = "\${springdoc.swagger-ui.server}", description = "Production Server"),
    ],
)
class SwaggerConfig {
    @Bean
    fun groupedOpenAPI(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("v1")
            .pathsToMatch("/api/v1/**")
            .build()
}
