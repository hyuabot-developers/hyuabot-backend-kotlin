package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Notice
import app.hyuabot.backend.database.entity.NoticeCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface NoticeRepository : JpaRepository<Notice, Int> {
    fun findByTitleContaining(title: String): List<Notice>

    fun findByCategoryID(categoryID: Int): List<Notice>

    fun findByCategoryIDAndExpiredAtAfter(
        categoryID: Int,
        expiredAt: ZonedDateTime,
    ): List<Notice>

    @Query("SELECT DISTINCT c FROM notice_category c LEFT JOIN FETCH c.notice n")
    fun findAllWithNotices(): List<NoticeCategory>
}
