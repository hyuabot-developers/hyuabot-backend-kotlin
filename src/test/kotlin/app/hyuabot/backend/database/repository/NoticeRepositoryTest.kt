package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Notice
import app.hyuabot.backend.database.entity.NoticeCategory
import app.hyuabot.backend.database.entity.User
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
class NoticeRepositoryTest {
    @Autowired lateinit var userRepository: UserRepository

    @Autowired lateinit var noticeRepository: NoticeRepository

    @Autowired lateinit var noticeCategoryRepository: NoticeCategoryRepository

    val currentTime: ZonedDateTime = ZonedDateTime.now()
    val user =
        User(
            userID = "test_user",
            password = "password".toByteArray(),
            name = "Test User",
            email = "test@example.com",
            phone = "1234567890",
            active = true,
        )
    val category =
        NoticeCategory(
            name = "General",
            notice = listOf(),
        )

    @BeforeEach
    fun setUp() {
        val savedUser = userRepository.save(user)
        noticeCategoryRepository.save(category)
        noticeRepository.saveAll(
            (0..3)
                .map {
                    Notice(
                        title = "Notice $it",
                        url = "https://example.com/notice_$it",
                        expiredAt = currentTime.plusDays(it.toLong()),
                        categoryID = category.id ?: 0,
                        userID = savedUser.userID,
                        language = "English",
                        category = category,
                        user = savedUser,
                    )
                }.toList(),
        )
    }

    @AfterEach
    fun tearDown() {
        noticeRepository.deleteAll()
        noticeCategoryRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("공지사항 카테고리 목록 키워드 검색")
    fun testNoticeCategoryFindByNameContaining() {
        val categories = noticeCategoryRepository.findByNameContaining("Gen")
        assert(categories.isNotEmpty())
        categories.forEach { category ->
            assert(category.name.contains("Gen"))
            assert(category.notice.isEmpty())
        }
    }

    @Test
    @DisplayName("공지사항 목록 제목 키워드 검색")
    fun testNoticeFindByTitleContaining() {
        val notices = noticeRepository.findByTitleContaining("Notice")
        assert(notices.isNotEmpty())
        notices.forEach { notice ->
            assert(notice.title.contains("Notice"))
            assert(notice.category!!.name == "General")
            assert(notice.user!!.userID == user.userID)
        }
    }

    @Test
    @DisplayName("카테고리별 공지사항 목록 조회")
    fun testNoticeFindByCategoryID() {
        val notices = noticeRepository.findByCategoryID(category.id ?: 0)
        assert(notices.isNotEmpty())
        notices.forEach { notice ->
            assert(notice.categoryID == category.id)
            assert(notice.category!!.name == "General")
            assert(notice.user!!.userID == user.userID)
        }
    }

    @Test
    @DisplayName("카테고리별 및 아직 종료되지 않은 공지사항 목록 조회")
    fun testNoticeFindByCategoryIDAndExpiredAtAfter() {
        val notices = noticeRepository.findByCategoryIDAndExpiredAtAfter(category.id ?: 0, currentTime)
        assert(notices.isNotEmpty())
        notices.forEach { notice ->
            assert(notice.id != null)
            assert(notice.userID == user.userID)
            assert(notice.categoryID == category.id)
            assert(notice.category!!.name == "General")
            assert(notice.user!!.userID == user.userID)
            assert(notice.expiredAt.isAfter(currentTime))
            assert(notice.title.contains("Notice"))
            assert(notice.url.startsWith("https://"))
            assert(notice.language == "English")
        }
    }
}
