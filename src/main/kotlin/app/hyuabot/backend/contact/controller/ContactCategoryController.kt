package app.hyuabot.backend.contact.controller

import app.hyuabot.backend.contact.ContactService
import app.hyuabot.backend.contact.domain.ContactCategoryListResponse
import app.hyuabot.backend.contact.domain.ContactCategoryRequest
import app.hyuabot.backend.contact.domain.ContactCategoryResponse
import app.hyuabot.backend.contact.domain.ContactListResponse
import app.hyuabot.backend.contact.domain.ContactResponse
import app.hyuabot.backend.contact.exception.ContactCategoryNotFoundException
import app.hyuabot.backend.contact.exception.DuplicateCategoryException
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/contact/category")
@RestController
@Tag(name = "Contact", description = "전화부 API")
class ContactCategoryController {
    @Autowired private lateinit var service: ContactService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "전화부 카테고리 조회", description = "전화부 카테고리를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "전화부 카테고리 조회 성공",
        content = [Content(schema = Schema(implementation = ContactCategoryListResponse::class))],
    )
    fun getContactCategory(): ContactCategoryListResponse =
        ContactCategoryListResponse(
            service.getContactCategories().map {
                ContactCategoryResponse(
                    seq = it.id!!,
                    name = it.name,
                )
            },
        )

    @PostMapping("")
    @Operation(
        summary = "전화부 카테고리 추가",
        description = "전화부 카테고리를 추가합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [Content(schema = Schema(implementation = ContactCategoryRequest::class))],
                description = "추가할 전화부 카테고리 정보",
            ),
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "전화부 카테고리 추가 성공",
            content = [Content(schema = Schema(implementation = ContactCategoryResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "전화부 카테고리 추가 실패 - 중복된 이름",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "중복된 이름",
                            value = """{"message": "DUPLICATE_CATEGORY_NAME"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun createContactCategory(
        @RequestBody request: ContactCategoryRequest,
    ): ResponseEntity<*> {
        try {
            val category = service.createContactCategory(request)
            return ResponseBuilder.response(
                status = HttpStatus.CREATED,
                body =
                    ContactCategoryResponse(
                        seq = category.id!!,
                        name = category.name,
                    ),
            )
        } catch (_: DuplicateCategoryException) {
            return ResponseBuilder.response(
                status = HttpStatus.CONFLICT,
                body = ResponseBuilder.Message("DUPLICATE_CATEGORY_NAME"),
            )
        } catch (e: Exception) {
            logger.error("Unexpected error occurred while creating contact category", e)
            return ResponseBuilder.response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                body = ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{seq}")
    @Operation(
        summary = "전화부 카테고리 조회",
        description = "전화부 카테고리를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "전화부 카테고리 ID",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "전화부 카테고리 조회 성공",
            content = [Content(schema = Schema(implementation = ContactCategoryResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "전화부 카테고리 조회 실패 - 존재하지 않는 ID",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "존재하지 않는 ID",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun getContactCategoryById(
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            return service.getContactCategoryById(seq).let {
                ResponseBuilder.response(
                    status = HttpStatus.OK,
                    body =
                        ContactCategoryResponse(
                            seq = it.id!!,
                            name = it.name,
                        ),
                )
            }
        } catch (_: ContactCategoryNotFoundException) {
            return ResponseBuilder.response(
                status = HttpStatus.NOT_FOUND,
                body = ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Unexpected error occurred while retrieving contact category", e)
            return ResponseBuilder.response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                body = ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "전화부 카테고리 삭제",
        description = "전화부 카테고리를 삭제합니다. 해당 카테고리에 속한 일정도 모두 삭제됩니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "전화부 카테고리 ID",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "204",
            description = "전화부 카테고리 삭제 성공",
        ),
        ApiResponse(
            responseCode = "404",
            description = "전화부 카테고리 삭제 실패 - 존재하지 않는 ID",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "존재하지 않는 ID",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun deleteContactCategory(
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            service.deleteContactCategoryById(seq)
            return ResponseBuilder.response(
                status = HttpStatus.NO_CONTENT,
                body = null,
            )
        } catch (_: ContactCategoryNotFoundException) {
            return ResponseBuilder.response(
                status = HttpStatus.NOT_FOUND,
                body = ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Unexpected error occurred while deleting contact category", e)
            return ResponseBuilder.response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                body = ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{seq}/contact")
    @Operation(
        summary = "전화부 카테고리에 속한 전화부 조회",
        description = "전화부 카테고리에 속한 연락처를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "전화부 카테고리 ID",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "전화부 카테고리에 속한 연락처 조회 성공",
            content = [Content(schema = Schema(implementation = ContactListResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "전화부 카테고리에 속한 연락처 조회 실패 - 존재하지 않는 ID",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "존재하지 않는 ID",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun getContactByCategoryId(
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                status = HttpStatus.OK,
                body =
                    ContactListResponse(
                        service.getContactByCategoryId(seq).map {
                            ContactResponse(
                                seq = it.id!!,
                                name = it.name,
                                phone = it.phone,
                                campusID = it.campusID,
                                categoryID = it.categoryID,
                            )
                        },
                    ),
            )
        } catch (_: ContactCategoryNotFoundException) {
            return ResponseBuilder.response(
                status = HttpStatus.NOT_FOUND,
                body = ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        }
    }
}
