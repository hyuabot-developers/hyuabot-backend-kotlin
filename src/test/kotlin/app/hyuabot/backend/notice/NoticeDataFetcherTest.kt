package app.hyuabot.backend.notice

import app.hyuabot.backend.database.entity.Notice
import app.hyuabot.backend.database.entity.NoticeCategory
import app.hyuabot.backend.notice.controller.NoticeDataFetcher
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@EnableDgsTest
@SpringJUnitConfig
@Import(NoticeDataFetcher::class, ScalarRegistration::class)
class NoticeDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var noticeService: NoticeService

    private val now = ZonedDateTime.now()

    private fun createNoticeCategory(
        id: Int = 1,
        name: String = "셔틀버스",
        notice: MutableList<Notice> = mutableListOf(),
    ) = NoticeCategory(
        id = id,
        name = name,
        notice = notice,
    )

    private fun createNotice(
        id: Int = 1,
        title: String = "Test Notice",
        url: String = "https://example.com/notice_$id",
        language: String = "KOREAN",
        expiredAt: ZonedDateTime = now.plusDays(1),
        userID: String = "user_$id",
        categoryID: Int = 1,
    ) = Notice(
        id = id,
        title = title,
        url = url,
        language = language,
        expiredAt = expiredAt,
        userID = userID,
        categoryID = categoryID,
        category = null,
        user = null,
    )

    @Test
    @DisplayName("공지사항 목록 조회")
    fun testNotices() {
        val notice1 = createNotice(id = 1, title = "셔틀버스 운행 안내")
        val notice2 = createNotice(id = 2, title = "학사 일정 변경 안내")
        val category = createNoticeCategory(id = 1, name = "셔틀버스", notice = mutableListOf(notice1, notice2))
        whenever(
            noticeService.fetchNotices(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any(),
            ),
        ).thenReturn(listOf(category))
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    notices {
                        seq
                        name
                        notices {
                            seq
                            title
                            url
                            language
                            expiredAt
                            userID
                        }
                    }
                }
                """.trimIndent(),
                "data.notices",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("셔틀버스", result[0]["name"])
        val notices = result[0]["notices"] as List<*>
        assertEquals(2, notices.size)
    }

    @Test
    @DisplayName("공지사항 목록 조회 - 카테고리 필터링")
    fun testNoticesWithCategoryFilter() {
        val notice1 = createNotice(id = 1, title = "셔틀버스 운행 안내")
        val notice2 = createNotice(id = 2, title = "학사 일정 변경 안내")
        val category2 = createNoticeCategory(id = 2, name = "학사", notice = mutableListOf(notice2))
        createNoticeCategory(id = 1, name = "셔틀버스", notice = mutableListOf(notice1))
        whenever(
            noticeService.fetchNotices(
                eq("학사"),
                anyOrNull(),
                anyOrNull(),
                any(),
            ),
        ).thenReturn(listOf(category2))
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    notices(category: "학사") {
                        seq
                        name
                        notices {
                            seq
                            title
                            url
                            language
                            expiredAt
                            userID
                        }
                    }
                }
                """.trimIndent(),
                "data.notices",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("학사", result[0]["name"])
        val notices = result[0]["notices"] as List<*>
        assertEquals(1, notices.size)
    }

    @Test
    @DisplayName("공지사항 목록 조회 - 언어 필터링")
    fun testNoticesWithLanguageFilter() {
        val notice1 = createNotice(id = 1, title = "셔틀버스 운행 안내", language = "KOREAN")
        val notice2 = createNotice(id = 2, title = "Shuttle Bus Schedule", language = "ENGLISH")
        val category = createNoticeCategory(id = 1, name = "셔틀버스", notice = mutableListOf(notice1, notice2))
        whenever(
            noticeService.fetchNotices(
                anyOrNull(),
                eq("KOREAN"),
                anyOrNull(),
                any(),
            ),
        ).thenReturn(listOf(category.copy(notice = mutableListOf(notice1))))
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    notices(language: "KOREAN") {
                        seq
                        name
                        notices {
                            seq
                            title
                            url
                            language
                            expiredAt
                            userID
                        }
                    }
                }
                """.trimIndent(),
                "data.notices",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("셔틀버스", result[0]["name"])
        val notices = result[0]["notices"] as List<*>
        assertEquals(1, notices.size)
        val notice = notices[0] as Map<*, *>
        assertEquals("KOREAN", notice["language"])
    }

    @Test
    @DisplayName("공지사항 목록 조회 - 시간 필터링")
    fun testNoticesWithTimeFilter() {
        val notice1 = createNotice(id = 1, title = "셔틀버스 운행 안내", expiredAt = now.plusDays(1))
        val notice2 = createNotice(id = 2, title = "학사 일정 변경 안내", expiredAt = now.minusDays(1))
        val category = createNoticeCategory(id = 1, name = "셔틀버스", notice = mutableListOf(notice1, notice2))
        whenever(
            noticeService.fetchNotices(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any(),
            ),
        ).thenReturn(listOf(category.copy(notice = mutableListOf(notice1))))
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    notices(timestamp: "${dateTimeFormatter.format(now)}") {
                        seq
                        name
                        notices {
                            seq
                            title
                            url
                            language
                            expiredAt
                            userID
                        }
                    }
                }
                """.trimIndent(),
                "data.notices",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("셔틀버스", result[0]["name"])
        val notices = result[0]["notices"] as List<*>
        assertEquals(1, notices.size)
        val notice = notices[0] as Map<*, *>
        assertEquals("셔틀버스 운행 안내", notice["title"])
    }
}
