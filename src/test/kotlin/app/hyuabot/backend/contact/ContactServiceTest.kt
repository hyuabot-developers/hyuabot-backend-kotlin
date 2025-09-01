package app.hyuabot.backend.contact

import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.contact.domain.ContactCategoryRequest
import app.hyuabot.backend.contact.domain.ContactRequest
import app.hyuabot.backend.contact.exception.ContactCategoryNotFoundException
import app.hyuabot.backend.contact.exception.ContactNotFoundException
import app.hyuabot.backend.contact.exception.ContactVersionNotFoundException
import app.hyuabot.backend.contact.exception.DuplicateCategoryException
import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.entity.Contact
import app.hyuabot.backend.database.entity.ContactCategory
import app.hyuabot.backend.database.entity.ContactVersion
import app.hyuabot.backend.database.exception.PhoneNumberNotValidException
import app.hyuabot.backend.database.repository.CampusRepository
import app.hyuabot.backend.database.repository.ContactCategoryRepository
import app.hyuabot.backend.database.repository.ContactRepository
import app.hyuabot.backend.database.repository.ContactVersionRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ContactServiceTest {
    @Mock
    lateinit var contactRepository: ContactRepository

    @Mock
    lateinit var categoryRepository: ContactCategoryRepository

    @Mock
    lateinit var versionRepository: ContactVersionRepository

    @Mock
    lateinit var campusRepository: CampusRepository

    @InjectMocks
    lateinit var service: ContactService

    @Test
    @DisplayName("전화부 버전 조회 테스트")
    fun testGetContactVersion() {
        whenever(versionRepository.findTopByOrderByCreatedAtDesc()).thenReturn(
            ContactVersion(
                id = 1,
                name = "1.0",
                createdAt = ZonedDateTime.now(),
            ),
        )
        val version = service.getContactVersion()
        assertEquals("1.0", version.name)
    }

    @Test
    @DisplayName("전화부 버전 조회 실패 테스트 - 버전이 없는 경우")
    fun testGetContactVersionNotFound() {
        whenever(versionRepository.findTopByOrderByCreatedAtDesc()).thenReturn(null)
        assertThrows<ContactVersionNotFoundException> { service.getContactVersion() }
    }

    @Test
    @DisplayName("전화부 카테고리 목록")
    fun testGetContactCategories() {
        val categories =
            listOf(
                ContactCategory(
                    id = 1,
                    name = "General",
                    contact = listOf(),
                ),
                ContactCategory(
                    id = 2,
                    name = "Emergency",
                    contact = listOf(),
                ),
            )
        whenever(categoryRepository.findAll()).thenReturn(categories)
        val result = service.getContactCategories()
        assertEquals(2, result.size)
        assertEquals("General", result[0].name)
        assertEquals("Emergency", result[1].name)
    }

    @Test
    @DisplayName("전화부 카테고리 생성")
    fun testCreateContactCategory() {
        val payload =
            ContactCategoryRequest(
                name = "Support",
            )
        val newCategory =
            ContactCategory(
                id = null,
                name = payload.name,
                contact = listOf(),
            )
        whenever(categoryRepository.findByName(payload.name)).thenReturn(null)
        whenever(categoryRepository.save(newCategory)).thenReturn(
            ContactCategory(
                id = 1,
                name = payload.name,
                contact = listOf(),
            ),
        )
        val result = service.createContactCategory(payload)
        assertEquals(1, result.id)
        assertEquals("Support", result.name)
    }

    @Test
    @DisplayName("전화부 카테고리 생성 실패 - 중복된 이름")
    fun testCreateContactCategoryDuplicateName() {
        val payload =
            ContactCategoryRequest(
                name = "General",
            )
        whenever(categoryRepository.findByName(payload.name)).thenReturn(
            ContactCategory(
                id = 1,
                name = payload.name,
                contact = listOf(),
            ),
        )
        assertThrows<DuplicateCategoryException> { service.createContactCategory(payload) }
    }

    @Test
    @DisplayName("전화부 카테고리 항목 조회 - ID로 조회")
    fun testGetContactCategoryByID() {
        val category =
            ContactCategory(
                id = 1,
                name = "General",
                contact = listOf(),
            )
        whenever(categoryRepository.findById(1)).thenReturn(Optional.of(category))
        val result = service.getContactCategoryById(1)
        assertEquals(1, result.id)
        assertEquals("General", result.name)
    }

    @Test
    @DisplayName("전화부 카테고리 항목 조회 실패 - 없는 ID로 조회")
    fun testGetContactCategoryByIDNotFound() {
        whenever(categoryRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<ContactCategoryNotFoundException> { service.getContactCategoryById(1) }
    }

    @Test
    @DisplayName("전화부 카테고리 항목 삭제 - ID로 삭제")
    fun testDeleteContactCategoryByID() {
        val category =
            ContactCategory(
                id = 1,
                name = "General",
                contact = listOf(),
            )
        whenever(categoryRepository.findById(1)).thenReturn(Optional.of(category))
        service.deleteContactCategoryById(1)
    }

    @Test
    @DisplayName("전화부 카테고리 항목 삭제 실패 - 없는 ID로 삭제")
    fun testDeleteContactCategoryByIDNotFound() {
        whenever(categoryRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<ContactCategoryNotFoundException> { service.deleteContactCategoryById(1) }
    }

    @Test
    @DisplayName("연락처 전체 목록 조회")
    fun testGetAllContacts() {
        whenever(contactRepository.findAll()).thenReturn(
            listOf(
                Contact(
                    id = 1,
                    campusID = 1,
                    categoryID = 1,
                    name = "홍길동",
                    phone = "010-1234-5678",
                    category = null,
                ),
                Contact(
                    id = 2,
                    campusID = 1,
                    categoryID = 2,
                    name = "김철수",
                    phone = "010-8765-4321",
                    category = null,
                ),
            ),
        )
        val contacts = service.getAllContacts()
        assertEquals(2, contacts.size)
        assertEquals("홍길동", contacts[0].name)
        assertEquals("김철수", contacts[1].name)
    }

    @Test
    @DisplayName("카테고리별 연락처 목록 조회")
    fun testGetContactsByCategoryID() {
        whenever(categoryRepository.findById(1)).thenReturn(
            Optional.of(
                ContactCategory(
                    id = 1,
                    name = "General",
                    contact =
                        listOf(
                            Contact(
                                id = 1,
                                campusID = 1,
                                categoryID = 1,
                                name = "홍길동",
                                phone = "010-1234-5678",
                                category = null,
                            ),
                        ),
                ),
            ),
        )
        val contacts = service.getContactByCategoryId(1)
        assertEquals(1, contacts.size)
        assertEquals("홍길동", contacts[0].name)
    }

    @Test
    @DisplayName("카테고리별 연락처 목록 조회 - 카테고리 없음")
    fun testGetContactsByCategoryIDNotFound() {
        whenever(categoryRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<ContactCategoryNotFoundException> { service.getContactByCategoryId(1) }
    }

    @Test
    @DisplayName("연락처 생성")
    fun testCreateContact() {
        val payload =
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            )
        whenever(campusRepository.findById(1)).thenReturn(
            Optional.of(
                Campus(
                    id = 1,
                    name = "Main Campus",
                ),
            ),
        )
        whenever(categoryRepository.findById(1)).thenReturn(
            Optional.of(
                ContactCategory(
                    id = 1,
                    name = "General",
                    contact = listOf(),
                ),
            ),
        )
        whenever(
            contactRepository.save(
                Contact(
                    campusID = payload.campusID,
                    name = payload.name,
                    phone = payload.phone,
                    categoryID = payload.categoryID,
                    category = null,
                ),
            ),
        ).thenReturn(
            Contact(
                id = 1,
                campusID = payload.campusID,
                categoryID = payload.categoryID,
                name = payload.name,
                phone = payload.phone,
                category = null,
            ),
        )
        val result = service.createContact(payload)
        assertEquals(1, result.id)
        assertEquals("홍길동", result.name)
        assertEquals("010-1234-5678", result.phone)
    }

    @Test
    @DisplayName("연락처 생성 - 잘못된 전화번호 형식")
    fun testCreateContactInvalidPhoneNumber() {
        val payload =
            ContactRequest(
                categoryID = 1,
                campusID = 1,
                name = "홍길동",
                phone = "invalid-phone-number",
            )
        assertThrows<PhoneNumberNotValidException> {
            service.createContact(payload)
        }
    }

    @Test
    @DisplayName("연락처 생성 - 없는 캠퍼스")
    fun testCreateContactCampusNotFound() {
        val payload =
            ContactRequest(
                categoryID = 1,
                campusID = 999,
                name = "홍길동",
                phone = "010-1234-5678",
            )
        whenever(campusRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<CampusNotFoundException> {
            service.createContact(payload)
        }
    }

    @Test
    @DisplayName("연락처 생성 - 없는 카테고리")
    fun testCreateContactCategoryNotFound() {
        val payload =
            ContactRequest(
                categoryID = 999,
                campusID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
            )
        whenever(campusRepository.findById(1)).thenReturn(
            Optional.of(
                Campus(
                    id = 1,
                    name = "Main Campus",
                ),
            ),
        )
        whenever(categoryRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ContactCategoryNotFoundException> {
            service.createContact(payload)
        }
    }

    @Test
    @DisplayName("연락처 항목 조회 - ID로 조회")
    fun testGetContactByID() {
        val contact =
            Contact(
                id = 1,
                campusID = 1,
                categoryID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
                category = null,
            )
        whenever(contactRepository.findById(1)).thenReturn(Optional.of(contact))
        val result = service.getContactById(1)
        assertEquals(1, result.id)
        assertEquals("홍길동", result.name)
    }

    @Test
    @DisplayName("연락처 항목 조회 실패 - 없는 ID로 조회")
    fun testGetContactByIDNotFound() {
        whenever(contactRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<ContactNotFoundException> { service.getContactById(1) }
    }

    @Test
    @DisplayName("연락처 항목 수정")
    fun testUpdateContact() {
        val payload =
            ContactRequest(
                categoryID = 2,
                campusID = 1,
                name = "김철수",
                phone = "010-8765-4321",
            )
        val existingContact =
            Contact(
                id = 1,
                campusID = 1,
                categoryID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
                category = null,
            )
        whenever(campusRepository.findById(1)).thenReturn(
            Optional.of(
                Campus(
                    id = 1,
                    name = "Main Campus",
                ),
            ),
        )
        whenever(categoryRepository.findById(2)).thenReturn(
            Optional.of(
                ContactCategory(
                    id = 2,
                    name = "Emergency",
                    contact = listOf(),
                ),
            ),
        )
        whenever(contactRepository.findById(1)).thenReturn(Optional.of(existingContact))
        whenever(contactRepository.save(existingContact)).thenReturn(
            existingContact.apply {
                name = payload.name
                phone = payload.phone
                campusID = payload.campusID
                categoryID = payload.categoryID
            },
        )
        val result = service.updateContact(1, payload)
        assertEquals(1, result.id)
        assertEquals("김철수", result.name)
        assertEquals("010-8765-4321", result.phone)
    }

    @Test
    @DisplayName("연락처 항목 수정 - 잘못된 전화번호 형식")
    fun testUpdateContactInvalidPhoneNumber() {
        val payload =
            ContactRequest(
                categoryID = 2,
                campusID = 1,
                name = "김철수",
                phone = "invalid-phone-number",
            )
        assertThrows<PhoneNumberNotValidException> {
            service.updateContact(1, payload)
        }
    }

    @Test
    @DisplayName("연락처 항목 수정 - 없는 캠퍼스")
    fun testUpdateContactCampusNotFound() {
        val payload =
            ContactRequest(
                categoryID = 2,
                campusID = 999,
                name = "김철수",
                phone = "010-8765-4321",
            )
        whenever(campusRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<CampusNotFoundException> {
            service.updateContact(1, payload)
        }
    }

    @Test
    @DisplayName("연락처 항목 수정 - 없는 카테고리")
    fun testUpdateContactCategoryNotFound() {
        val payload =
            ContactRequest(
                categoryID = 999,
                campusID = 1,
                name = "김철수",
                phone = "010-8765-4321",
            )
        whenever(campusRepository.findById(1)).thenReturn(
            Optional.of(
                Campus(
                    id = 1,
                    name = "Main Campus",
                ),
            ),
        )
        whenever(categoryRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ContactCategoryNotFoundException> {
            service.updateContact(1, payload)
        }
    }

    @Test
    @DisplayName("연락처 항목 수정 실패 - 없는 ID로 수정")
    fun testUpdateContactNotFound() {
        val payload =
            ContactRequest(
                categoryID = 2,
                campusID = 1,
                name = "김철수",
                phone = "010-8765-4321",
            )
        whenever(campusRepository.findById(1)).thenReturn(
            Optional.of(
                Campus(
                    id = 1,
                    name = "Main Campus",
                ),
            ),
        )
        whenever(categoryRepository.findById(2)).thenReturn(
            Optional.of(
                ContactCategory(
                    id = 2,
                    name = "Emergency",
                    contact = listOf(),
                ),
            ),
        )
        whenever(contactRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<ContactNotFoundException> {
            service.updateContact(1, payload)
        }
    }

    @Test
    @DisplayName("연락처 항목 삭제 - ID로 삭제")
    fun testDeleteContactByID() {
        val contact =
            Contact(
                id = 1,
                campusID = 1,
                categoryID = 1,
                name = "홍길동",
                phone = "010-1234-5678",
                category = null,
            )
        whenever(contactRepository.findById(1)).thenReturn(Optional.of(contact))
        service.deleteContactById(1)
    }

    @Test
    @DisplayName("연락처 항목 삭제 실패 - 없는 ID로 삭제")
    fun testDeleteContactByIDNotFound() {
        whenever(contactRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<ContactNotFoundException> { service.deleteContactById(1) }
    }

    @Test
    @DisplayName("연락처 버전 업데이트 - 새로 생성")
    fun testUpdateContactVersion() {
        whenever(versionRepository.findTopByOrderByCreatedAtDesc()).thenReturn(null)
        service.updateContactVersion()
        verify(versionRepository).save(
            org.mockito.kotlin.check { saved ->
                assert(saved.name.isNotBlank())
            },
        )
    }

    @Test
    @DisplayName("연락처 버전 업데이트 - 기존 버전과 다를 때")
    fun testUpdateExistingVersion() {
        val oldVersion =
            ContactVersion(
                id = 1,
                name = "oldVersion",
                createdAt = ZonedDateTime.now().minusDays(1),
            )
        whenever(versionRepository.findTopByOrderByCreatedAtDesc()).thenReturn(oldVersion)
        service.updateContactVersion()
        verify(versionRepository).save(
            org.mockito.kotlin.check { saved ->
                assertEquals(oldVersion.id, saved.id)
                assert(saved.name != "oldVersion")
            },
        )
    }
}
