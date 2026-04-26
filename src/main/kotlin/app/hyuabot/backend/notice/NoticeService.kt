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
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class NoticeService(
    private val categoryRepository: NoticeCategoryRepository,
    private val noticeRepository: NoticeRepository,
) {
    fun getNoticeCategories() = categoryRepository.findAll().sortedBy { it.id }

    fun createNoticeCategory(payload: NoticeCategoryRequest): NoticeCategory {
        categoryRepository.findByName(payload.name)?.let {
            throw DuplicateCategoryException()
        }
        return categoryRepository.save(
            NoticeCategory(
                name = payload.name,
                notice = mutableListOf(),
            ),
        )
    }

    fun getNoticeCategoryById(id: Int): NoticeCategory = categoryRepository.findById(id).orElseThrow { NoticeCategoryNotFoundException() }

    fun updateNoticeCategoryById(
        id: Int,
        payload: NoticeCategoryRequest,
    ): NoticeCategory {
        categoryRepository.findById(id).orElseThrow { NoticeCategoryNotFoundException() }.let { category ->
            categoryRepository.findByName(payload.name)?.let {
                if (it.id!! != id) {
                    throw DuplicateCategoryException()
                }
            }
            category.name = payload.name
            return categoryRepository.save(category)
        }
    }

    fun deleteNoticeCategoryById(id: Int) {
        categoryRepository.findById(id).orElseThrow { NoticeCategoryNotFoundException() }.let { category ->
            // 해당 카테고리에 속한 공지사항도 모두 삭제
            noticeRepository.deleteAll(category.notice)
            categoryRepository.delete(category)
        }
    }

    fun getAllNotices() = noticeRepository.findAll().sortedBy { it.id }

    fun getNoticesByCategoryId(categoryId: Int) =
        categoryRepository.findById(categoryId).orElseThrow { NoticeCategoryNotFoundException() }.let {
            noticeRepository.findByCategoryID(categoryId).sortedBy { it.id }
        }

    fun createNotice(
        userID: String,
        payload: NoticeRequest,
    ): Notice {
        if (!LocalDateTimeBuilder.checkLocalDateTimeFormat(payload.expiredAt)) {
            throw LocalDateTimeNotValidException()
        }
        val expiredLocalDateTime = LocalDateTime.parse(payload.expiredAt, LocalDateTimeBuilder.datetimeFormatter)
        val expiredAt = ZonedDateTime.of(expiredLocalDateTime, LocalDateTimeBuilder.serviceTimezone)
        categoryRepository.findById(payload.categoryID).orElseThrow { NoticeCategoryNotFoundException() }
        return noticeRepository.save(
            Notice(
                title = payload.title,
                url = payload.url,
                expiredAt = expiredAt,
                categoryID = payload.categoryID,
                userID = userID,
                language = payload.language,
                category = null,
                user = null,
            ),
        )
    }

    fun getNoticesById(id: Int): Notice = noticeRepository.findById(id).orElseThrow { NoticeNotFoundException() }

    fun updateNotice(
        id: Int,
        payload: NoticeRequest,
    ): Notice {
        if (!LocalDateTimeBuilder.checkLocalDateTimeFormat(payload.expiredAt)) {
            throw LocalDateTimeNotValidException()
        }
        val expiredLocalDateTime = LocalDateTime.parse(payload.expiredAt, LocalDateTimeBuilder.datetimeFormatter)
        val expiredAt = ZonedDateTime.of(expiredLocalDateTime, LocalDateTimeBuilder.serviceTimezone)
        categoryRepository.findById(payload.categoryID).orElseThrow { NoticeCategoryNotFoundException() }
        return noticeRepository.findById(id).orElseThrow { NoticeNotFoundException() }.let { notice ->
            val updatedNotice =
                notice.copy(
                    title = payload.title,
                    url = payload.url,
                    expiredAt = expiredAt,
                    categoryID = payload.categoryID,
                    language = payload.language,
                )
            noticeRepository.save(updatedNotice)
        }
    }

    fun deleteNoticeById(id: Int) {
        noticeRepository.findById(id).orElseThrow { NoticeNotFoundException() }.let { notice ->
            noticeRepository.delete(notice)
        }
    }

    fun fetchNotices(
        category: String?,
        language: String?,
        since: ZonedDateTime?,
        currentTime: ZonedDateTime,
    ): List<NoticeCategory> {
        val categories = noticeRepository.findAllWithNotices()
        val categoryFilter = category?.split(",")?.map { it.trim() } ?: emptyList()
        val timestamp = since ?: currentTime
        return categories
            .filter { cat ->
                categoryFilter.isEmpty() || categoryFilter.contains(cat.name)
            }.map { cat ->
                val filteredNotices =
                    cat.notice
                        .filter { n ->
                            (language == null || n.language == language) && n.expiredAt.isAfter(timestamp)
                        }.toMutableList()
                cat.copy(notice = filteredNotices)
            }
    }
}
