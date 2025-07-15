package app.hyuabot.backend.utility

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import kotlin.test.assertEquals

class ResponseBuilderTest {
    @Test
    @DisplayName("Test response with message and cookies")
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
        assertEquals(response.statusCode, HttpStatus.OK)
        assertEquals(response.body?.message, "Success")
        response.headers.forEach {
            if (it.key == "Set-Cookie") {
                assertEquals(it.value.size, 2)
                assertEquals(it.value[0].contains("accessToken"), true)
                assertEquals(it.value[1].contains("refreshToken"), true)
            }
        }
    }

    @Test
    @DisplayName("Test response with message only")
    fun createResponseWithMessage() {
        val response = ResponseBuilder.response(HttpStatus.OK, message = "Operation successful")
        assertEquals(response.statusCode, HttpStatus.OK)
        assertEquals(response.body?.message, "Operation successful")
        assertEquals(response.headers.contentType?.toString(), "application/json")
    }

    @Test
    @DisplayName("Test response with body")
    fun createResponseWithBody() {
        val response = ResponseBuilder.response(HttpStatus.OK, body = mapOf("key" to "value"))
        assertEquals(response.statusCode, HttpStatus.OK)
        assertEquals(response.body is Map<*, *>, true)
        assertEquals((response.body as Map<*, *>)["key"], "value")
        assertEquals(response.headers.contentType?.toString(), "application/json")
    }

    @Test
    @DisplayName("Test exception response")
    fun createExceptionResponse() {
        val exception = ResponseBuilder.exception(HttpStatus.BAD_REQUEST, "Bad Request")
        assertEquals(exception.statusCode, HttpStatus.BAD_REQUEST)
        assertEquals(exception.reason, "Bad Request")
    }
}
