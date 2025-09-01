package app.hyuabot.backend.contact.controller

import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.contact.ContactService
import app.hyuabot.backend.contact.domain.ContactListResponse
import app.hyuabot.backend.contact.domain.ContactRequest
import app.hyuabot.backend.contact.domain.ContactResponse
import app.hyuabot.backend.contact.exception.ContactCategoryNotFoundException
import app.hyuabot.backend.contact.exception.ContactNotFoundException
import app.hyuabot.backend.database.exception.PhoneNumberNotValidException
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/contact/contact")
@RestController
@Tag(name = "Contact", description = "전화부 관련 API")
class ContactController {
    @Autowired private lateinit var service: ContactService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "연락처 전체 조회", description = "연락처를 전체 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "연락처 전체 조회 성공",
        content = [Content(schema = Schema(implementation = ContactListResponse::class))],
    )
    fun getAllContacts(): ContactListResponse =
        ContactListResponse(
            service.getAllContacts().map {
                ContactResponse(
                    seq = it.id!!,
                    name = it.name,
                    phone = it.phone,
                    categoryID = it.categoryID,
                    campusID = it.campusID,
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "연락처 등록", description = "연락처를 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "연락처 등록 성공",
                content = [Content(schema = Schema(implementation = ContactResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "연락처 등록 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 캠퍼스",
                                value = """{"message":"CAMPUS_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 카테고리",
                                value = """{"message":"CATEGORY_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "잘못된 전화번호 형식",
                                value = """{"message": "PHONE_NUMBER_NOT_VALID"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createContact(
        @RequestBody request: ContactRequest,
    ): ResponseEntity<*> =
        try {
            val contact = service.createContact(request)
            ResponseBuilder.response(
                HttpStatus.CREATED,
                ContactResponse(
                    seq = contact.id!!,
                    name = contact.name,
                    phone = contact.phone,
                    categoryID = contact.categoryID,
                    campusID = contact.campusID,
                ),
            )
        } catch (_: CampusNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CAMPUS_NOT_FOUND"),
            )
        } catch (_: ContactCategoryNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (_: PhoneNumberNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("PHONE_NUMBER_NOT_VALID"),
            )
        } catch (e: Exception) {
            logger.error("Failed to create contact", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{seq}")
    @Operation(
        summary = "연락처 단건 조회",
        description = "연락처를 단건 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations
                .Parameter(name = "seq", description = "연락처 ID", example = "1"),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연락처 단건 조회 성공",
                content = [Content(schema = Schema(implementation = ContactResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "연락처 단건 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 연락처",
                                value = """{"message":"CONTACT_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getContact(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            val contact = service.getContactById(seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                ContactResponse(
                    seq = contact.id!!,
                    name = contact.name,
                    phone = contact.phone,
                    categoryID = contact.categoryID,
                    campusID = contact.campusID,
                ),
            )
        } catch (_: ContactNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CONTACT_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to get contact", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{seq}")
    @Operation(
        summary = "연락처 수정",
        description = "연락처를 수정합니다.",
        parameters = [
            io.swagger.v3.oas.annotations
                .Parameter(name = "seq", description = "연락처 ID", example = "1"),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연락처 수정 성공",
                content = [Content(schema = Schema(implementation = ContactResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "연락처 수정 실패 - 잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 캠퍼스",
                                value = """{"message":"CAMPUS_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 카테고리",
                                value = """{"message":"CATEGORY_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "잘못된 전화번호 형식",
                                value = """{"message":"PHONE_NUMBER_NOT_VALID"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "연락처 수정 실패 - 연락처 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 연락처",
                                value = """{"message":"CONTACT_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateContact(
        @PathVariable seq: Int,
        @RequestBody request: ContactRequest,
    ): ResponseEntity<*> =
        try {
            val contact = service.updateContact(seq, request)
            ResponseBuilder.response(
                HttpStatus.OK,
                ContactResponse(
                    seq = contact.id!!,
                    name = contact.name,
                    phone = contact.phone,
                    categoryID = contact.categoryID,
                    campusID = contact.campusID,
                ),
            )
        } catch (_: ContactNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CONTACT_NOT_FOUND"),
            )
        } catch (_: CampusNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CAMPUS_NOT_FOUND"),
            )
        } catch (_: ContactCategoryNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (_: PhoneNumberNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("PHONE_NUMBER_NOT_VALID"),
            )
        } catch (e: Exception) {
            logger.error("Failed to update contact", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "연락처 삭제",
        description = "연락처를 삭제합니다.",
        parameters = [
            io.swagger.v3.oas.annotations
                .Parameter(name = "seq", description = "연락처 ID", example = "1"),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "연락처 삭제 성공",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "연락처 삭제 실패 - 연락처 없음",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun deleteContact(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteContactById(seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: ContactNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CONTACT_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to delete contact", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
