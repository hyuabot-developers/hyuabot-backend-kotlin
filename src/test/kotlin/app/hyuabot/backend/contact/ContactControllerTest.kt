package app.hyuabot.backend.contact

import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.contact.domain.ContactCategoryRequest
import app.hyuabot.backend.contact.domain.ContactRequest
import app.hyuabot.backend.contact.exception.ContactCategoryNotFoundException
import app.hyuabot.backend.contact.exception.ContactNotFoundException
import app.hyuabot.backend.contact.exception.DuplicateCategoryException
import app.hyuabot.backend.database.entity.Contact
import app.hyuabot.backend.database.entity.ContactCategory
import app.hyuabot.backend.database.entity.ContactVersion
import app.hyuabot.backend.database.exception.PhoneNumberNotValidException
import app.hyuabot.backend.security.WithCustomMockUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactControllerTest {
    @MockitoBean
    private lateinit var contactService: ContactService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("전화부 버전 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactVersion() {
        doReturn(
            ContactVersion(
                id = 1,
                name = "1.0",
                createdAt = ZonedDateTime.now(),
            ),
        ).whenever(contactService).getContactVersion()
        mockMvc
            .perform(get("/api/v1/contact/version"))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("전화부 카테고리 목록 조회 테스트")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactCategories() {
        doReturn(
            listOf(
                ContactCategory(
                    id = 1,
                    name = "General",
                    contact = emptyList(),
                ),
                ContactCategory(
                    id = 2,
                    name = "Emergency",
                    contact = emptyList(),
                ),
            ),
        ).whenever(contactService).getContactCategories()
        mockMvc
            .perform(get("/api/v1/contact/category"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].name").value("General"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].name").value("Emergency"))
    }

    @Test
    @DisplayName("전화부 카테고리 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContactCategory() {
        doReturn(
            ContactCategory(
                id = 1,
                name = "General",
                contact = emptyList(),
            ),
        ).whenever(contactService).createContactCategory(
            ContactCategoryRequest(
                name = "General",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ContactCategoryRequest(name = "General"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("General"))
    }

    @Test
    @DisplayName("전화부 카테고리 생성 - 중복된 이름")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContactCategoryDuplicateName() {
        doThrow(DuplicateCategoryException()).whenever(contactService).createContactCategory(
            ContactCategoryRequest(
                name = "General",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ContactCategoryRequest(name = "General"))),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_CATEGORY_NAME"))
    }

    @Test
    @DisplayName("전화부 카테고리 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContactCategoryOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(contactService).createContactCategory(
            ContactCategoryRequest(
                name = "General",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ContactCategoryRequest(name = "General"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전화부 카테고리 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactCategoryById() {
        doReturn(
            ContactCategory(
                id = 1,
                name = "General",
                contact = emptyList(),
            ),
        ).whenever(contactService).getContactCategoryById(1)
        mockMvc
            .perform(get("/api/v1/contact/category/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("General"))
    }

    @Test
    @DisplayName("전화부 카테고리 조회 - 존재하지 않는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactCategoryByIdNotFound() {
        doThrow(ContactCategoryNotFoundException()).whenever(contactService).getContactCategoryById(999)
        mockMvc
            .perform(get("/api/v1/contact/category/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("전화부 카테고리 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactCategoryByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(contactService).getContactCategoryById(1)
        mockMvc
            .perform(get("/api/v1/contact/category/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전화부 카테고리 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteContactCategoryById() {
        mockMvc
            .perform(delete("/api/v1/contact/category/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("전화부 카테고리 삭제 - 존재하지 않는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteContactCategoryByIdNotFound() {
        doThrow(ContactCategoryNotFoundException()).whenever(contactService).deleteContactCategoryById(999)
        mockMvc
            .perform(delete("/api/v1/contact/category/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("전화부 카테고리 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteContactCategoryByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(contactService).deleteContactCategoryById(1)
        mockMvc
            .perform(delete("/api/v1/contact/category/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전화부 카테고리별 연락처 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactsByCategoryId() {
        doReturn(
            listOf(
                Contact(
                    id = 1,
                    campusID = 1,
                    name = "홍길동",
                    phone = "010-1234-5678",
                    categoryID = 1,
                    category = ContactCategory(id = 1, name = "General", contact = emptyList()),
                ),
                Contact(
                    id = 2,
                    campusID = 1,
                    name = "김철수",
                    phone = "010-8765-4321",
                    categoryID = 1,
                    category = ContactCategory(id = 1, name = "General", contact = emptyList()),
                ),
            ),
        ).whenever(contactService).getContactByCategoryId(1)
        mockMvc
            .perform(get("/api/v1/contact/category/1/contact"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].name").value("홍길동"))
            .andExpect(jsonPath("$.result[0].phone").value("010-1234-5678"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].name").value("김철수"))
            .andExpect(jsonPath("$.result[1].phone").value("010-8765-4321"))
    }

    @Test
    @DisplayName("전화부 카테고리별 연락처 목록 조회 - 존재하지 않는 카테고리 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactsByCategoryIdNotFound() {
        doThrow(ContactCategoryNotFoundException()).whenever(contactService).getContactByCategoryId(999)
        mockMvc
            .perform(get("/api/v1/contact/category/999/contact"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetAllContacts() {
        doReturn(
            listOf(
                Contact(
                    id = 1,
                    campusID = 1,
                    name = "홍길동",
                    phone = "010-1234-5678",
                    categoryID = 1,
                    category = ContactCategory(id = 1, name = "General", contact = emptyList()),
                ),
                Contact(
                    id = 2,
                    campusID = 1,
                    name = "김철수",
                    phone = "010-8765-4321",
                    categoryID = 1,
                    category = ContactCategory(id = 1, name = "General", contact = emptyList()),
                ),
            ),
        ).whenever(contactService).getAllContacts()
        mockMvc
            .perform(get("/api/v1/contact/contact"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].name").value("홍길동"))
            .andExpect(jsonPath("$.result[0].phone").value("010-1234-5678"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].name").value("김철수"))
            .andExpect(jsonPath("$.result[1].phone").value("010-8765-4321"))
    }

    @Test
    @DisplayName("연락처 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContact() {
        doReturn(
            Contact(
                id = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
                categoryID = 1,
                category = ContactCategory(id = 1, name = "General", contact = emptyList()),
            ),
        ).whenever(contactService).createContact(
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.phone").value("010-1234-5678"))
    }

    @Test
    @DisplayName("연락처 생성 - 전화번호 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContactInvalidPhone() {
        doThrow(PhoneNumberNotValidException()).whenever(contactService).createContact(
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "invalid-phone",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 1,
                                name = "홍길동",
                                phone = "invalid-phone",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("PHONE_NUMBER_NOT_VALID"))
    }

    @Test
    @DisplayName("연락처 생성 - 카테고리 존재하지 않음")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContactCategoryNotFound() {
        doThrow(ContactCategoryNotFoundException()).whenever(contactService).createContact(
            ContactRequest(
                categoryID = 999,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 999,
                                campusID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 생성 - 캠퍼스 존재하지 않음")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContactCampusNotFound() {
        doThrow(CampusNotFoundException()).whenever(contactService).createContact(
            ContactRequest(
                categoryID = 1,
                campusID = 999,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 999,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CAMPUS_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateContactOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(contactService).createContact(
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/contact/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("연락처 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactById() {
        doReturn(
            Contact(
                id = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
                categoryID = 1,
                category = ContactCategory(id = 1, name = "General", contact = emptyList()),
            ),
        ).whenever(contactService).getContactById(1)
        mockMvc
            .perform(get("/api/v1/contact/contact/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.phone").value("010-1234-5678"))
    }

    @Test
    @DisplayName("연락처 항목 조회 - 존재하지 않는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactByIdNotFound() {
        doThrow(ContactNotFoundException()).whenever(contactService).getContactById(999)
        mockMvc
            .perform(get("/api/v1/contact/contact/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CONTACT_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetContactByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(contactService).getContactById(1)
        mockMvc
            .perform(get("/api/v1/contact/contact/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("연락처 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateContact() {
        doReturn(
            Contact(
                id = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
                categoryID = 1,
                category = ContactCategory(id = 1, name = "General", contact = emptyList()),
            ),
        ).whenever(contactService).updateContact(
            1,
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/contact/contact/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.phone").value("010-1234-5678"))
    }

    @Test
    @DisplayName("연락처 항목 수정 - 존재하지 않는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateContactNotFound() {
        doThrow(ContactNotFoundException()).whenever(contactService).updateContact(
            999,
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/contact/contact/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CONTACT_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 항목 수정 - 전화번호 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateContactInvalidPhone() {
        doThrow(PhoneNumberNotValidException()).whenever(contactService).updateContact(
            1,
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "invalid-phone",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/contact/contact/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 1,
                                name = "홍길동",
                                phone = "invalid-phone",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("PHONE_NUMBER_NOT_VALID"))
    }

    @Test
    @DisplayName("연락처 항목 수정 - 카테고리 존재하지 않음")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateContactCategoryNotFound() {
        doThrow(ContactCategoryNotFoundException()).whenever(contactService).updateContact(
            1,
            ContactRequest(
                categoryID = 999,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/contact/contact/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 999,
                                campusID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 항목 수정 - 캠퍼스 존재하지 않음")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateContactCampusNotFound() {
        doThrow(CampusNotFoundException()).whenever(contactService).updateContact(
            1,
            ContactRequest(
                categoryID = 1,
                campusID = 999,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/contact/contact/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 999,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CAMPUS_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 항목 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateContactOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(contactService).updateContact(
            1,
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/contact/contact/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ContactRequest(
                                categoryID = 1,
                                campusID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("연락처 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteContactById() {
        mockMvc
            .perform(delete("/api/v1/contact/contact/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("연락처 항목 삭제 - 존재하지 않는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteContactByIdNotFound() {
        doThrow(ContactNotFoundException()).whenever(contactService).deleteContactById(999)
        mockMvc
            .perform(delete("/api/v1/contact/contact/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CONTACT_NOT_FOUND"))
    }

    @Test
    @DisplayName("연락처 항목 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteContactByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(contactService).deleteContactById(1)
        mockMvc
            .perform(delete("/api/v1/contact/contact/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
