package app.hyuabot.backend.config

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SwaggerConfigTest {
    @Test
    @DisplayName("Test Swagger loads grouped API documentation")
    fun groupedOpenAPI() {
        val swaggerConfig = SwaggerConfig()
        val groupedOpenApi = swaggerConfig.groupedOpenAPI()

        assert(groupedOpenApi.group == "v1")
        assert(groupedOpenApi.pathsToMatch.contains("/api/v1/**"))
    }
}
