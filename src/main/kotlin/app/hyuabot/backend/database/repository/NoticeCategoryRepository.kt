package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.NoticeCategory
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeCategoryRepository : JpaRepository<NoticeCategory, Int> {
    fun findByNameContaining(name: String): List<NoticeCategory>

    fun findByName(name: String): NoticeCategory?
}
