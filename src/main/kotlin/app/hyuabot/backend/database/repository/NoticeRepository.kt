package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface NoticeRepository : JpaRepository<Notice, Int> {
    fun findByTitleContaining(title: String): List<Notice>

    fun findByCategoryID(categoryID: Int): List<Notice>

    fun findByCategoryIDAndExpiredAtAfter(
        categoryID: Int,
        expiredAt: ZonedDateTime,
    ): List<Notice>
}
