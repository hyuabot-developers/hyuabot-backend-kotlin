package app.hyuabot.backend.notice

import app.hyuabot.backend.database.entity.Notice
import app.hyuabot.backend.database.entity.NoticeCategory
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.notice.domain.NoticeCategoryRequest
import app.hyuabot.backend.notice.domain.NoticeRequest
import app.hyuabot.backend.notice.exception.DuplicateCategoryException
import app.hyuabot.backend.notice.exception.NoticeCategoryNotFoundException
import app.hyuabot.backend.notice.exception.NoticeNotFoundException
import app.hyuabot.backend.security.WithCustomMockUser
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
class NoticeControllerTest {
    @MockitoBean
    private lateinit var noticeService: NoticeService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("공지사항 카테고리 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticeCategories() {
        doReturn(
            listOf(
                NoticeCategory(id = 1, name = "General", notice = emptyList()),
                NoticeCategory(id = 2, name = "Events", notice = emptyList()),
            ),
        ).whenever(noticeService).getNoticeCategories()
        mockMvc
            .perform(get("/api/v1/notice/category"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].name").value("General"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].name").value("Events"))
    }

    @Test
    @DisplayName("공지사항 카테고리 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateNoticeCategory() {
        val newCategory = NoticeCategory(id = 3, name = "Updates", notice = emptyList())
        doReturn(newCategory).whenever(noticeService).createNoticeCategory(
            NoticeCategoryRequest(
                name = "Updates",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/notice/category")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(NoticeCategoryRequest(name = "Updates"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(3))
            .andExpect(jsonPath("$.name").value("Updates"))
    }

    @Test
    @DisplayName("공지사항 카테고리 생성 - 중복된 이름")
    @WithCustomMockUser(username = "test_user")
    fun testCreateNoticeCategoryDuplicateName() {
        doThrow(DuplicateCategoryException()).whenever(noticeService).createNoticeCategory(
            NoticeCategoryRequest(
                name = "General",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/notice/category")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(NoticeCategoryRequest(name = "General"))),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_CATEGORY_NAME"))
    }

    @Test
    @DisplayName("공지사항 카테고리 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateNoticeCategoryOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).createNoticeCategory(
            NoticeCategoryRequest(
                name = "NewCategory",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/notice/category")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(NoticeCategoryRequest(name = "NewCategory"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공지사항 카테고리 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticeCategoryById() {
        val category = NoticeCategory(id = 1, name = "General", notice = emptyList())
        doReturn(category).whenever(noticeService).getNoticeCategoryById(1)
        mockMvc
            .perform(get("/api/v1/notice/category/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("General"))
    }

    @Test
    @DisplayName("공지사항 카테고리 항목 조회 - 존재하지 않는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticeCategoryByIdNotFound() {
        doThrow(NoticeCategoryNotFoundException()).whenever(noticeService).getNoticeCategoryById(999)
        mockMvc
            .perform(get("/api/v1/notice/category/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 카테고리 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticeCategoryByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).getNoticeCategoryById(2)
        mockMvc
            .perform(get("/api/v1/notice/category/2"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공지사항 카테고리 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteNoticeCategoryById() {
        mockMvc
            .perform(
                delete("/api/v1/notice/category/1"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("공지사항 카테고리 삭제 - 존재하지 않는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteNoticeCategoryByIdNotFound() {
        doThrow(NoticeCategoryNotFoundException()).whenever(noticeService).deleteNoticeCategoryById(999)
        mockMvc
            .perform(delete("/api/v1/notice/category/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 카테고리 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteNoticeCategoryByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).deleteNoticeCategoryById(2)
        mockMvc
            .perform(delete("/api/v1/notice/category/2"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공지사항 카테고리별 공지사항 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticesByCategoryId() {
        doReturn(
            listOf(
                Notice(
                    id = 1,
                    title = "Notice 1",
                    url = "https://example.com/notice_1",
                    expiredAt = ZonedDateTime.now(),
                    categoryID = 1,
                    userID = "test_user",
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
                    userID = "test_user",
                    language = "English",
                    category = null,
                    user = null,
                ),
            ),
        ).whenever(noticeService).getNoticesByCategoryId(1)
        mockMvc
            .perform(get("/api/v1/notice/category/1/notices"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").isArray)
    }

    @Test
    @DisplayName("공지사항 카테고리별 공지사항 목록 조회 - 존재하지 않는 카테고리 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticesByCategoryIdNotFound() {
        doThrow(NoticeCategoryNotFoundException()).whenever(noticeService).getNoticesByCategoryId(999)
        mockMvc
            .perform(get("/api/v1/notice/category/999/notices"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 카테고리별 공지사항 목록 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticesByCategoryIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).getNoticesByCategoryId(2)
        mockMvc
            .perform(get("/api/v1/notice/category/2/notices"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공지사항 전체 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetAllNotices() {
        doReturn(
            listOf(
                Notice(
                    id = 1,
                    title = "Notice 1",
                    url = "https://example.com/notice_1",
                    expiredAt = ZonedDateTime.now(),
                    categoryID = 1,
                    userID = "test_user",
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
                    userID = "test_user",
                    language = "English",
                    category = null,
                    user = null,
                ),
            ),
        ).whenever(noticeService).getAllNotices()
        mockMvc
            .perform(get("/api/v1/notice/notices"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").isArray)
    }

    @Test
    @DisplayName("공지사항 등록")
    @WithCustomMockUser(username = "test_user")
    fun testCreateNotice() {
        val newNotice =
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
                userID = "test_user",
                language = "English",
                category = null,
                user = null,
            )
        val noticeRequest =
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        doReturn(newNotice).whenever(noticeService).createNotice(
            "test_user",
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 1,
                language = "English",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/notice/notices")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.title").value("New Notice"))
            .andExpect(jsonPath("$.url").value("https://example.com/new_notice"))
            .andExpect(jsonPath("$.categoryID").value(1))
            .andExpect(jsonPath("$.userID").value("test_user"))
            .andExpect(jsonPath("$.language").value("English"))
    }

    @Test
    @DisplayName("공지사항 등록 - 날짜 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateNoticeInvalidDateFormat() {
        val noticeRequest =
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024/12/31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        doThrow(LocalDateTimeNotValidException()).whenever(noticeService).createNotice(
            "test_user",
            noticeRequest,
        )
        mockMvc
            .perform(
                post("/api/v1/notice/notices")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("공지사항 등록 - 존재하지 않는 카테고리 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateNoticeCategoryNotFound() {
        val noticeRequest =
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 999,
                language = "English",
            )
        doThrow(NoticeCategoryNotFoundException()).whenever(noticeService).createNotice(
            "test_user",
            noticeRequest,
        )
        mockMvc
            .perform(
                post("/api/v1/notice/notices")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 등록 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateNoticeOtherException() {
        val noticeRequest =
            NoticeRequest(
                title = "New Notice",
                url = "https://example.com/new_notice",
                expiredAt = "2024-12-31 23:59:59",
                categoryID = 1,
                language = "English",
            )
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).createNotice(
            "test_user",
            noticeRequest,
        )
        mockMvc
            .perform(
                post("/api/v1/notice/notices")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공지사항 상세 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticeById() {
        val notice =
            Notice(
                id = 1,
                title = "Notice 1",
                url = "https://example.com/notice_1",
                expiredAt = ZonedDateTime.now(),
                categoryID = 1,
                userID = "test_user",
                language = "English",
                category = null,
                user = null,
            )
        doReturn(notice).whenever(noticeService).getNoticesById(1)
        mockMvc
            .perform(get("/api/v1/notice/notices/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.title").value("Notice 1"))
            .andExpect(jsonPath("$.url").value("https://example.com/notice_1"))
            .andExpect(jsonPath("$.categoryID").value(1))
            .andExpect(jsonPath("$.userID").value("test_user"))
            .andExpect(jsonPath("$.language").value("English"))
    }

    @Test
    @DisplayName("공지사항 상세 조회 - 존재하지 않는 공지사항 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticeByIdNotFound() {
        doThrow(NoticeNotFoundException()).whenever(noticeService).getNoticesById(999)
        mockMvc
            .perform(get("/api/v1/notice/notices/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("NOTICE_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 상세 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetNoticeByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).getNoticesById(2)
        mockMvc
            .perform(get("/api/v1/notice/notices/2"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공지사항 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateNotice() {
        val updatedNotice =
            Notice(
                id = 1,
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt =
                    ZonedDateTime.of(
                        2025,
                        1,
                        31,
                        23,
                        59,
                        59,
                        0,
                        LocalDateTimeBuilder.serviceTimezone,
                    ),
                categoryID = 2,
                userID = "test_user",
                language = "Korean",
                category = null,
                user = null,
            )
        val noticeRequest =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2025-01-31 23:59:59",
                categoryID = 2,
                language = "Korean",
            )
        doReturn(updatedNotice).whenever(noticeService).updateNotice(1, noticeRequest)
        mockMvc
            .perform(
                put("/api/v1/notice/notices/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.title").value("Updated Notice"))
            .andExpect(jsonPath("$.url").value("https://example.com/updated_notice"))
            .andExpect(jsonPath("$.categoryID").value(2))
            .andExpect(jsonPath("$.userID").value("test_user"))
            .andExpect(jsonPath("$.language").value("Korean"))
    }

    @Test
    @DisplayName("공지사항 수정 - 날짜 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateNoticeInvalidDateFormat() {
        val noticeRequest =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2025/01/31 23:59:59",
                categoryID = 2,
                language = "Korean",
            )
        doThrow(LocalDateTimeNotValidException()).whenever(noticeService).updateNotice(1, noticeRequest)
        mockMvc
            .perform(
                put("/api/v1/notice/notices/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("공지사항 수정 - 존재하지 않는 카테고리 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateNoticeCategoryNotFound() {
        val noticeRequest =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2025-01-31 23:59:59",
                categoryID = 999,
                language = "Korean",
            )
        doThrow(NoticeCategoryNotFoundException()).whenever(noticeService).updateNotice(1, noticeRequest)
        mockMvc
            .perform(
                put("/api/v1/notice/notices/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 수정 - 존재하지 않는 공지사항 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateNoticeNotFound() {
        val noticeRequest =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2025-01-31 23:59:59",
                categoryID = 2,
                language = "Korean",
            )
        doThrow(NoticeNotFoundException()).whenever(noticeService).updateNotice(999, noticeRequest)
        mockMvc
            .perform(
                put("/api/v1/notice/notices/999")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("NOTICE_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateNoticeOtherException() {
        val noticeRequest =
            NoticeRequest(
                title = "Updated Notice",
                url = "https://example.com/updated_notice",
                expiredAt = "2025-01-31 23:59:59",
                categoryID = 2,
                language = "Korean",
            )
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).updateNotice(1, noticeRequest)
        mockMvc
            .perform(
                put("/api/v1/notice/notices/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(noticeRequest)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공지사항 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteNoticeById() {
        mockMvc
            .perform(
                delete("/api/v1/notice/notices/1"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("공지사항 삭제 - 존재하지 않는 공지사항 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteNoticeByIdNotFound() {
        doThrow(NoticeNotFoundException()).whenever(noticeService).deleteNoticeById(999)
        mockMvc
            .perform(delete("/api/v1/notice/notices/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("NOTICE_NOT_FOUND"))
    }

    @Test
    @DisplayName("공지사항 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteNoticeByIdOtherException() {
        doThrow(RuntimeException("Unexpected error")).whenever(noticeService).deleteNoticeById(2)
        mockMvc
            .perform(delete("/api/v1/notice/notices/2"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
