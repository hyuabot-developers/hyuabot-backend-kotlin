package app.hyuabot.backend.contact.controller

import app.hyuabot.backend.contact.ContactService
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/contact/version")
@RestController
@Tag(name = "Contact", description = "전화부 API")
class ContactVersionController {
    @Autowired private lateinit var service: ContactService

    @GetMapping("")
    @Operation(
        summary = "전화부 버전 조회",
        description = "전화부 데이터의 버전을 조회합니다. 관리자 페이지에서 전화부 데이터를 수정한 경우 버전이 갱신됩니다.",
    )
    fun getCalendarVersion(): ResponseEntity<ResponseBuilder.Message> {
        service.getContactVersion().let {
            return ResponseBuilder.response(
                status = HttpStatus.OK,
                body = ResponseBuilder.Message(it.name),
            )
        }
    }
}
