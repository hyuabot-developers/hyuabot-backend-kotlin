package app.hyuabot.backend.notice

import app.hyuabot.backend.database.entity.Notice
import app.hyuabot.backend.database.entity.NoticeCategory
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.database.repository.NoticeCategoryRepository
import app.hyuabot.backend.database.repository.NoticeRepository
import app.hyuabot.backend.notice.domain.NoticeCategoryRequest
import app.hyuabot.backend.notice.domain.NoticeRequest
import app.hyuabot.backend.notice.exception.DuplicateCategoryException
import app.hyuabot.backend.notice.exception.NoticeCategoryNotFoundException
import app.hyuabot.backend.notice.exception.NoticeNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class NoticeServiceTest {
    @Mock
    lateinit var noticeRepository: NoticeRepository

    @Mock
    lateinit var categoryRepository: NoticeCategoryRepository

    @InjectMocks
    lateinit var service: NoticeService

    @Test
    @DisplayName("공지사항 카테고리 목록")
    fun testGetNoticeCategories() {
        whenever(categoryRepository.findAll()).thenReturn(
            listOf(
                NoticeCategory(
                    id = 1,
                    name = "General",
                    notice = emptyList(),
                ),
                NoticeCategory(
                    id = 2,
                    name = "Events",
                    notice = emptyList(),
                ),
            ),
        )
        val categories = service.getNoticeCategories()
        assertEquals(2, categories.size)
        assertEquals("General", categories[0].name)
        assertEquals("Events", categories[1].name)
    }

    @Test
    @DisplayName("공지사항 카테고리 생성")
    fun testCreateNoticeCategory() {
        val newCategory =
            NoticeCategory(
                id = 3,
                name = "Announcements",
                notice = emptyList(),
            )
        whenever(categoryRepository.findByName("Announcements")).thenReturn(null)
        whenever(
            categoryRepository.save(
                NoticeCategory(
                    name = "Announcements",
                    notice = emptyList(),
                ),
            ),
        ).thenReturn(newCategory)

        val createdCategory =
            service.createNoticeCategory(
                NoticeCategoryRequest(
                    name = "Announcements",
                ),
            )
        assertEquals("Announcements", createdCategory.name)
        assertEquals(3, createdCategory.id)
    }

    @Test
    @DisplayName("공지사항 카테고리 생성 - 중복된 이름")
    fun testCreateNoticeCategoryDuplicateName() {
        val existingCategory =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        whenever(categoryRepository.findByName("General")).thenReturn(existingCategory)
        assertThrows<DuplicateCategoryException> { service.createNoticeCategory(NoticeCategoryRequest("General")) }
    }

    @Test
    @DisplayName("공지사항 카테고리 항목 ID로 조회")
    fun testGetNoticeCategoryById() {
        val category =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        whenever(categoryRepository.findById(1)).thenReturn(Optional.of(category))
        val foundCategory = service.getNoticeCategoryById(1)
        assertEquals("General", foundCategory.name)
        assertEquals(1, foundCategory.id)
    }

    @Test
    @DisplayName("공지사항 카테고리 항목 ID로 조회 - 존재하지 않는 ID")
    fun testGetNoticeCategoryByIdNotFound() {
        whenever(categoryRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<NoticeCategoryNotFoundException> { service.getNoticeCategoryById(999) }
    }

    @Test
    @DisplayName("공지사항 카테고리 항목 ID로 삭제")
    fun testDeleteNoticeCategoryById() {
        val category =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        whenever(categoryRepository.findById(1)).thenReturn(Optional.of(category))
        service.deleteNoticeCategoryById(1)
    }

    @Test
    @DisplayName("공지사항 카테고리 항목 ID로 삭제 - 존재하지 않는 ID")
    fun testDeleteNoticeCategoryByIdNotFound() {
        whenever(categoryRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<NoticeCategoryNotFoundException> { service.deleteNoticeCategoryById(999) }
    }

    @Test
    @DisplayName("공지사항 카테고리별 공지사항 목록 조회")
    fun testGetNoticesByCategoryId() {
        val category =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        whenever(categoryRepository.findById(1)).thenReturn(Optional.of(category))
        whenever(noticeRepository.findByCategoryID(1)).thenReturn(
            listOf(
                Notice(
                    id = 1,
                    title = "Notice 1",
                    url = "https://example.com/notice_1",
                    expiredAt = ZonedDateTime.now(),
                    categoryID = 1,
                    userID = "user1",
                    language = "English",
                    category = category,
                    user = null,
                ),
                Notice(
                    id = 2,
                    title = "Notice 2",
                    url = "https://example.com/notice_2",
                    expiredAt = ZonedDateTime.now(),
                    categoryID = 1,
                    userID = "user2",
                    language = "English",
                    category = category,
                    user = null,
                ),
            ),
        )
        val notices = service.getNoticesByCategoryId(1)
        assertEquals(2, notices.size)
        assertEquals("Notice 1", notices[0].title)
        assertEquals("Notice 2", notices[1].title)
    }

    @Test
    @DisplayName("카테고리별 공지사항 목록 조회 - 카테고리 없음")
    fun testGetNoticesByCategoryIdNoCategory() {
        whenever(categoryRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<NoticeCategoryNotFoundException> { service.getNoticesByCategoryId(1) }
    }

    @Test
    @DisplayName("전체 공지사항 목록 조회")
    fun testGetAllNotices() {
        whenever(noticeRepository.findAll()).thenReturn(
            listOf(
                Notice(
                    id = 1,
                    title = "Notice 1",
                    url = "https://example.com/notice_1",
                    expiredAt = ZonedDateTime.now(),
                    categoryID = 1,
                    userID = "user1",
                    language = "English",
                    category = null,
                    user = null,
                ),
                Notice(
                    id = 2,
                    title = "Notice 2",
                    url = "https://example.com/notice_2",
                    expiredAt = ZonedDateTime.now(),
                    categoryID = 1,
                    userID = "user2",
                    language = "English",
                    category = null,
                    user = null,
                ),
            ),
        )
        val notices = service.getAllNotices()
        assertEquals(2, notices.size)
        assertEquals("Notice 1", notices[0].title)
        assertEquals("Notice 2", notices[1].title)
    }

    @Test
    @DisplayName("공지사항 등록")
    fun testCreateCategory() {
        val request =
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        val category =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        whenever(categoryRepository.findById(1)).thenReturn(Optional.of(category))
        whenever(
            noticeRepository.save(
                Notice(
                    title = "New Notice",
                    url = "https://example.com/new_notice",
                    expiredAt =
                        ZonedDateTime.of(
                            2024,
                            12,
                            31,
                            23,
                            59,
                            59,
                            0,
                            LocalDateTimeBuilder.serviceTimezone,
                        ),
                    categoryID = 1,
                    userID = "admin",
                    language = "English",
                    category = null,
                    user = null,
                ),
            ),
        ).thenReturn(
            Notice(
                id = 1,
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt =
                    ZonedDateTime.of(
                        2024,
                        12,
                        31,
                        23,
                        59,
                        59,
                        0,
                        LocalDateTimeBuilder.serviceTimezone,
                    ),
                categoryID = 1,
                userID = "admin",
                language = "English",
                category = category,
                user = null,
            ),
        )
        val createdNotice = service.createNotice("admin", request)
        assertEquals("New Notice", createdNotice.title)
        assertEquals(1, createdNotice.id)
    }

    @Test
    @DisplayName("공지사항 등록 - 잘못된 날짜 형식")
    fun testCreateNoticeInvalidDateFormat() {
        val request =
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024/12/31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        assertThrows<LocalDateTimeNotValidException> {
            service.createNotice("admin", request)
        }
    }

    @Test
    @DisplayName("공지사항 등록 - 존재하지 않는 카테고리")
    fun testCreateNoticeNoCategory() {
        val request =
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 999,
                language = "English",
            )
        whenever(categoryRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<NoticeCategoryNotFoundException> {
            service.createNotice("admin", request)
        }
    }

    @Test
    @DisplayName("공지사항 항목 ID로 조회")
    fun testGetNoticesById() {
        val category =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        val notice =
            Notice(
                id = 1,
                title = "Notice 1",
                url = "https://example.com/notice_1",
                expiredAt = ZonedDateTime.now(),
                categoryID = 1,
                userID = "user1",
                language = "English",
                category = category,
                user = null,
            )
        whenever(noticeRepository.findById(1)).thenReturn(Optional.of(notice))
        val foundNotice = service.getNoticesById(1)
        assertEquals("Notice 1", foundNotice.title)
        assertEquals(1, foundNotice.id)
    }

    @Test
    @DisplayName("공지사항 항목 ID로 조회 - 존재하지 않는 ID")
    fun testGetNoticesByIdNotFound() {
        whenever(noticeRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<NoticeNotFoundException> { service.getNoticesById(999) }
    }

    @Test
    @DisplayName("공지사항 수정")
    fun testUpdateNotice() {
        val request =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        val category =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        val existingNotice =
            Notice(
                id = 1,
                title = "Old Notice",
                url = "https://example.com/old_notice",
                expiredAt = ZonedDateTime.now(),
                categoryID = 1,
                userID = "user1",
                language = "English",
                category = category,
                user = null,
            )
        whenever(noticeRepository.findById(1)).thenReturn(Optional.of(existingNotice))
        whenever(categoryRepository.findById(1)).thenReturn(Optional.of(category))
        whenever(
            noticeRepository.save(
                Notice(
                    id = 1,
                    title = "Updated Notice",
                    url = "https://example.com/updated_notice",
                    expiredAt =
                        ZonedDateTime.of(
                            2024,
                            12,
                            31,
                            23,
                            59,
                            59,
                            0,
                            LocalDateTimeBuilder.serviceTimezone,
                        ),
                    categoryID = 1,
                    userID = "user1",
                    language = "English",
                    category = category,
                    user = null,
                ),
            ),
        ).thenReturn(
            Notice(
                id = 1,
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt =
                    ZonedDateTime.of(
                        2024,
                        12,
                        31,
                        23,
                        59,
                        59,
                        0,
                        LocalDateTimeBuilder.serviceTimezone,
                    ),
                categoryID = 1,
                userID = "user1",
                language = "English",
                category = category,
                user = null,
            ),
        )
        val updatedNotice = service.updateNotice(1, request)
        assertEquals("Updated Notice", updatedNotice.title)
        assertEquals(1, updatedNotice.id)
    }

    @Test
    @DisplayName("공지사항 수정 - 잘못된 날짜 형식")
    fun testUpdateNoticeInvalidDateFormat() {
        val request =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2024/12/31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        assertThrows<LocalDateTimeNotValidException> {
            service.updateNotice(1, request)
        }
    }

    @Test
    @DisplayName("공지사항 수정 - 존재하지 않는 카테고리")
    fun testUpdateNoticeNoCategory() {
        val request =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 999,
                language = "English",
            )
        whenever(categoryRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<NoticeCategoryNotFoundException> {
            service.updateNotice(1, request)
        }
    }

    @Test
    @DisplayName("공지사항 수정 - 존재하지 않는 공지사항")
    fun testUpdateNoticeNotFound() {
        val request =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        whenever(categoryRepository.findById(1)).thenReturn(
            Optional.of(
                NoticeCategory(
                    id = 1,
                    name = "General",
                    notice = emptyList(),
                ),
            ),
        )
        whenever(noticeRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<NoticeNotFoundException> {
            service.updateNotice(999, request)
        }
    }

    @Test
    @DisplayName("공지사항 항목 ID로 삭제")
    fun testDeleteNoticeById() {
        val category =
            NoticeCategory(
                id = 1,
                name = "General",
                notice = emptyList(),
            )
        val notice =
            Notice(
                id = 1,
                title = "Notice 1",
                url = "https://example.com/notice_1",
                expiredAt = ZonedDateTime.now(),
                categoryID = 1,
                userID = "user1",
                language = "English",
                category = category,
                user = null,
            )
        whenever(noticeRepository.findById(1)).thenReturn(Optional.of(notice))
        service.deleteNoticeById(1)
    }

    @Test
    @DisplayName("공지사항 항목 ID로 삭제 - 존재하지 않는 ID")
    fun testDeleteNoticeByIdNotFound() {
        whenever(noticeRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<NoticeNotFoundException> { service.deleteNoticeById(999) }
    }
}
