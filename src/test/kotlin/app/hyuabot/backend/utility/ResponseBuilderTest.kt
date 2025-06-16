package app.hyuabot.backend.utility

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie

class ResponseBuilderTest {
    @Test
    @DisplayName("Test response with message amd cookies")
    fun createResponseWithCookies() {
        val response =
            ResponseBuilder.response(
                HttpStatus.OK,
                message = "Success",
                cookies =
                    listOf(
                        ResponseCookie
                            .from("accessToken", "token123")
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .build(),
                        ResponseCookie
                            .from("refreshToken", "refreshToken123")
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .build(),
                    ),
            )
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body?.message == "Success")
        response.headers.forEach {
            if (it.key == "Set-Cookie") {
                assert(it.value.size == 2)
                assert(it.value[0].contains("accessToken"))
                assert(it.value[1].contains("refreshToken"))
            }
        }
    }

    @Test
    @DisplayName("Test response with message only")
    fun createResponseWithMessage() {
        val response = ResponseBuilder.response(HttpStatus.OK, message = "Operation successful")
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body?.message == "Operation successful")
        assert(response.headers.contentType?.toString() == "application/json")
    }

    @Test
    @DisplayName("Test response with body")
    fun createResponseWithBody() {
        val response = ResponseBuilder.response(HttpStatus.OK, body = mapOf("key" to "value"))
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body is Map<*, *>)
        assert((response.body as Map<*, *>)["key"] == "value")
        assert(response.headers.contentType?.toString() == "application/json")
    }

    @Test
    @DisplayName("Test exception response")
    fun createExceptionResponse() {
        val exception = ResponseBuilder.exception(HttpStatus.BAD_REQUEST, "Bad Request")
        assert(exception.statusCode == HttpStatus.BAD_REQUEST)
        assert(exception.reason == "Bad Request")
    }
}
