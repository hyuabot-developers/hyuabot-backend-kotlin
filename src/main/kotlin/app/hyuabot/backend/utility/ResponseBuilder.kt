package app.hyuabot.backend.utility

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException

class ResponseBuilder {
    data class Message(
        val message: String? = null,
    )

    companion object {
        fun response(
            status: HttpStatus,
            message: String? = null,
        ): ResponseEntity<Message> {
            val message = Message(message)
            val header = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            return ResponseEntity(message, header, status)
        }

        fun exception(
            status: HttpStatus,
            message: String? = null,
        ): ResponseStatusException = ResponseStatusException(status, message)
    }
}
