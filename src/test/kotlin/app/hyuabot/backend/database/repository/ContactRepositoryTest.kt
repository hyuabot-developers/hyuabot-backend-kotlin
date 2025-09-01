package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.entity.Contact
import app.hyuabot.backend.database.entity.ContactCategory
import app.hyuabot.backend.database.entity.ContactVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.ZonedDateTime

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class ContactRepositoryTest {
    @Autowired lateinit var campusRepository: CampusRepository

    @Autowired lateinit var contactRepository: ContactRepository

    @Autowired lateinit var contactCategoryRepository: ContactCategoryRepository

    @Autowired lateinit var contactVersionRepository: ContactVersionRepository

    val category =
        ContactCategory(
            name = "General",
            contact = listOf(),
        )

    private val campus = Campus(name = "Main Campus")

    val contactVersion =
        ContactVersion(
            name = "1.0",
            createdAt = ZonedDateTime.now(),
        )

    @BeforeEach
    fun setUp() {
        campusRepository.save(campus)
        contactVersionRepository.save(contactVersion)
        contactCategoryRepository.save(category)
        contactRepository.saveAll(
            listOf(
                Contact(
                    campusID = campus.id!!,
                    categoryID = category.id ?: 0,
                    name = "Contact 1",
                    phone = "1234567890",
                    category = category,
                ),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        contactRepository.deleteAll()
        contactCategoryRepository.deleteAll()
        contactVersionRepository.deleteAll()
        campusRepository.deleteAll()
    }

    @Test
    @DisplayName("카테고리별 연락처 목록 조회")
    fun testFindContactByCategoryID() {
        val contacts = contactRepository.findByCategoryID(category.id ?: 0)
        assert(contacts.isNotEmpty())
        assert(contacts[0].id != null)
        assert(contacts[0].categoryID == category.id)
        assert(contacts[0].category!!.id == category.id)
        assert(contacts[0].campusID == campus.id!!)
        assert(contacts[0].name == "Contact 1")
        assert(contacts[0].phone == "1234567890")
    }

    @Test
    @DisplayName("캠퍼스 ID로 연락처 목록 조회")
    fun testFindContactByCampusID() {
        val contacts = contactRepository.findByCampusID(campus.id!!)
        assert(contacts.isNotEmpty())
        assert(contacts[0].campusID == campus.id!!)
    }

    @Test
    @DisplayName("연락처 목록 이름 또는 전화번호 키워드 검색")
    fun testFindContactByNameOrPhone() {
        val contacts = contactRepository.findByNameContainingOrPhoneContains("Contact", "1234567890")
        assert(contacts.isNotEmpty())
        assert(contacts[0].name == "Contact 1")
        assert(contacts[0].phone == "1234567890")
    }

    @Test
    @DisplayName("연락처 카테고리 이름 키워드 검색")
    fun testFindCategoryByNameContaining() {
        val categories = contactCategoryRepository.findByNameContaining("General")
        assert(categories.isNotEmpty())
        assert(categories[0].name == "General")
        assert(categories[0].contact.isEmpty())
    }

    @Test
    @DisplayName("최신 연락처 버전 조회")
    fun testFindLatestContactVersion() {
        val latestVersion = contactVersionRepository.findTopByOrderByCreatedAtDesc()
        assert(latestVersion != null)
        assert(latestVersion?.id != null)
        assert(latestVersion?.name == "1.0")
        assert(latestVersion?.createdAt != null)
    }
}
